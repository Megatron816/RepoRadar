package com.example.githubprofileexplorer.ui.compare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.repository.GeminiError
import com.example.githubprofileexplorer.ui.components.AppCard
import com.example.githubprofileexplorer.ui.components.CenteredContent
import com.example.githubprofileexplorer.ui.components.CompareSkeleton
import com.example.githubprofileexplorer.ui.components.ErrorState
import com.example.githubprofileexplorer.ui.components.SectionCard
import com.example.githubprofileexplorer.ui.components.errorMessage
import com.example.githubprofileexplorer.ui.components.formatCount
import com.example.githubprofileexplorer.ui.components.sanitizeNormalText

@Composable
fun CompareScreen(
    state: CompareUiState,
    onLeftChange: (String) -> Unit,
    onRightChange: (String) -> Unit,
    onCompare: () -> Unit,
    onGenerateAiComparison: () -> Unit,
    onAiPromptChange: (String) -> Unit,
    onAskAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onCompare()
    }

    CenteredContent(modifier) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 500.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompareField(state.left, onLeftChange, R.string.compare_developer_one, ImeAction.Next)
                        CompareField(
                            state.right,
                            onRightChange,
                            R.string.compare_developer_two,
                            ImeAction.Search,
                            KeyboardActions(onSearch = { if (state.canCompare) submit() })
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompareField(state.left, onLeftChange, R.string.compare_developer_one, ImeAction.Next, modifier = Modifier.weight(1f))
                        CompareField(state.right, onRightChange, R.string.compare_developer_two, ImeAction.Search, KeyboardActions(onSearch = { if (state.canCompare) submit() }), Modifier.weight(1f))
                    }
                }
            }
            Button(onClick = { submit() }, enabled = state.canCompare, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.compare_button))
            }

            if (state.loading) {
                CompareSkeleton()
            }

            state.result?.let {
                ResultSection(
                    result = it,
                    aiState = state.ai,
                    onGenerateAiComparison = onGenerateAiComparison,
                    onAiPromptChange = onAiPromptChange,
                    onAskAi = onAskAi
                )
            }
        }
    }
}

@Composable
private fun CompareField(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions = KeyboardActions(),
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(label), maxLines = 1) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = keyboardActions
    )
}

