package com.example.githubprofileexplorer.ui.repos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.repository.Profile
import com.example.githubprofileexplorer.ui.components.CenteredContent
import com.example.githubprofileexplorer.ui.components.EmptyState
import com.example.githubprofileexplorer.ui.components.ErrorState
import com.example.githubprofileexplorer.ui.components.LoadingState
import com.example.githubprofileexplorer.ui.components.WrappingRow
import com.example.githubprofileexplorer.ui.components.errorMessage

enum class RepoSort { UPDATED, STARS, NAME }

@Composable
fun RepositoriesScreen(
    profile: Profile?,
    reloading: Boolean,
    activeDownloads: Set<String>,
    onRetryRepos: () -> Unit,
    onOpenRepo: (GitHubRepo) -> Unit,
    onDownload: (GitHubRepo) -> Unit,
    modifier: Modifier = Modifier,
    queuedRepos: Set<String> = emptySet(),
    onSaveToggle: (GitHubRepo) -> Unit = {},
    onGoToProfile: () -> Unit
) {
    CenteredContent(modifier) {
        when {
            profile == null -> EmptyState(
                title = stringResource(R.string.need_profile_title),
                body = stringResource(R.string.need_profile_body),
                icon = Icons.AutoMirrored.Filled.List,
                actionLabel = stringResource(R.string.go_to_profile),
                onAction = onGoToProfile
            )

            reloading -> LoadingState()

            profile.reposError != null && profile.repos.isEmpty() -> ErrorState(
                message = errorMessage(profile.reposError),
                onRetry = onRetryRepos
            )

            profile.repos.isEmpty() -> EmptyState(
                title = stringResource(R.string.repos_empty_title),
                body = stringResource(R.string.repos_empty),
                icon = Icons.AutoMirrored.Filled.List
            )

            else -> RepositoryList(profile.repos, activeDownloads, queuedRepos, onOpenRepo, onDownload, onSaveToggle)
        }
    }
}

@Composable
private fun RepositoryList(
    repos: List<GitHubRepo>,
    activeDownloads: Set<String>,
    queuedRepos: Set<String>,
    onOpenRepo: (GitHubRepo) -> Unit,
    onDownload: (GitHubRepo) -> Unit
    , onSaveToggle: (GitHubRepo) -> Unit
) {
    var filter by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(RepoSort.UPDATED) }

    val visible = remember(repos, filter, sort) {
        val matching = repos.filter { it.name.contains(filter.trim(), ignoreCase = true) }
        when (sort) {
            RepoSort.UPDATED -> matching.sortedByDescending { it.pushedAt ?: "" }
            RepoSort.STARS -> matching.sortedByDescending { it.stargazersCount }
            RepoSort.NAME -> matching.sortedBy { it.name.lowercase() }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.filter_repositories)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.isNotEmpty()) {
                        IconButton(onClick = { filter = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                        }
                    }
                }
            )
            WrappingRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortChip(RepoSort.UPDATED, sort, R.string.sort_updated) { sort = it }
                SortChip(RepoSort.STARS, sort, R.string.sort_stars) { sort = it }
                SortChip(RepoSort.NAME, sort, R.string.sort_name) { sort = it }
            }
            Text(
                pluralStringResource(R.plurals.repositories_count, visible.size, visible.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (visible.isEmpty()) {
            Text(
                stringResource(R.string.no_matching_repos),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visible, key = { it.htmlUrl }) { repo ->
                    RepoCard(
                        repo = repo,
                        downloading = repo.downloadKey() in activeDownloads,
                        onClick = { onOpenRepo(repo) },
                        onDownload = { onDownload(repo) },
                        saved = repo.downloadKey() in queuedRepos,
                        onSaveToggle = { onSaveToggle(repo) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortChip(value: RepoSort, selected: RepoSort, label: Int, onSelect: (RepoSort) -> Unit) {
    FilterChip(
        selected = value == selected,
        onClick = { onSelect(value) },
        label = { Text(stringResource(label)) }
    )
}
