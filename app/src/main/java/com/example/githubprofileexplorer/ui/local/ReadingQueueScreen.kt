package com.example.githubprofileexplorer.ui.local

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.local.ReadingQueueEntity
import com.example.githubprofileexplorer.data.local.ReadingStatus
import com.example.githubprofileexplorer.ui.components.AppCard
import com.example.githubprofileexplorer.ui.components.EmptyState

@Composable
fun ReadingQueueScreen(
    items: List<ReadingQueueEntity>,
    onOpen: (ReadingQueueEntity) -> Unit,
    onStatusChange: (ReadingQueueEntity, ReadingStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.queue_empty_title),
            body = stringResource(R.string.queue_empty_body),
            icon = Icons.Default.Bookmark,
            modifier = modifier
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { "${it.owner}/${it.repoName}" }) { item ->
            AppCard(Modifier.fillMaxWidth(), onClick = { onOpen(item) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${item.owner}/${item.repoName}", style = MaterialTheme.typography.titleMedium)
                    item.description?.takeIf { it.isNotBlank() }?.let { Text(it, maxLines = 2) }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReadingStatus.entries.forEach { status ->
                            FilterChip(
                                selected = item.status == status,
                                onClick = { onStatusChange(item, status) },
                                label = { Text(statusLabel(status)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun statusLabel(status: ReadingStatus): String = stringResource(
    when (status) {
        ReadingStatus.TO_READ -> R.string.queue_status_to_read
        ReadingStatus.EXPLORING -> R.string.queue_status_exploring
        ReadingStatus.DONE -> R.string.queue_status_done
    }
)
