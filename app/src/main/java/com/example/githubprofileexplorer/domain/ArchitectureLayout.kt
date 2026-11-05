package com.example.githubprofileexplorer.domain

data class GraphPoint(val x: Float, val y: Float)

object ArchitectureLayout {
    fun layout(graph: ArchitectureGraph, columns: Int = 3): Map<String, GraphPoint> =
        graph.nodes.sortedBy { it.id }.mapIndexed { index, node ->
            node.id to GraphPoint(
                x = (index % columns) * 180f + 90f,
                y = (index / columns) * 110f + 70f
            )
        }.toMap()
}

data class ArchitectureComparison(
    val addedModules: Set<String>,
    val removedModules: Set<String>,
    val addedDependencies: Set<String>,
    val removedDependencies: Set<String>,
    val changedVersions: Set<String>,
    val changedUnresolvedDeclarations: Boolean
)

object ArchitectureComparator {
    fun compare(before: ArchitectureGraph, after: ArchitectureGraph): ArchitectureComparison {
        val beforeModules = before.nodes.filter { it.type == ArchitectureNodeType.MODULE }.associateBy { it.label }
        val afterModules = after.nodes.filter { it.type == ArchitectureNodeType.MODULE }.associateBy { it.label }
        val beforeDeps = before.nodes.filter { it.type == ArchitectureNodeType.EXTERNAL }.associateBy { it.label }
        val afterDeps = after.nodes.filter { it.type == ArchitectureNodeType.EXTERNAL }.associateBy { it.label }
        val changed = beforeDeps.keys.intersect(afterDeps.keys).filter {
            beforeDeps[it]?.version != afterDeps[it]?.version
        }.toSet()
        return ArchitectureComparison(
            afterModules.keys - beforeModules.keys,
            beforeModules.keys - afterModules.keys,
            afterDeps.keys - beforeDeps.keys,
            beforeDeps.keys - afterDeps.keys,
            changed,
            before.warnings.map { it.message }.toSet() != after.warnings.map { it.message }.toSet()
        )
    }
}
