package com.example.githubprofileexplorer.domain

object ArchitectureGraphAnalyzer {
    fun build(ref: RepositoryRef, commitSha: String, parsed: MultiEcosystemParseResult, partial: Boolean): ArchitectureGraph {
        val nodes = linkedMapOf<String, ArchitectureNode>()
        parsed.modules.forEach {
            val id = "module:${it.ecosystem}:${it.path}"
            nodes[id] = ArchitectureNode(id, it.path, ArchitectureNodeType.MODULE,
                sourcePath = it.manifestPath, ecosystem = it.ecosystem, projectRoot = it.projectRoot)
        }
        val edges = linkedSetOf<ArchitectureEdge>()
        parsed.declarations.forEach { declaration ->
            val source = "module:${declaration.ecosystem}:${moduleFor(declaration.path)}"
            val isInternal = declaration.coordinate.startsWith(":") ||
                declaration.coordinate.startsWith("./") || declaration.coordinate.startsWith("../")
            val target = if (isInternal) "module:${declaration.ecosystem}:${declaration.coordinate}"
            else "external:${declaration.ecosystem}:${declaration.coordinate}"
            nodes.putIfAbsent(target, ArchitectureNode(
                target, declaration.coordinate, if (isInternal) ArchitectureNodeType.MODULE else ArchitectureNodeType.EXTERNAL,
                declaration.version, declaration.path, declaration.resolvedVersion, declaration.ecosystem, declaration.projectRoot
            ))
            edges += ArchitectureEdge(
                source, target, declaration.scope, declaration.path,
                if (declaration.dependencyType == "lock") "lock" else "manifest"
            )
        }
        return ArchitectureGraph(ref, commitSha, nodes.values.sortedBy { it.id },
            edges.sortedWith(compareBy({ it.from }, { it.to }, { it.scope.name })),
            parsed.warnings, partial || !parsed.complete, detectedManifests = parsed.detectedManifests,
            parsedManifests = parsed.parsedManifests, unsupportedManifests = parsed.unsupportedManifests,
            completeness = if (partial || !parsed.complete) "partial" else "complete")
    }

    fun build(ref: RepositoryRef, commitSha: String, parsed: GradleParseResult, partial: Boolean): ArchitectureGraph {
        val nodes = linkedMapOf<String, ArchitectureNode>()
        parsed.modules.forEach { nodes["module:${it.path}"] = ArchitectureNode("module:${it.path}", it.path, ArchitectureNodeType.MODULE, sourcePath = it.manifestPath) }
        val edges = linkedSetOf<ArchitectureEdge>()
        parsed.declarations.forEach { declaration ->
            val source = "module:${moduleFor(declaration.path)}"
            if (declaration.coordinate.startsWith(":")) {
                val target = "module:${declaration.coordinate}"
                nodes.putIfAbsent(target, ArchitectureNode(target, declaration.coordinate, ArchitectureNodeType.MODULE, sourcePath = declaration.path))
                edges += ArchitectureEdge(source, target, declaration.scope, declaration.path, "manifest")
            } else {
                val target = "external:${declaration.coordinate}"
                nodes.putIfAbsent(target, ArchitectureNode(target, declaration.coordinate, ArchitectureNodeType.EXTERNAL, declaration.version, declaration.path))
                edges += ArchitectureEdge(source, target, declaration.scope, declaration.path)
            }
        }
        return ArchitectureGraph(ref, commitSha, nodes.values.sortedBy { it.id }, edges.sortedWith(compareBy({ it.from }, { it.to }, { it.scope.name })), parsed.warnings, partial,
            parsedManifests = parsed.modules.mapNotNull { it.manifestPath }.distinct().sorted(),
            completeness = if (partial || parsed.warnings.any { it.message.contains("unresolved", true) }) "partial" else "complete")
    }

    fun analyze(graph: ArchitectureGraph): GraphAnalysis {
        val adjacency = graph.edges.filter { graph.nodes.firstOrNull { n -> n.id == it.to }?.type == ArchitectureNodeType.MODULE }
            .groupBy { it.from }.mapValues { it.value.map(ArchitectureEdge::to) }
        val cycles = mutableSetOf<List<String>>()
        fun visit(node: String, path: List<String>) {
            if (node in path) cycles += path.drop(path.indexOf(node))
            else adjacency[node].orEmpty().forEach { visit(it, path + node) }
        }
        graph.nodes.filter { it.type == ArchitectureNodeType.MODULE }.forEach { visit(it.id, emptyList()) }
        val incoming = graph.edges.groupingBy { it.to }.eachCount()
        val shared = graph.nodes.filter { it.type == ArchitectureNodeType.EXTERNAL && incoming[it.id].orZero() > 1 }.map { it.id }.toSet()
        val connected = graph.edges.flatMap { listOf(it.from, it.to) }.toSet()
        val isolated = graph.nodes.filter { it.type == ArchitectureNodeType.MODULE && it.id !in connected }.map { it.id }.toSet()
        return GraphAnalysis(cycles.toList().sortedBy { it.joinToString() }, shared, isolated, incoming.entries.sortedByDescending { it.value }.take(5).map { it.key }, graph.warnings.count { "unresolved" in it.message.lowercase() })
    }

    private fun moduleFor(path: String): String {
        val normalized = path.replace('\\', '/')
        return normalized.substringBeforeLast('/').takeIf { it.isNotBlank() }?.let { ":${it.replace('/', ':')}" } ?: ":"
    }
    private fun Int?.orZero() = this ?: 0
}
