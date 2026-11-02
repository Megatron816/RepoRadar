package com.example.githubprofileexplorer.ui.languages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.repository.Profile
import com.example.githubprofileexplorer.domain.LanguageAnalytics
import com.example.githubprofileexplorer.ui.components.AppIcons
import com.example.githubprofileexplorer.ui.components.CenteredContent
import com.example.githubprofileexplorer.ui.components.EmptyState
import com.example.githubprofileexplorer.ui.components.LanguageDot
import com.example.githubprofileexplorer.ui.components.SectionCard
import com.example.githubprofileexplorer.ui.components.languageColor

@Composable
fun LanguagesScreen(
    profile: Profile?,
    onGoToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenteredContent(modifier) {
        if (profile == null) {
            EmptyState(
                title = stringResource(R.string.need_profile_title),
                body = stringResource(R.string.need_profile_body),
                icon = AppIcons.Code,
                actionLabel = stringResource(R.string.go_to_profile),
                onAction = onGoToProfile
            )
            return@CenteredContent
        }

        val slices = remember(profile.repos) { LanguageAnalytics.aggregate(profile.repos) }
        if (slices.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.languages_empty_title),
                body = stringResource(R.string.languages_empty),
                icon = AppIcons.Code
            )
            return@CenteredContent
        }

        val description = slices.take(5).joinToString(", ") { "${it.language} ${it.percentage.toInt()}%" }

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(
                title = stringResource(R.string.languages_title),
                subtitle = stringResource(R.string.languages_subtitle, profile.repos.size)
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(200.dp)
                        .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    Canvas(Modifier.size(200.dp)) {
                        val strokeWidth = 28.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(strokeWidth)
                        )
                        var start = -90f
                        slices.forEach { slice ->
                            val sweep = 360f * slice.percentage / 100f
                            drawArc(
                                color = languageColor(slice.language),
                                startAngle = start,
                                sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(strokeWidth)
                            )
                            start += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(slices.first().language, style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.languages_count, slices.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SectionCard(title = stringResource(R.string.languages_breakdown)) {
                slices.forEach { slice ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LanguageDot(slice.language, size = 12.dp)
                        Text(slice.language, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.languages_repo_count, slice.repositories),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${"%.1f".format(slice.percentage)}%",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
