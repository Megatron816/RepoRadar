package com.example.githubprofileexplorer.ui.downloads

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.local.DownloadEntity
import com.example.githubprofileexplorer.data.local.DownloadError
import com.example.githubprofileexplorer.data.local.DownloadStatus
import com.example.githubprofileexplorer.data.local.fullName
import com.example.githubprofileexplorer.data.repository.DownloadProgress
import com.example.githubprofileexplorer.ui.components.AppCard
import com.example.githubprofileexplorer.ui.components.AppIcons
import com.example.githubprofileexplorer.ui.components.CenteredContent
import com.example.githubprofileexplorer.ui.components.EmptyState
import java.text.DateFormat
import java.util.Date

@Composable
fun DownloadsScreen(
    history: List<DownloadEntity>,
    progress: Map<Long, DownloadProgress>,
    onOpen: (DownloadEntity) -> Unit,
    onShare: (DownloadEntity) -> Unit,
    onRetry: (DownloadEntity) -> Unit,
    onDelete: (DownloadEntity) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<DownloadEntity?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    CenteredContent(modifier) {
        if (history.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.downloads_empty_title),
                body = stringResource(R.string.downloads_empty_body),
                icon = AppIcons.Download
            )
        } else {
            val context = LocalContext.current
            val totalBytes = history.filter { it.status == DownloadStatus.COMPLETED }.sumOf { it.sizeBytes }

            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        pluralStringResource(
                            R.plurals.downloads_summary,
                            history.size,
                            history.size,
                            Formatter.formatShortFileSize(context, totalBytes)
                        ),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { confirmClear = true }) {
                        Text(stringResource(R.string.clear_history))
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(history, key = { it.id }) { entity ->
                        DownloadItem(
                            entity = entity,
                            progress = progress[entity.id],
                            onOpen = { onOpen(entity) },
                            onShare = { onShare(entity) },
                            onRetry = { onRetry(entity) },
                            onDelete = { pendingDelete = entity }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { entity ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_download_title)) },
            text = { Text(stringResource(R.string.delete_download_body, entity.fullName)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(entity)
                    pendingDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onClearAll()
                    confirmClear = false
                }) { Text(stringResource(R.string.clear_history)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun DownloadItem(
    entity: DownloadEntity,
    progress: DownloadProgress?,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val statusColor = when (entity.status) {
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
    }
    val icon: ImageVector = when (entity.status) {
        DownloadStatus.DOWNLOADING -> AppIcons.Download
        DownloadStatus.COMPLETED -> Icons.Default.CheckCircle
        DownloadStatus.FAILED -> Icons.Default.Warning
    }

    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        entity.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildSubtitle(entity, context),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }

            when (entity.status) {
                DownloadStatus.DOWNLOADING -> {
                    val fraction = progress?.fraction
                    if (fraction != null) {
                        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Text(
                        text = stringResource(
                            R.string.download_received,
                            Formatter.formatShortFileSize(context, progress?.bytes ?: 0L)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DownloadStatus.COMPLETED -> Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.download_saved_to),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onOpen) { Text(stringResource(R.string.open)) }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_download))
                    }
                }

                DownloadStatus.FAILED -> Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        downloadErrorText(entity.error),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
            }
        }
    }
}

private fun buildSubtitle(entity: DownloadEntity, context: android.content.Context): String {
    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entity.createdAt))
    val parts = mutableListOf(entity.branch)
    if (entity.status == DownloadStatus.COMPLETED && entity.sizeBytes > 0) {
        parts += Formatter.formatShortFileSize(context, entity.sizeBytes)
    }
    parts += date
    return parts.joinToString("  ·  ")
}

@Composable
private fun downloadErrorText(error: DownloadError?): String = stringResource(
    when (error) {
        DownloadError.NETWORK -> R.string.download_error_network
        DownloadError.NOT_FOUND -> R.string.download_error_not_found
        DownloadError.RATE_LIMITED -> R.string.download_error_rate_limited
        DownloadError.STORAGE -> R.string.download_error_storage
        DownloadError.INTERRUPTED -> R.string.download_error_interrupted
        DownloadError.UNKNOWN, null -> R.string.download_error_unknown
    }
)
