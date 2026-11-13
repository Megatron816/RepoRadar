package com.example.githubprofileexplorer.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import coil3.compose.AsyncImage
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.model.GithubUser
import com.example.githubprofileexplorer.data.repository.Profile
import com.example.githubprofileexplorer.domain.ActivityLevel
import com.example.githubprofileexplorer.domain.ProfileInsights
import com.example.githubprofileexplorer.domain.ProfileInsightsGenerator
import com.example.githubprofileexplorer.ui.components.AppCard
import com.example.githubprofileexplorer.ui.components.AppSearchField
import com.example.githubprofileexplorer.ui.components.CenteredContent
import com.example.githubprofileexplorer.ui.components.EmptyState
import com.example.githubprofileexplorer.ui.components.ErrorState
import com.example.githubprofileexplorer.ui.components.LoadingState
import com.example.githubprofileexplorer.ui.components.RepoCardSkeleton
import com.example.githubprofileexplorer.ui.components.SectionCard
import com.example.githubprofileexplorer.ui.components.StatTile
import com.example.githubprofileexplorer.ui.components.errorMessage
import com.example.githubprofileexplorer.ui.components.formatCount
import com.example.githubprofileexplorer.ui.components.isRetryable
import com.example.githubprofileexplorer.ui.repos.RepoCard
import com.example.githubprofileexplorer.ui.repos.downloadKey

private val ExampleUsernames = listOf("octocat", "torvalds", "gaearon")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    activeDownloads: Set<String>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onExampleClick: (String) -> Unit,
    onRetryRepos: () -> Unit,
    onOpenRepo: (GitHubRepo) -> Unit,
    onSeeAllRepos: () -> Unit,
    onDownload: (GitHubRepo) -> Unit,
    modifier: Modifier = Modifier,
    queuedRepos: Set<String> = emptySet(),
    onSaveToggle: (GitHubRepo) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current

    CenteredContent(modifier) {
        Column(Modifier.fillMaxSize()) {
            AppSearchField(
                value = state.query,
                onValueChange = onQueryChange,
                label = stringResource(R.string.search_label),
                onSearch = { submit(focusManager, onSearch) },
                enabled = state.search !is SearchState.Loading,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )

            when (val search = state.search) {
                SearchState.Idle -> EmptyState(
                    title = stringResource(R.string.idle_title),
                    body = stringResource(R.string.idle_body),
                    icon = Icons.Default.Search,
                    extra = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.try_examples),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                            ) {
                                ExampleUsernames.forEach { name ->
                                    AssistChip(onClick = { onExampleClick(name) }, label = { Text(name) })
                                }
                            }
                        }
                    }
                )

                SearchState.Loading -> LoadingState()

                is SearchState.Error -> ErrorState(
                    message = errorMessage(search.error),
                    onRetry = if (search.error.isRetryable()) {
                        { submit(focusManager, onSearch) }
                    } else null
                )

                is SearchState.Success -> ProfileContent(
                    profile = search.profile,
                    reloadingRepos = search.reloadingRepos,
                    focusManager = focusManager,
                    activeDownloads = activeDownloads,
                    onRetryRepos = onRetryRepos,
                    onOpenRepo = onOpenRepo,
                    onSeeAllRepos = onSeeAllRepos,
                    onDownload = onDownload
                    , queuedRepos = queuedRepos, onSaveToggle = onSaveToggle
                )
            }
        }
    }
}

private fun submit(focusManager: FocusManager, onSearch: () -> Unit) {
    focusManager.clearFocus()
    onSearch()
}

