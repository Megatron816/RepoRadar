package com.example.githubprofileexplorer.domain

data class RepositoryRef(val id: Long, val owner: String, val name: String, val branch: String) {
    val key: String get() = if (id > 0) "github-repo:$id" else "repo:${owner.lowercase()}/${name.lowercase()}"
}

enum class ArchitectureNodeType { MODULE, EXTERNAL }
enum class ArchitectureScope { IMPLEMENTATION, API, COMPILE_ONLY, RUNTIME_ONLY, TEST, ANDROID_TEST, DEV, PEER, OPTIONAL, BUILD, RUNTIME, UNRESOLVED }

enum class ArchitectureEcosystem {
    GRADLE, MAVEN, NODE, PYTHON, RUST, GO, DOTNET, RUBY, PHP, SWIFT, DART, ELIXIR, SCALA, CPP, UNKNOWN
}

data class ManifestDeclaration(
    val coordinate: String,
    val version: String?,
    val scope: ArchitectureScope,
    val path: String,
    val line: Int?,
    val unresolved: Boolean = false,
    val context: String? = null,
    val ecosystem: ArchitectureEcosystem = ArchitectureEcosystem.GRADLE,
    val projectRoot: String = "",
    val resolvedVersion: String? = null,
    val dependencyType: String? = null
)

data class ArchitectureModule(
    val path: String,
    val manifestPath: String?,
    val ecosystem: ArchitectureEcosystem = ArchitectureEcosystem.GRADLE,
    val projectRoot: String = ""
)
data class ArchitectureNode(
    val id: String,
    val label: String,
    val type: ArchitectureNodeType,
    val version: String? = null,
    val sourcePath: String? = null,
    val resolvedVersion: String? = null,
    val ecosystem: ArchitectureEcosystem = ArchitectureEcosystem.GRADLE,
    val projectRoot: String = ""
)
data class ArchitectureEdge(
    val from: String,
    val to: String,
    val scope: ArchitectureScope,
    val sourcePath: String?,
    val sourceKind: String = "manifest"
)
data class ArchitectureWarning(val message: String, val sourcePath: String? = null, val line: Int? = null)

data class ArchitectureGraph(
    val repository: RepositoryRef,
    val commitSha: String,
    val nodes: List<ArchitectureNode>,
    val edges: List<ArchitectureEdge>,
    val warnings: List<ArchitectureWarning>,
    val partial: Boolean,
    val scannedAt: Long = System.currentTimeMillis(),
    val detectedManifests: List<String> = emptyList(),
    val parsedManifests: List<String> = emptyList(),
    val unsupportedManifests: List<String> = emptyList(),
    val completeness: String = if (partial) "partial" else "complete"
) {
    val moduleCount get() = nodes.count { it.type == ArchitectureNodeType.MODULE }
    val dependencyCount get() = nodes.count { it.type == ArchitectureNodeType.EXTERNAL }
}

data class GraphAnalysis(
    val cycles: List<List<String>>,
    val sharedExternalDependencies: Set<String>,
    val isolatedModules: Set<String>,
    val mostConnectedModules: List<String>,
    val unresolvedCount: Int
)

data class GradleParseResult(
    val modules: List<ArchitectureModule>,
    val declarations: List<ManifestDeclaration>,
    val warnings: List<ArchitectureWarning>
)

data class ParserCapabilities(
    val directDependencies: Boolean = true,
    val lockResolution: Boolean = false,
    val internalRelationships: Boolean = false,
    val workspaceRelationships: Boolean = false
)

data class AdapterParseResult(
    val ecosystem: ArchitectureEcosystem,
    val modules: List<ArchitectureModule>,
    val declarations: List<ManifestDeclaration>,
    val warnings: List<ArchitectureWarning>,
    val parsedManifests: List<String>,
    val capabilities: ParserCapabilities = ParserCapabilities()
)

interface ManifestParserAdapter {
    val name: String
    val version: String
    val ecosystem: ArchitectureEcosystem
    val capabilities: ParserCapabilities
    fun recognizes(path: String): Boolean
    fun parse(files: Map<String, String>): AdapterParseResult
}

data class MultiEcosystemParseResult(
    val modules: List<ArchitectureModule>,
    val declarations: List<ManifestDeclaration>,
    val warnings: List<ArchitectureWarning>,
    val detectedManifests: List<String>,
    val parsedManifests: List<String>,
    val unsupportedManifests: List<String>,
    val adapterNames: List<String>
) {
    val complete: Boolean get() = unsupportedManifests.isEmpty() && warnings.isEmpty()
}

data class RepositorySignals(
    val lastPush: String?,
    val openIssues: Int,
    val archived: Boolean,
    val fork: Boolean,
    val license: String?,
    val commitPaths: List<String>,
    val sourceUrl: String,
    val scannedAt: Long
)
