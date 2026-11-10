package com.example.githubprofileexplorer.ui.architecture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.repository.ArchitectureError
import com.example.githubprofileexplorer.data.repository.GeminiError
import com.example.githubprofileexplorer.domain.ArchitectureGraph
import com.example.githubprofileexplorer.domain.ArchitectureNodeType
import com.example.githubprofileexplorer.domain.ArchitectureEcosystem
import com.example.githubprofileexplorer.ui.components.AppIcons
import com.example.githubprofileexplorer.ui.components.CenteredContent
import com.example.githubprofileexplorer.ui.components.EmptyState
import com.example.githubprofileexplorer.ui.components.SectionCard

@Composable
fun ArchitectureScreen(
    state: ArchitectureUiState,
    onQueryChange: (String) -> Unit,
    onModulesOnlyChange: (Boolean) -> Unit,
    onEcosystemChange: (ArchitectureEcosystem?) -> Unit,
    onOpenSnapshot: (String) -> Unit,
    onDeleteSnapshot: (String) -> Unit,
    onGenerateAiInsights: () -> Unit = {},
    onAiPromptChange: (String) -> Unit = {},
    onAskAi: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    CenteredContent(modifier) {
        when (val screen = state.state) {
            ArchitectureState.Idle -> EmptyState(stringResource(R.string.architecture_title), stringResource(R.string.architecture_idle), Icons.Default.AccountTree)
            ArchitectureState.Loading -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
            is ArchitectureState.Error -> EmptyState(stringResource(R.string.architecture_error_title), architectureError(screen.error), Icons.Default.AccountTree)
            is ArchitectureState.Success -> {
                LaunchedEffect(screen.result.graph.repository.key, screen.result.graph.commitSha) {
                    onGenerateAiInsights()
                }
                ArchitectureGraphContent(
                    state, screen.result.graph, onQueryChange, onModulesOnlyChange, onEcosystemChange,
                    onOpenSnapshot, onDeleteSnapshot, onGenerateAiInsights, onAiPromptChange, onAskAi
                )
            }
        }
    }
}