@Composable
private fun ResultSection(
    result: CompareResult,
    aiState: CompareAiState,
    onGenerateAiComparison: () -> Unit,
    onAiPromptChange: (String) -> Unit,
    onAskAi: () -> Unit
) {
    val left = result.left
    val right = result.right

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SideHeader(left, Modifier.weight(1f))
        SideHeader(right, Modifier.weight(1f))
    }

    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            NumericRow(R.string.followers, left.profile?.user?.followers, right.profile?.user?.followers, true)
            NumericRow(R.string.following, left.profile?.user?.following, right.profile?.user?.following, false)
            NumericRow(R.string.repos, left.profile?.user?.publicRepos, right.profile?.user?.publicRepos, true)
            NumericRow(R.string.compare_total_stars, left.totalStars, right.totalStars, true)
            NumericRow(R.string.compare_total_forks, left.totalForks, right.totalForks, true)
            NumericRow(R.string.compare_total_issues, left.totalOpenIssues, right.totalOpenIssues, false)
            NumericRow(R.string.compare_average_health, left.averageHealth, right.averageHealth, true, compact = false)
            if (result.includesContributions) {
                NumericRow(R.string.compare_contributions, left.contributions, right.contributions, true)
            }

            MetricRow(
                label = stringResource(R.string.compare_top_languages),
                left = left.topLanguages.joinToString(", ").ifBlank { DASH },
                right = right.topLanguages.joinToString(", ").ifBlank { DASH },
                leftWins = false,
                rightWins = false
            )

            MetricRow(
                label = stringResource(R.string.compare_top_repo),
                left = left.topStarredRepo?.let { "${it.name} (${formatCount(it.stargazersCount)}★)" } ?: DASH,
                right = right.topStarredRepo?.let { "${it.name} (${formatCount(it.stargazersCount)}★)" } ?: DASH,
                leftWins = (left.topStarredRepo?.stargazersCount ?: 0) > (right.topStarredRepo?.stargazersCount ?: 0),
                rightWins = (right.topStarredRepo?.stargazersCount ?: 0) > (left.topStarredRepo?.stargazersCount ?: 0)
            )
        }
    }

    AiComparisonSection(
        aiState = aiState,
        leftLogin = left.login,
        rightLogin = right.login,
        onGenerate = onGenerateAiComparison,
        onPromptChange = onAiPromptChange,
        onAskAi = onAskAi
    )

    Text(
        stringResource(R.string.compare_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun AiComparisonSection(
    aiState: CompareAiState,
    leftLogin: String,
    rightLogin: String,
    onGenerate: () -> Unit,
    onPromptChange: (String) -> Unit,
    onAskAi: () -> Unit
) {
    SectionCard(
        title = stringResource(R.string.compare_ai_title),
        subtitle = stringResource(R.string.compare_ai_subtitle)
    ) {
        Button(
            onClick = onGenerate,
            enabled = !aiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (aiState.isLoading) "Generating Comparison..." else stringResource(R.string.compare_ai_generate))
        }
        if (aiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.5.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }

        OutlinedTextField(
            value = aiState.prompt,
            onValueChange = onPromptChange,
            label = { Text(stringResource(R.string.compare_ai_prompt_label)) },
            placeholder = { Text(stringResource(R.string.compare_ai_prompt_hint)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !aiState.isLoading,
            shape = RoundedCornerShape(16.dp),
            minLines = 2
        )

        Button(
            onClick = onAskAi,
            enabled = aiState.prompt.isNotBlank() && !aiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.compare_ai_button))
        }

        aiState.error?.let { error ->
            ErrorState(geminiErrorMessage(error))
        }

        aiState.response?.let { response ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val cards = parseTwoDeveloperResponse(response, leftLogin, rightLogin)
                cards.forEach { card ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                sanitizeNormalText(card.title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            card.content.forEach { paragraph ->
                                val cleaned = sanitizeNormalText(paragraph)
                                if (cleaned.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            "•",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            cleaned,
                                            modifier = Modifier.weight(1f),
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
        }
    }
}

data class DeveloperComparisonCard(
    val title: String,
    val content: List<String>
)

private fun parseTwoDeveloperResponse(response: String, leftLogin: String, rightLogin: String): List<DeveloperComparisonCard> {
    val cleanedResponse = sanitizeNormalText(response)
    val lines = cleanedResponse.lines().map { it.trim() }.filter { it.isNotEmpty() }

    val dev1Title = "Developer 1: @$leftLogin"
    val dev2Title = "Developer 2: @$rightLogin"

    val dev1Content = mutableListOf<String>()
    val dev2Content = mutableListOf<String>()
    var currentDev = 1

    lines.forEach { line ->
        val lower = line.lowercase()
        when {
            lower.contains("developer 2") || (lower.contains(rightLogin.lowercase()) && !lower.contains(leftLogin.lowercase()) && line.length < 50) -> {
                currentDev = 2
            }
            lower.contains("developer 1") || (lower.contains(leftLogin.lowercase()) && !lower.contains(rightLogin.lowercase()) && line.length < 50) -> {
                currentDev = 1
            }
            else -> {
                if (currentDev == 1) {
                    dev1Content.add(line)
                } else {
                    dev2Content.add(line)
                }
            }
        }
    }

    if (dev1Content.isEmpty() && dev2Content.isEmpty()) {
        val mid = lines.size / 2
        dev1Content.addAll(lines.take(mid))
        dev2Content.addAll(lines.drop(mid))
    }

    return listOf(
        DeveloperComparisonCard(dev1Title, dev1Content.ifEmpty { listOf("No analysis generated.") }),
        DeveloperComparisonCard(dev2Title, dev2Content.ifEmpty { listOf("No analysis generated.") })
    )
}

@Composable
private fun NumericRow(label: Int, left: Int?, right: Int?, highlight: Boolean, compact: Boolean = true) {
    val differ = left != null && right != null && left != right
    MetricRow(
        label = stringResource(label),
        left = left?.let { if (compact) formatCount(it) else it.toString() } ?: DASH,
        right = right?.let { if (compact) formatCount(it) else it.toString() } ?: DASH,
        leftWins = highlight && left != null && right != null && left > right,
        rightWins = highlight && left != null && right != null && right > left
    )
}

@Composable
private fun SideHeader(side: CompareSide, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val user = side.profile?.user
            if (user != null) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = stringResource(R.string.avatar_description, user.login),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )
                Text(
                    user.name?.takeIf { it.isNotBlank() } ?: user.login,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    "@${user.login}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(side.login, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                side.error?.let {
                    Text(
                        errorMessage(it),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, left: String, right: String, leftWins: Boolean, rightWins: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ValueCell(left, leftWins, Modifier.weight(1f))
        Text(
            label,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        ValueCell(right, rightWins, Modifier.weight(1f))
    }
}

@Composable
private fun ValueCell(text: String, highlighted: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (highlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

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

private const val DASH = "—"
