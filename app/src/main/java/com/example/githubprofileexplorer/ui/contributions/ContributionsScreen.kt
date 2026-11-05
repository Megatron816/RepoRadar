package com.example.githubprofileexplorer.ui.contributions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.model.ContributionCalendar
import com.example.githubprofileexplorer.data.repository.Profile
import com.example.githubprofileexplorer.data.repository.ProfileError
import com.example.githubprofileexplorer.domain.ContributionSummary
import com.example.githubprofileexplorer.ui.components.CenteredContent
import com.example.githubprofileexplorer.ui.components.EmptyState
import com.example.githubprofileexplorer.ui.components.ErrorState
import com.example.githubprofileexplorer.ui.components.LoadingState
import com.example.githubprofileexplorer.ui.components.SectionCard
import com.example.githubprofileexplorer.ui.components.StatTile
import com.example.githubprofileexplorer.ui.components.errorMessage
import com.example.githubprofileexplorer.ui.components.isRetryable
import java.time.LocalDate

@Composable
fun ContributionsScreen(
    profile: Profile?,
    state: ContributionsState,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
    onGoToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenteredContent(modifier) {
        when {
            profile == null -> EmptyState(
                title = stringResource(R.string.need_profile_title),
                body = stringResource(R.string.need_profile_body),
                icon = Icons.Default.DateRange,
                actionLabel = stringResource(R.string.go_to_profile),
                onAction = onGoToProfile
            )

            state is ContributionsState.SignedOut -> SignInRequired(onSignIn)

            state is ContributionsState.Error && state.error == ProfileError.UNAUTHORIZED -> SignInRequired(onSignIn)

            state is ContributionsState.Error -> ErrorState(
                message = errorMessage(state.error),
                onRetry = if (state.error.isRetryable()) onRetry else null
            )

            state is ContributionsState.Ready -> ContributionsContent(profile.user.login, state.calendar, state.summary)

            else -> LoadingState()
        }
    }
}

@Composable
private fun SignInRequired(onSignIn: () -> Unit) {
    EmptyState(
        title = stringResource(R.string.contributions_signin_title),
        body = stringResource(R.string.contributions_signin_body),
        icon = Icons.Default.Lock,
        actionLabel = stringResource(R.string.sign_in),
        onAction = onSignIn
    )
}

@Composable
private fun ContributionsContent(login: String, calendar: ContributionCalendar, summary: ContributionSummary) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(
            title = stringResource(R.string.contributions_title),
            subtitle = stringResource(R.string.contributions_subtitle, login)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatTile(summary.total.toString(), stringResource(R.string.contributions_total))
                StatTile(summary.longestStreak.toString(), stringResource(R.string.contributions_longest))
                StatTile(summary.currentStreak.toString(), stringResource(R.string.contributions_current))
                StatTile(summary.activeDays.toString(), stringResource(R.string.contributions_active_days))
            }
        }

        SectionCard(title = stringResource(R.string.contributions_heatmap)) {
            Heatmap(calendar, summary)
            Row(
                Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    stringResource(R.string.heatmap_less),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                heatmapColors().forEach { color ->
                    Canvas(Modifier.size(12.dp)) {
                        drawRoundRect(color, cornerRadius = CornerRadius(3.dp.toPx()))
                    }
                }
                Text(
                    stringResource(R.string.heatmap_more),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Heatmap(calendar: ContributionCalendar, summary: ContributionSummary) {
    val colors = heatmapColors()
    val weeks = calendar.weeks
    val description = pluralStringResource(R.plurals.heatmap_description, summary.activeDays, summary.activeDays, summary.total)
    // Row index (Sunday = 0) is derived from the date; the first column may start mid-week.
    val cells = remember(calendar) {
        weeks.mapIndexed { column, week ->
            week.contributionDays.mapIndexed { index, day ->
                val row = runCatching { LocalDate.parse(day.date).dayOfWeek.value % 7 }.getOrDefault(index)
                Triple(column, row, levelIndex(day.contributionLevel))
            }
        }.flatten()
    }

    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(weeks.size.coerceAtLeast(1) / 7f)
            .semantics { contentDescription = description }
    ) {
        val cell = size.width / weeks.size.coerceAtLeast(1)
        val gap = cell * 0.18f
        cells.forEach { (column, row, level) ->
            drawRoundRect(
                color = colors[level],
                topLeft = Offset(column * cell, row * cell),
                size = Size(cell - gap, cell - gap),
                cornerRadius = CornerRadius(cell * 0.22f)
            )
        }
    }
}

private fun levelIndex(level: String): Int = when (level) {
    "FIRST_QUARTILE" -> 1
    "SECOND_QUARTILE" -> 2
    "THIRD_QUARTILE" -> 3
    "FOURTH_QUARTILE" -> 4
    else -> 0
}

@Composable
private fun heatmapColors(): List<Color> =
    if (isSystemInDarkTheme()) {
        listOf(Color(0xFF161B22), Color(0xFF0E4429), Color(0xFF006D32), Color(0xFF26A641), Color(0xFF39D353))
    } else {
        listOf(Color(0xFFEBEDF0), Color(0xFF9BE9A8), Color(0xFF40C463), Color(0xFF30A14E), Color(0xFF216E39))
    }
