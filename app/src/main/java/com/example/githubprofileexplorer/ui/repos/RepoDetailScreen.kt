package com.example.githubprofileexplorer.ui.repos

import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.domain.HealthReason
import com.example.githubprofileexplorer.domain.RepoHealthScorer
import com.example.githubprofileexplorer.ui.components.AppCard
import com.example.githubprofileexplorer.ui.components.AppIcons
import com.example.githubprofileexplorer.ui.components.CenteredContent
import com.example.githubprofileexplorer.ui.components.HealthBadge
import com.example.githubprofileexplorer.ui.components.LanguageLabel
import com.example.githubprofileexplorer.ui.components.MarkdownText
import com.example.githubprofileexplorer.ui.components.SectionCard
import com.example.githubprofileexplorer.ui.components.StatTile
import com.example.githubprofileexplorer.ui.components.TagChip
import com.example.githubprofileexplorer.ui.components.WrappingRow
import com.example.githubprofileexplorer.ui.components.errorMessage
import com.example.githubprofileexplorer.ui.components.formatCount
import com.example.githubprofileexplorer.ui.components.formatIsoDate
import com.example.githubprofileexplorer.ui.profile.ReadmeState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RepoDetailScreen(
    repo: GitHubRepo,
    readme: ReadmeState,
    downloading: Boolean,
    onDownload: () -> Unit,
    onExploreArchitecture: () -> Unit = {},
    modifier: Modifier = Modifier,
    saved: Boolean = false,
    onSaveToggle: () -> Unit = {}
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val health = remember(repo) { RepoHealthScorer().score(repo) }

    CenteredContent(modifier) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(repo.name, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        repo.fullName ?: repo.ownerLogin,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    repo.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    WrappingRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HealthBadge(health)
                        repo.language?.takeIf { it.isNotBlank() }?.let { LanguageLabel(it) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatTile(formatCount(repo.stargazersCount), stringResource(R.string.label_stars))
                        StatTile(formatCount(repo.forksCount), stringResource(R.string.label_forks))
                        StatTile(formatCount(repo.openIssuesCount), stringResource(R.string.label_issues))
                    }
                    InfoLine(R.string.info_license, repo.license?.name ?: stringResource(R.string.info_none))
                    InfoLine(R.string.info_branch, repo.defaultBranch)
                    formatIsoDate(repo.pushedAt)?.let { InfoLine(R.string.info_last_push, it) }
                    if (repo.size > 0) {
                        InfoLine(R.string.info_size, Formatter.formatShortFileSize(context, repo.size * 1024))
                    }
                    if (repo.topics.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) { repo.topics.forEach { TagChip(it) } }
                    }
                }
            }

            Button(onClick = onDownload, enabled = !downloading, modifier = Modifier.fillMaxWidth()) {
                if (downloading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.downloading), Modifier.padding(start = 8.dp))
                } else {
                    Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.download_zip), Modifier.padding(start = 8.dp))
                }
            }
            OutlinedButton(onClick = onSaveToggle, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (saved) R.string.remove_from_queue else R.string.save_to_queue))
            }
            Button(onClick = onExploreArchitecture, modifier = Modifier.fillMaxWidth()) {
                Icon(AppIcons.Code, contentDescription = stringResource(R.string.architecture_title), modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.architecture_scan_button), Modifier.padding(start = 8.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { runCatching { uriHandler.openUri(repo.htmlUrl) } },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(AppIcons.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.open_browser), Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = { shareRepo(context, repo) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.share_repository), Modifier.padding(start = 8.dp))
                }
            }

            SectionCard(title = stringResource(R.string.health_title)) {
                health.reasons.forEach { reason ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", style = MaterialTheme.typography.bodyMedium)
                        Text(healthReasonText(reason), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            SectionCard(title = stringResource(R.string.readme_title)) {
                when (readme) {
                    ReadmeState.Idle, ReadmeState.Loading -> CircularProgressIndicator()
                    ReadmeState.Unavailable -> Text(
                        stringResource(R.string.readme_unavailable),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    is ReadmeState.Error -> Text(
                        errorMessage(readme.error),
                        color = MaterialTheme.colorScheme.error
                    )
                    is ReadmeState.Ready -> MarkdownText(readme.blocks)
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: Int, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(label),
            modifier = Modifier.weight(0.42f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            modifier = Modifier.weight(0.58f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun healthReasonText(reason: HealthReason): String = stringResource(
    when (reason) {
        HealthReason.UPDATED_RECENTLY -> R.string.reason_updated_recently
        HealthReason.UPDATED_THIS_YEAR -> R.string.reason_updated_this_year
        HealthReason.NO_PUSH_IN_OVER_A_YEAR -> R.string.reason_no_push_over_year
        HealthReason.NO_PUSH_DATE -> R.string.reason_no_push_date
        HealthReason.HAS_LICENSE -> R.string.reason_has_license
        HealthReason.NO_LICENSE -> R.string.reason_no_license
        HealthReason.HAS_CONTEXT -> R.string.reason_has_context
        HealthReason.MANY_OPEN_ISSUES -> R.string.reason_many_issues
        HealthReason.COMMUNITY_INTEREST -> R.string.reason_community
        HealthReason.ARCHIVED -> R.string.reason_archived
        HealthReason.FORKED -> R.string.reason_forked
    }
)

private fun shareRepo(context: Context, repo: GitHubRepo) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, repo.htmlUrl)
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_repository)))
}
