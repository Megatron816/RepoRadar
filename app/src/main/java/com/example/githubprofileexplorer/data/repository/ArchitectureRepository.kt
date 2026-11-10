package com.example.githubprofileexplorer.data.repository

import android.util.Base64
import com.example.githubprofileexplorer.data.local.ArchitectureDao
import com.example.githubprofileexplorer.data.local.ArchitectureEdgeEntity
import com.example.githubprofileexplorer.data.local.ArchitectureNodeEntity
import com.example.githubprofileexplorer.data.local.ArchitectureSnapshotBundle
import com.example.githubprofileexplorer.data.local.ArchitectureSnapshotEntity
import com.example.githubprofileexplorer.data.local.ArchitectureWarningEntity
import com.example.githubprofileexplorer.data.remote.GithubRepoApi
import com.example.githubprofileexplorer.domain.ArchitectureGraph
import com.example.githubprofileexplorer.domain.ArchitectureGraphAnalyzer
import com.example.githubprofileexplorer.domain.ArchitectureNodeType
import com.example.githubprofileexplorer.domain.RepositoryRef
import com.example.githubprofileexplorer.domain.ManifestParserRegistry
import com.example.githubprofileexplorer.domain.ArchitectureEcosystem
import com.example.githubprofileexplorer.domain.RepositorySignals
import com.example.githubprofileexplorer.domain.ArchitectureScanStats
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

enum class ArchitectureError { INVALID_REPOSITORY, NOT_FOUND, RATE_LIMITED, NETWORK, SERVER, EMPTY, UNSUPPORTED, UNKNOWN }
class ArchitectureException(val error: ArchitectureError) : Exception(error.name)

data class ArchitectureScanResult(val graph: ArchitectureGraph, val signals: RepositorySignals)

class ArchitectureRepository(
    private val api: GithubRepoApi,
    private val dao: ArchitectureDao
) {
    fun observeSnapshots() = dao.observeSnapshots()

    suspend fun scan(owner: String, name: String, requestedBranch: String? = null): Result<ArchitectureScanResult> =
        withContext(Dispatchers.IO) {
            try {
                validateIdentifier(owner, name)
                requestedBranch?.let(::validateBranch)
                val metadata = response(api.getRepository(owner, name))
                val branch = requestedBranch ?: metadata.defaultBranch
                validateBranch(branch)
                val tree = response(api.getTree(owner, name, branch))
                if (tree.tree.isEmpty()) throw ArchitectureException(ArchitectureError.EMPTY)
                val candidates = tree.tree.asSequence()
                    .filter { it.type == "blob" && it.size.orZero() <= MAX_FILE_BYTES }
                    .map { it.path }
                    .filter(ManifestParserRegistry::isKnownManifest)
                    .filterNot(::isExcludedPath)
                    .take(MAX_MANIFESTS)
                    .toList()
                if (candidates.isEmpty()) throw ArchitectureException(ArchitectureError.UNSUPPORTED)
                val files = candidates.mapNotNull { path ->
                    api.getContent(owner, name, path, branch).body()?.let { path to decode(it.content, it.encoding) }
                }.toMap()
                val parsed = ManifestParserRegistry.parse(files)
                val partial = tree.truncated || candidates.size >= MAX_MANIFESTS || files.size < candidates.size
                val scanStats = tree.tree.toScanStats(candidates)
                val ref = RepositoryRef(metadata.id, owner, name, branch)
                val graph = ArchitectureGraphAnalyzer.build(ref, tree.sha, parsed, partial)
                    .copy(scanStats = scanStats)
                val commits = api.getCommits(owner, name, perPage = 20).body().orEmpty()
                val signals = RepositorySignals(
                    metadata.pushedAt,
                    metadata.openIssuesCount,
                    metadata.archived,
                    metadata.fork,
                    metadata.license?.name,
                    commits.flatMap { it.files.orEmpty().mapNotNull { file -> file.filename } }.distinct().take(50),
                    metadata.htmlUrl ?: "https://github.com/$owner/$name",
                    graph.scannedAt
                )
                val bundle = graph.toBundle(signals)
                dao.save(bundle)
                Result.success(ArchitectureScanResult(graph, signals))
            } catch (e: Exception) {
                Result.failure(e.toArchitectureException())
            }
        }

    suspend fun load(snapshotKey: String): ArchitectureScanResult? {
        val bundle = dao.load(snapshotKey) ?: return null
        return bundle.toScanResult()
    }

    suspend fun delete(snapshotKey: String) = dao.deleteSnapshot(snapshotKey)

    private fun <T> response(response: retrofit2.Response<T>): T {
        if (response.isSuccessful) return response.body() ?: throw ArchitectureException(ArchitectureError.EMPTY)
        throw ArchitectureException(
            when (response.code()) {
                401, 403, 429 -> ArchitectureError.RATE_LIMITED
                404 -> ArchitectureError.NOT_FOUND
                in 500..599 -> ArchitectureError.SERVER
                else -> ArchitectureError.UNKNOWN
            }
        )
    }

    companion object {
        const val MAX_MANIFESTS = 80
        const val MAX_FILE_BYTES = 256_000L
        private val REPOSITORY = Regex("^[A-Za-z0-9_.-]+$")
        private val BRANCH = Regex("^[A-Za-z0-9._/-]{1,255}$")
        fun validateIdentifier(owner: String, name: String) {
            if (!REPOSITORY.matches(owner) || !REPOSITORY.matches(name)) throw ArchitectureException(ArchitectureError.INVALID_REPOSITORY)
        }
        fun validateBranch(branch: String) {
            if (!BRANCH.matches(branch) || branch.contains("..") || branch.startsWith("/") || branch.endsWith("/")) {
                throw ArchitectureException(ArchitectureError.INVALID_REPOSITORY)
            }
        }
        private fun decode(content: String?, encoding: String?): String =
            if (content == null) "" else if (encoding == "base64") String(Base64.decode(content, Base64.DEFAULT), Charsets.UTF_8) else content
        private fun isExcludedPath(path: String): Boolean {
            val segments = path.split('/')
            return segments.any { it in setOf(".git", "node_modules", "vendor", "build", "dist", "target", "generated") }
        }
        private fun Long?.orZero() = this ?: 0L
    }
}

