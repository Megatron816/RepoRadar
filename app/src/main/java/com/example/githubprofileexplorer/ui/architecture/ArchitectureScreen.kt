package com.example.githubprofileexplorer.ui.architecture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.repository.ArchitectureError
import com.example.githubprofileexplorer.domain.ArchitectureGraph
import com.example.githubprofileexplorer.domain.ArchitectureNodeType
import com.example.githubprofileexplorer.domain.ArchitectureEcosystem
import com.example.githubprofileexplorer.domain.ArchitectureLayout
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
            is ArchitectureState.Success -> ArchitectureGraphContent(state, screen.result.graph, onQueryChange, onModulesOnlyChange, onEcosystemChange, onOpenSnapshot, onDeleteSnapshot)
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
    onDeleteSnapshot: (String) -> Unit
) {
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
                title = "${graph.repository.owner}/${graph.repository.name}",
                subtitle = stringResource(R.string.architecture_revision, graph.repository.branch, graph.commitSha)
            ) {
                if (graph.partial) Text(stringResource(R.string.architecture_partial), color = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.architecture_static_note))
                Text("Coverage: ${graph.parsedManifests.size}/${graph.detectedManifests.size} manifests parsed")
                graph.unsupportedManifests.takeIf { it.isNotEmpty() }?.let {
                    Text("Detected but unsupported: ${it.joinToString()}", color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.architecture_modules, graph.moduleCount))
                    Text(stringResource(R.string.architecture_dependencies, graph.dependencyCount))
                }
                ArchitectureCanvas(visibleGraph)
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
                node.resolvedVersion?.let { Text("Resolved version: $it") }
                Text("Ecosystem: ${node.ecosystem.name}")
                node.sourcePath?.let { path ->
                    Text(
                        "Source link: https://github.com/${graph.repository.owner}/${graph.repository.name}/blob/${graph.commitSha}/$path",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                node.sourcePath?.let { Text(stringResource(R.string.architecture_source, it), style = MaterialTheme.typography.bodySmall) }
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
private fun ArchitectureCanvas(graph: ArchitectureGraph) {
    val points = ArchitectureLayout.layout(graph)
    val outline = MaterialTheme.colorScheme.outline
    val moduleColor = MaterialTheme.colorScheme.primaryContainer
    val externalColor = MaterialTheme.colorScheme.secondaryContainer
    var zoom = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    var pan = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(Offset.Zero) }
    Canvas(
        Modifier.fillMaxWidth().height(260.dp)
            .pointerInput(graph) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    pan.value += panChange
                    zoom.floatValue = (zoom.floatValue * zoomChange).coerceIn(0.6f, 2.5f)
                }
            }
            .pointerInput(graph) { detectTapGestures(onTap = { }) }
            .graphicsLayer(scaleX = zoom.floatValue, scaleY = zoom.floatValue, translationX = pan.value.x, translationY = pan.value.y)
    ) {
        graph.edges.forEach { edge ->
            val from = points[edge.from] ?: return@forEach
            val to = points[edge.to] ?: return@forEach
            drawLine(outline, Offset(from.x, from.y), Offset(to.x, to.y), strokeWidth = 2f)
        }
        graph.nodes.forEach { node ->
            val point = points[node.id] ?: return@forEach
            drawCircle(if (node.type == ArchitectureNodeType.MODULE) moduleColor else externalColor, radius = 24f, center = Offset(point.x, point.y))
            drawCircle(outline, radius = 24f, center = Offset(point.x, point.y), style = Stroke(1f))
        }
    }
}

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