@Composable
private fun ArchitectureGraphContent(
    state: ArchitectureUiState,
    graph: ArchitectureGraph,
    onQueryChange: (String) -> Unit,
    onModulesOnlyChange: (Boolean) -> Unit,
    onEcosystemChange: (ArchitectureEcosystem?) -> Unit,
    onOpenSnapshot: (String) -> Unit,
    onDeleteSnapshot: (String) -> Unit,
    onGenerateAiInsights: () -> Unit,
    onAiPromptChange: (String) -> Unit,
    onAskAi: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val visibleIds = graph.nodes.filter {
        (!state.modulesOnly || it.type == ArchitectureNodeType.MODULE) &&
            (state.ecosystem == null || it.ecosystem == state.ecosystem) &&
            (state.query.isBlank() || it.label.contains(state.query, ignoreCase = true))
    }.map { it.id }.toSet()
    val visibleGraph = graph.copy(
        nodes = graph.nodes.filter { it.id in visibleIds },
        edges = graph.edges.filter { it.from in visibleIds && it.to in visibleIds }
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard(
                title = "AI Architecture Insights",
                subtitle = "Generate a summary or ask questions about this repository."
            ) {
                Button(onClick = onGenerateAiInsights, enabled = !state.ai.isLoading) {
                    if (state.ai.isLoading) CircularProgressIndicator()
                    else Text("Generate AI Insights")
                }
                OutlinedTextField(
                    value = state.ai.prompt,
                    onValueChange = onAiPromptChange,
                    label = { Text("Ask about this repository") },
                    placeholder = { Text("Which module has the most dependencies?") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.ai.isLoading
                )
                TextButton(
                    onClick = onAskAi,
                    enabled = state.ai.prompt.isNotBlank() && !state.ai.isLoading
                ) { Text("Ask Gemini") }
                state.ai.error?.let {
                    Text(
                        geminiErrorMessage(it),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                state.ai.response?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "AI response",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            AiResponseBullets(it)
                        }
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "${graph.repository.owner}/${graph.repository.name}",
                subtitle = stringResource(R.string.architecture_revision, graph.repository.branch, graph.commitSha)
            ) {
                if (graph.partial) Text(stringResource(R.string.architecture_partial), color = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.architecture_static_note))
                Text(stringResource(R.string.architecture_coverage, graph.parsedManifests.size, graph.detectedManifests.size))
                graph.unsupportedManifests.takeIf { it.isNotEmpty() }?.let {
                    Text(stringResource(R.string.architecture_unsupported_manifests, it.joinToString()), color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.architecture_modules, graph.moduleCount))
                    Text(stringResource(R.string.architecture_dependencies, graph.dependencyCount))
                }
                Text(
                    "Repository contents: ${graph.scanStats.totalFiles} files, " +
                        "${graph.scanStats.totalDirectories} directories, " +
                        "${graph.scanStats.sourceFiles} source files"
                )
                Text("Top-level layout: ${graph.scanStats.topLevelPaths.joinToString()}")
                Text(
                    "Detected README: ${graph.scanStats.readmePath ?: "not found"} · " +
                        "License: ${graph.scanStats.licensePath ?: "not found"}"
                )
                if (graph.scanStats.extensionCounts.isNotEmpty()) {
                    Text(
                        "File types: " + graph.scanStats.extensionCounts.entries
                            .sortedByDescending { it.value }
                            .take(10)
                            .joinToString { "${it.key}=${it.value}" }
                    )
                }
                if (graph.scanStats.largestFiles.isNotEmpty()) {
                    Text("Largest scanned files: ${graph.scanStats.largestFiles.joinToString()}")
                }
            }

        }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.architecture_search)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            FilterChip(
                selected = state.modulesOnly,
                onClick = { onModulesOnlyChange(!state.modulesOnly) },
                label = { Text(stringResource(R.string.architecture_modules_only)) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.ecosystem == null,
                    onClick = { onEcosystemChange(null) },
                    label = { Text("All ecosystems") }
                )
                graph.nodes.map { it.ecosystem }.distinct().sortedBy { it.name }.forEach { ecosystem ->
                    FilterChip(
                        selected = state.ecosystem == ecosystem,
                        onClick = { onEcosystemChange(ecosystem) },
                        label = { Text(ecosystem.name) }
                    )
                }

            }
        }
        item { Text(stringResource(R.string.architecture_outline), style = MaterialTheme.typography.titleMedium) }
        items(visibleGraph.nodes, key = { it.id }) { node ->
            val incoming = visibleGraph.edges.count { it.to == node.id }
            val outgoing = visibleGraph.edges.count { it.from == node.id }
            SectionCard(title = node.label, subtitle = if (node.type == ArchitectureNodeType.MODULE) stringResource(R.string.architecture_module_node) else stringResource(R.string.architecture_external_node)) {
                Text(stringResource(R.string.architecture_relationships, outgoing, incoming))
                node.version?.let { Text(stringResource(R.string.architecture_declared_version, it)) }
                node.resolvedVersion?.let { Text(stringResource(R.string.architecture_resolved_version, it)) }
                Text(stringResource(R.string.architecture_ecosystem, node.ecosystem.name))
                node.sourcePath?.let { path ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.architecture_source, path), style = MaterialTheme.typography.bodySmall)
                        IconButton(
                            onClick = {
                                runCatching {
                                    uriHandler.openUri(
                                        "https://github.com/${graph.repository.owner}/${graph.repository.name}/blob/${graph.commitSha}/$path"
                                    )
                                }
                            },
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(
                                AppIcons.OpenInNew,
                                contentDescription = stringResource(R.string.architecture_open_source)
                            )
                        }
                    }
                }
            }
        }
        if (state.snapshots.isNotEmpty()) {
            item { Text(stringResource(R.string.architecture_saved_scans), style = MaterialTheme.typography.titleMedium) }
            items(state.snapshots, key = { it.snapshotKey }) { snapshot ->
                SectionCard(
                    title = "${snapshot.owner}/${snapshot.name}",
                    subtitle = stringResource(R.string.architecture_saved_scan_details, snapshot.branch, snapshot.commitSha)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpenSnapshot(snapshot.snapshotKey) }) {
                            Text(stringResource(R.string.architecture_open_scan))
                        }
                        IconButton(onClick = { onDeleteSnapshot(snapshot.snapshotKey) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.architecture_delete_scan))
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun AiResponseBullets(response: String) {
    val categories = parseAiResponse(response)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        categories.forEach { category ->
            if (category.findings.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            category.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        category.findings.forEach { finding ->
                            Text(
                                finding,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class AiResponseCategory(
    val title: String,
    val findings: List<String>
)

private fun parseAiResponse(response: String): List<AiResponseCategory> {
    val titles = listOf("Technology", "Modules", "Dependencies", "Other findings")
    val findings = linkedMapOf<String, MutableList<String>>()
    titles.forEach { findings[it] = mutableListOf() }
    var current = "Other findings"

    response.lines().forEach { rawLine ->
        val line = rawLine.trim()
            .replace(Regex("""^(?:[-*•]|\d+[.)])\s+"""), "")
            .replace(Regex("""[`*_#]"""), "")
            .trim()
        if (line.isBlank()) return@forEach

        val heading = titles.firstOrNull { title ->
            line.equals(title, ignoreCase = true) ||
                line.removeSuffix(":").equals(title, ignoreCase = true)
        }
        if (heading != null) {
            current = heading
        } else {
            val target = if (current == "Other findings") {
                when {
                    line.containsAny("kotlin", "java", "android", "gradle", "compose", "swift", "python", "javascript", "typescript") -> "Technology"
                    line.containsAny("module", "project", "app") -> "Modules"
                    line.containsAny("dependenc", "library", "libraries", "version") -> "Dependencies"
                    else -> current
                }
            } else {
                current
            }
            findings.getValue(target).add(line)
        }
    }

    return titles.map { AiResponseCategory(it, findings.getValue(it)) }
}

private fun String.containsAny(vararg values: String): Boolean =
    values.any { contains(it, ignoreCase = true) }

@Composable
private fun geminiErrorMessage(error: GeminiError): String = stringResource(
    when (error) {
        GeminiError.MissingApiKey -> R.string.gemini_error_missing_key
        GeminiError.InvalidApiKey -> R.string.gemini_error_invalid_key
        GeminiError.NoInternet -> R.string.gemini_error_network
        GeminiError.RateLimited -> R.string.gemini_error_rate_limit
        GeminiError.InvalidRequest -> R.string.gemini_error_bad_request
        GeminiError.PermissionDenied -> R.string.gemini_error_permission
        GeminiError.ModelNotFound -> R.string.gemini_error_model
        GeminiError.Server -> R.string.gemini_error_server
        GeminiError.Timeout -> R.string.gemini_error_timeout
        GeminiError.EmptyResponse -> R.string.gemini_error_empty
        GeminiError.Unknown -> R.string.gemini_error_unknown
    }
)

@Composable
private fun architectureError(error: ArchitectureError) = stringResource(
    when (error) {
        ArchitectureError.INVALID_REPOSITORY -> R.string.architecture_error_invalid_repository
        ArchitectureError.NOT_FOUND -> R.string.architecture_error_not_found
        ArchitectureError.RATE_LIMITED -> R.string.architecture_error_rate_limited
        ArchitectureError.NETWORK -> R.string.architecture_error_network
        ArchitectureError.SERVER -> R.string.architecture_error_server
        ArchitectureError.EMPTY -> R.string.architecture_error_empty
        ArchitectureError.UNSUPPORTED -> R.string.architecture_error_unsupported
        ArchitectureError.UNKNOWN -> R.string.architecture_error_unknown
    }
)