private fun ArchitectureGraph.toBundle(signals: RepositorySignals): ArchitectureSnapshotBundle {
    val key = "${repository.key}:${repository.branch}:$commitSha"
    val entity = ArchitectureSnapshotEntity(
        key, repository.key, repository.id, repository.owner, repository.name, repository.branch,
        commitSha, scannedAt, partial, warnings.size, signals.encode(), completeness,
        detectedManifests.joinToString("\u001F"), parsedManifests.joinToString("\u001F"),
        unsupportedManifests.joinToString("\u001F"), Gson().toJson(scanStats)
    )
    return ArchitectureSnapshotBundle(
        entity,
        nodes.map { ArchitectureNodeEntity(key, it.id, it.label, it.type.name, it.version, it.sourcePath, it.resolvedVersion, it.ecosystem.name, it.projectRoot) },
        edges.map { ArchitectureEdgeEntity(key, it.from, it.to, it.scope.name, it.sourcePath, it.sourceKind) },
        warnings.map { ArchitectureWarningEntity(key, it.message, it.sourcePath.orEmpty(), it.line ?: 0) }
    )
}

private fun ArchitectureSnapshotBundle.toScanResult(): ArchitectureScanResult {
    val ref = RepositoryRef(snapshot.repositoryId, snapshot.owner, snapshot.name, snapshot.branch)
    val graph = ArchitectureGraph(
        ref, snapshot.commitSha,
        nodes.map { com.example.githubprofileexplorer.domain.ArchitectureNode(
            it.nodeId, it.label, ArchitectureNodeType.valueOf(it.type), it.version, it.sourcePath,
            it.resolvedVersion, runCatching { com.example.githubprofileexplorer.domain.ArchitectureEcosystem.valueOf(it.ecosystem) }
                .getOrDefault(com.example.githubprofileexplorer.domain.ArchitectureEcosystem.UNKNOWN), it.projectRoot
        ) },
        edges.map { com.example.githubprofileexplorer.domain.ArchitectureEdge(it.fromId, it.toId, com.example.githubprofileexplorer.domain.ArchitectureScope.valueOf(it.scope), it.sourcePath, it.sourceKind) },
        warnings.map { com.example.githubprofileexplorer.domain.ArchitectureWarning(it.message, it.sourcePath.ifBlank { null }, it.line.takeIf { value -> value > 0 }) },
        snapshot.partial, snapshot.scannedAt,
        snapshot.detectedManifests.split("\u001F").filter(String::isNotBlank),
        snapshot.parsedManifests.split("\u001F").filter(String::isNotBlank),
        snapshot.unsupportedManifests.split("\u001F").filter(String::isNotBlank),
        snapshot.completeness,
        runCatching { Gson().fromJson(snapshot.scanStatsJson, ArchitectureScanStats::class.java) }
            .getOrDefault(ArchitectureScanStats())
    )
    return ArchitectureScanResult(graph, snapshot.signalsJson.decodeSignals(snapshot.scannedAt))
}

