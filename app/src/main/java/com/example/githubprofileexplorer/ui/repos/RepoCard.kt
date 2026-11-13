package com.example.githubprofileexplorer.ui.repos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.domain.RepoHealthScorer
import com.example.githubprofileexplorer.ui.components.AppCard
import com.example.githubprofileexplorer.ui.components.AppIcons
import com.example.githubprofileexplorer.ui.components.HealthBadge
import com.example.githubprofileexplorer.ui.components.LanguageLabel
import com.example.githubprofileexplorer.ui.components.SkeletonBox
import com.example.githubprofileexplorer.ui.components.TagChip
import com.example.githubprofileexplorer.ui.components.WrappingRow
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import com.example.githubprofileexplorer.ui.components.formatCount

/** Key used to match a repository with its in-progress download. */
fun GitHubRepo.downloadKey(): String = "$ownerLogin/$name"

@Composable
fun RepoCard(
    repo: GitHubRepo,
    downloading: Boolean,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
    saved: Boolean = false,
    onSaveToggle: () -> Unit = {}
) {
    val health = remember(repo) { RepoHealthScorer().score(repo) }
    AppCard(modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    repo.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.size(8.dp))
                HealthBadge(health)
            }
            repo.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (repo.fork || repo.archived) {
                WrappingRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (repo.fork) TagChip(stringResource(R.string.tag_fork))
                    if (repo.archived) TagChip(stringResource(R.string.tag_archived))
                }
            }
            WrappingRow(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                repo.language?.takeIf { it.isNotBlank() }?.let { LanguageLabel(it) }
                Text(
                    stringResource(R.string.stars_count, formatCount(repo.stargazersCount)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.forks_count, formatCount(repo.forksCount)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSaveToggle) {
                    Icon(
                        if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = stringResource(if (saved) R.string.remove_from_queue else R.string.save_to_queue)
                    )
                }
                IconButton(onClick = onDownload, enabled = !downloading) {
                    if (downloading) {
                        SkeletonBox(Modifier.size(16.dp), CircleShape)
                    } else {
                        Icon(AppIcons.Download, contentDescription = stringResource(R.string.download_zip))
                    }
                }
            }
        }
    }
}
