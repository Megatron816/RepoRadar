package com.example.githubprofileexplorer.ui.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.githubprofileexplorer.ui.components.AppCard
import com.example.githubprofileexplorer.ui.components.CenteredContent
import com.example.githubprofileexplorer.ui.components.errorMessage
import com.example.githubprofileexplorer.ui.components.formatCount

@Composable
fun CompareScreen(
    state: CompareUiState,
    onLeftChange: (String) -> Unit,
    onRightChange: (String) -> Unit,
    onCompare: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.left,
                    onValueChange = onLeftChange,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.compare_developer_one)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = state.right,
                    onValueChange = onRightChange,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.compare_developer_two)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (state.canCompare) submit() })
                )
            }
            Button(onClick = { submit() }, enabled = state.canCompare, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.compare_button))
            }

            if (state.loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }

            state.result?.let { ResultSection(it) }
        }
    }
}

@Composable
private fun ResultSection(result: CompareResult) {
    val left = result.left
    val right = result.right

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SideHeader(left, Modifier.weight(1f))
        SideHeader(right, Modifier.weight(1f))
    }

    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            numericRow(R.string.followers, left.profile?.user?.followers, right.profile?.user?.followers, true)
            numericRow(R.string.following, left.profile?.user?.following, right.profile?.user?.following, false)
            numericRow(R.string.repos, left.profile?.user?.publicRepos, right.profile?.user?.publicRepos, true)
            numericRow(R.string.compare_total_stars, left.totalStars, right.totalStars, true)
            numericRow(R.string.compare_average_health, left.averageHealth, right.averageHealth, true, compact = false)
            if (result.includesContributions) {
                numericRow(R.string.compare_contributions, left.contributions, right.contributions, true)
            }
            MetricRow(
                label = stringResource(R.string.compare_top_languages),
                left = left.topLanguages.joinToString(", ").ifBlank { DASH },
                right = right.topLanguages.joinToString(", ").ifBlank { DASH },
                leftWins = false,
                rightWins = false
            )
        }
    }

    Text(
        stringResource(R.string.compare_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun numericRow(label: Int, left: Int?, right: Int?, highlight: Boolean, compact: Boolean = true) {
    val differ = left != null && right != null && left != right
    MetricRow(
        label = stringResource(label),
        left = left?.let { if (compact) formatCount(it) else it.toString() } ?: DASH,
        right = right?.let { if (compact) formatCount(it) else it.toString() } ?: DASH,
        leftWins = highlight && differ && left!! > right!!,
        rightWins = highlight && differ && right!! > left!!
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
            textAlign = TextAlign.Center
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

private const val DASH = "—"