private fun List<com.example.githubprofileexplorer.data.model.GitTreeEntry>.toScanStats(
    manifests: List<String>
): ArchitectureScanStats {
    val files = filter { it.type == "blob" }
    val directories = filter { it.type == "tree" }
    val extensionCounts = files.groupingBy {
        it.path.substringAfterLast('.', "")
            .lowercase()
            .takeIf(String::isNotBlank) ?: "[no extension]"
    }.eachCount().toSortedMap()
    val sourceExtensions = setOf(
        "kt", "java", "kts", "gradle", "xml", "swift", "ts", "tsx", "js", "jsx",
        "py", "go", "rs", "rb", "php", "cs", "cpp", "h", "hpp", "dart", "ex"
    )
    return ArchitectureScanStats(
        totalFiles = files.size,
        totalDirectories = directories.size,
        totalBytes = files.sumOf { it.size ?: 0L },
        sourceFiles = files.count { it.path.substringAfterLast('.', "").lowercase() in sourceExtensions },
        extensionCounts = extensionCounts,
        topLevelPaths = files.map { it.path.substringBefore('/') }.distinct().sorted().take(MAX_LAYOUT_ENTRIES),
        manifestSizes = files.filter { it.path in manifests }.associate { it.path to (it.size ?: 0L) },
        readmePath = files.firstOrNull { it.path.substringAfterLast('/').equals("README.md", true) }?.path,
        licensePath = files.firstOrNull { it.path.substringAfterLast('/').lowercase().startsWith("license") }?.path,
        largestFiles = files.sortedByDescending { it.size ?: 0L }
            .take(MAX_LARGE_FILES)
            .map { "${it.path} (${it.size ?: 0L} bytes)" }
    )
}

private fun RepositorySignals.encode() = listOf(lastPush.orEmpty(), openIssues.toString(), archived.toString(), fork.toString(), license.orEmpty(), sourceUrl, scannedAt.toString(), commitPaths.joinToString(",")).joinToString("|")
private fun String.decodeSignals(fallback: Long): RepositorySignals {
    val parts = split('|')
    return RepositorySignals(parts.getOrNull(0)?.ifBlank { null }, parts.getOrNull(1)?.toIntOrNull() ?: 0, parts.getOrNull(2) == "true", parts.getOrNull(3) == "true", parts.getOrNull(4)?.ifBlank { null }, parts.getOrNull(7)?.split(',').orEmpty().filter(String::isNotBlank), parts.getOrNull(5).orEmpty(), parts.getOrNull(6)?.toLongOrNull() ?: fallback)
}

private fun Exception.toArchitectureException(): ArchitectureException = when (this) {
    is ArchitectureException -> this
    is IOException -> ArchitectureException(ArchitectureError.NETWORK)
    else -> ArchitectureException(ArchitectureError.UNKNOWN)
}

private const val MAX_LAYOUT_ENTRIES = 100
private const val MAX_LARGE_FILES = 10
