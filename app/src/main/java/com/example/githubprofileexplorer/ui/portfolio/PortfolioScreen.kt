package com.example.githubprofileexplorer.ui.portfolio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.local.PortfolioProjectEntity
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.repository.Profile
import com.example.githubprofileexplorer.domain.LanguageAnalytics
import com.example.githubprofileexplorer.domain.PortfolioAnalytics
import com.example.githubprofileexplorer.domain.PortfolioRepoSort
import com.example.githubprofileexplorer.ui.components.AppCard
import com.example.githubprofileexplorer.ui.components.CenteredContent
import com.example.githubprofileexplorer.ui.components.EmptyState
import com.example.githubprofileexplorer.ui.components.SectionCard
import com.example.githubprofileexplorer.ui.components.WrappingRow

@Composable
fun PortfolioScreen(
    profile: Profile?,
    state: PortfolioUiState,
    selectedProjects: List<PortfolioProjectEntity>,
    onOpenProfile: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onTopicChange: (String) -> Unit,
    onMinimumStarsChange: (String) -> Unit,
    onRecentDaysChange: (String) -> Unit,
    onIncludeArchivedChange: (Boolean) -> Unit,
    onIncludeForksChange: (Boolean) -> Unit,
    onSortChange: (PortfolioRepoSort) -> Unit,
    onClearFilters: () -> Unit,
    onToggleSaved: () -> Unit,
    onToggleProject: (GitHubRepo) -> Unit,
    onExport: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (profile == null) {
        CenteredContent(modifier) {
            EmptyState(
                title = stringResource(R.string.need_profile_title),
                body = stringResource(R.string.portfolio_need_profile),
                icon = Icons.Default.Assessment,
                actionLabel = stringResource(R.string.go_to_profile),
                onAction = onOpenProfile
            )
        }
        return
    }

    val repos = remember(profile.repos, state.filters, state.sort) {
        PortfolioAnalytics.filterAndSort(profile.repos, state.filters, state.sort)
    }
    val languages = remember(profile.repos) { LanguageAnalytics.aggregate(profile.repos) }
    val topics = remember(profile.repos) { PortfolioAnalytics.topics(profile.repos) }
    val selectedKeys = remember(selectedProjects) { selectedProjects.map { it.repositoryKey }.toSet() }

    CenteredContent(modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val displayName = buildString {
                            profile.user.name?.takeIf(String::isNotBlank)?.let {
                                append(it)
                                append("  ")
                            }
                            append("(@${profile.user.login})")
                        }
                        Text(
                            displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        profile.user.bio?.takeIf(String::isNotBlank)?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            PortfolioStat(profile.repos.size.toString(), stringResource(R.string.portfolio_repositories_stat))
                            PortfolioStat(selectedProjects.size.toString(), stringResource(R.string.portfolio_selected_stat))
                        }
                        Text(stringResource(R.string.portfolio_disclosure), style = MaterialTheme.typography.bodySmall)
                        val fetchedAt = state.lastFetchedAt?.let {
                            java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))
                        } ?: stringResource(R.string.not_available)
                        Text(
                            stringResource(R.string.portfolio_fetched_at, fetchedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(stringResource(R.string.portfolio_actions), style = MaterialTheme.typography.labelLarge)
                        val isGenerating = state.exportState is PortfolioExportState.Generating
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = onToggleSaved, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(if (state.saved) R.string.remove_saved_profile else R.string.save_profile))
                            }
                            OutlinedButton(
                                onClick = onExport,
                                enabled = !isGenerating,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_portfolio))
                                Text(if (isGenerating) "Exporting Portfolio..." else stringResource(R.string.share_portfolio), modifier = Modifier.padding(start = 4.dp))
                            }
                            if (isGenerating) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.5.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            }
                        }
                        when (state.exportState) {
                            PortfolioExportState.Error -> Text(
                                stringResource(R.string.portfolio_export_error),
                                color = MaterialTheme.colorScheme.error
                            )
                            else -> Unit
                        }
                        OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.refresh_profile))
                        }
                    }
                }
            }
            item {
                SectionCard(
                    title = stringResource(R.string.portfolio_filters),
                    subtitle = stringResource(R.string.portfolio_filters_hint)
                ) {
                    OutlinedTextField(
                        value = state.filters.language.orEmpty(),
                        onValueChange = onLanguageChange,
                        label = { Text(stringResource(R.string.filter_language)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.filters.topic.orEmpty(),
                        onValueChange = onTopicChange,
                        label = { Text(stringResource(R.string.filter_topic)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.minimumStarsText,
                            onValueChange = onMinimumStarsChange,
                            label = { Text(stringResource(R.string.minimum_stars)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.recentDaysText,
                            onValueChange = onRecentDaysChange,
                            label = { Text(stringResource(R.string.updated_within_days)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    WrappingRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.filters.includeArchived,
                            onClick = { onIncludeArchivedChange(!state.filters.includeArchived) },
                            label = { Text(stringResource(R.string.include_archived)) }
                        )
                        FilterChip(
                            selected = state.filters.includeForks,
                            onClick = { onIncludeForksChange(!state.filters.includeForks) },
                            label = { Text(stringResource(R.string.include_forks)) }
                        )
                    }
                    Text(stringResource(R.string.sort_by), style = MaterialTheme.typography.labelLarge)
                    WrappingRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.sort == PortfolioRepoSort.UPDATED,
                            onClick = { onSortChange(PortfolioRepoSort.UPDATED) },
                            label = { Text(stringResource(R.string.sort_updated)) }
                        )
                        FilterChip(
                            selected = state.sort == PortfolioRepoSort.STARS,
                            onClick = { onSortChange(PortfolioRepoSort.STARS) },
                            label = { Text(stringResource(R.string.sort_stars)) }
                        )
                        FilterChip(
                            selected = state.sort == PortfolioRepoSort.NAME,
                            onClick = { onSortChange(PortfolioRepoSort.NAME) },
                            label = { Text(stringResource(R.string.sort_name)) }
                        )
                    }
                    if (state.filters.isActive) {
                        OutlinedButton(onClick = onClearFilters) { Text(stringResource(R.string.clear_filters)) }
                    }
                }
            }
            item {
                SectionCard(title = stringResource(R.string.portfolio_overview)) {
                    Text(
                        stringResource(
                            R.string.portfolio_language_summary,
                            languages.map {
                                stringResource(
                                    R.string.portfolio_language_item,
                                    it.language,
                                    it.repositories,
                                    it.percentage.toInt()
                                )
                            }.joinToString().ifBlank { stringResource(R.string.not_available) }
                        )
                    )
                    Text(
                        stringResource(
                            R.string.portfolio_topics_summary,
                            topics.take(5).map {
                                stringResource(R.string.portfolio_topic_item, it.topic, it.repositories)
                            }.joinToString().ifBlank { stringResource(R.string.not_available) }
                        )
                    )
                    Text(stringResource(R.string.portfolio_context_note))
                }
            }
            item {
                SectionCard(
                    title = stringResource(R.string.selected_projects),
                    subtitle = stringResource(R.string.portfolio_matching_count, repos.size, profile.repos.size)
                ) {
                    if (repos.isEmpty()) {
                        Text(
                            stringResource(R.string.no_matching_projects_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            repos.forEachIndexed { index, repo ->
                                val key = repo.id.takeIf { it > 0 }?.let { "github-repo:$it" }
                                    ?: "url:${repo.htmlUrl.trimEnd('/').lowercase()}"
                                val isSelected = key in selectedKeys

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (index > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                    Text(
                                        repo.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    repo.description?.takeIf(String::isNotBlank)?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    WrappingRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            repo.language ?: stringResource(R.string.language_not_specified),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.height(16.dp))
                                            Text(
                                                stringResource(R.string.repository_stars, repo.stargazersCount),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onToggleProject(repo) },
                                        label = {
                                            Text(stringResource(if (isSelected) R.string.deselect_project else R.string.select_project))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PortfolioStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