@Composable
private fun ProfileContent(
    profile: Profile,
    reloadingRepos: Boolean,
    focusManager: FocusManager,
    activeDownloads: Set<String>,
    onRetryRepos: () -> Unit,
    onOpenRepo: (GitHubRepo) -> Unit,
    onSeeAllRepos: () -> Unit,
    onDownload: (GitHubRepo) -> Unit
    , queuedRepos: Set<String>, onSaveToggle: (GitHubRepo) -> Unit
) {
    val insights = remember(profile.repos) { ProfileInsightsGenerator.generate(profile.repos) }
    val topRepos = remember(profile.repos) { profile.repos.sortedByDescending { it.stargazersCount }.take(3) }
    val dismissKeyboardOnScroll = remember(focusManager) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    focusManager.clearFocus()
                }
                return Offset.Zero
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(dismissKeyboardOnScroll),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") { ProfileHeader(profile.user) }

        if (reloadingRepos) {
            item(key = "repos-loading") {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    RepoCardSkeleton()
                    RepoCardSkeleton()
                }
            }
        } else if (profile.reposError != null) {
            item(key = "repos-error") {
                AppCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            errorMessage(profile.reposError),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = onRetryRepos) { Text(stringResource(R.string.retry)) }
                    }
                }
            }
        } else {
            item(key = "summary") { SummaryCard(insights) }

            if (topRepos.isNotEmpty()) {
                item(key = "top-title") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.top_repositories),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = onSeeAllRepos) { Text(stringResource(R.string.see_all)) }
                    }
                }
                items(topRepos, key = { "top-" + it.htmlUrl }) { repo ->
                    RepoCard(
                        repo = repo,
                        downloading = repo.downloadKey() in activeDownloads,
                        onClick = { onOpenRepo(repo) },
                        onDownload = { onDownload(repo) }
                        , saved = repo.downloadKey() in queuedRepos,
                        onSaveToggle = { onSaveToggle(repo) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(user: GithubUser) {
    val uriHandler = LocalUriHandler.current
    AppCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = stringResource(R.string.avatar_description, user.login),
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
            )
            Text(
                text = user.name?.takeIf { it.isNotBlank() } ?: user.login,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "@${user.login}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            user.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                Text(
                    text = bio,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatTile(formatCount(user.followers), stringResource(R.string.followers))
                    StatTile(formatCount(user.following), stringResource(R.string.following))
                    StatTile(formatCount(user.publicRepos), stringResource(R.string.repos))
                }
            }
            OutlinedButton(
                onClick = { runCatching { uriHandler.openUri("https://github.com/${user.login}") } },
                modifier = Modifier.padding(top = 16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) { Text(stringResource(R.string.view_on_github)) }
        }
    }
}

@Composable
private fun SummaryCard(insights: ProfileInsights) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.summary_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.summary_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!insights.hasData) {
                Text(
                    stringResource(R.string.summary_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                insightLines(insights).forEach { line ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            "•",
                            modifier = Modifier.width(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(line, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun insightLines(insights: ProfileInsights): List<String> {
    val lines = mutableListOf<String>()

    val primary = insights.primaryLanguage
    val others = insights.otherLanguages
    lines += when {
        primary == null -> stringResource(R.string.insight_no_language)
        others.isEmpty() -> stringResource(R.string.insight_language_one, primary)
        others.size == 1 -> stringResource(R.string.insight_language_two, primary, others[0])
        else -> stringResource(R.string.insight_language_three, primary, others[0], others[1])
    }

    lines += stringResource(R.string.insight_repos, insights.ownedRepos, insights.forkedRepos)

    if (insights.topRepoName != null) {
        lines += stringResource(
            R.string.insight_top_repo,
            insights.topRepoName,
            formatCount(insights.topRepoStars),
            formatCount(insights.totalStars)
        )
    }

    when (insights.activity) {
        ActivityLevel.VERY_ACTIVE -> lines += stringResource(R.string.insight_activity_very_active)
        ActivityLevel.ACTIVE -> lines += stringResource(R.string.insight_activity_active)
        ActivityLevel.OCCASIONAL -> lines += stringResource(R.string.insight_activity_occasional)
        ActivityLevel.DORMANT -> lines += stringResource(R.string.insight_activity_dormant)
        ActivityLevel.UNKNOWN -> Unit
    }

    if (insights.topTopics.isNotEmpty()) {
        lines += stringResource(R.string.insight_topics, insights.topTopics.joinToString(", "))
    }

    val licensed = insights.licensedPercent
    val health = insights.averageHealth
    if (licensed != null && health != null) {
        lines += stringResource(R.string.insight_quality, licensed, health)
    }

    if (insights.archivedRepos > 0) {
        lines += stringResource(R.string.insight_archived, insights.archivedRepos)
    }
    return lines
}
