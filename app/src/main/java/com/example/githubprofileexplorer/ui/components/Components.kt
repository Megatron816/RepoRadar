package com.example.githubprofileexplorer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.repository.ProfileError
import com.example.githubprofileexplorer.domain.HealthLabel
import com.example.githubprofileexplorer.domain.HealthScore
import com.example.githubprofileexplorer.ui.theme.HealthAmberDark
import com.example.githubprofileexplorer.ui.theme.HealthAmberLight
import com.example.githubprofileexplorer.ui.theme.HealthGreenDark
import com.example.githubprofileexplorer.ui.theme.HealthGreenLight
import com.example.githubprofileexplorer.ui.theme.HealthRedDark
import com.example.githubprofileexplorer.ui.theme.HealthRedLight
import java.text.DateFormat
import java.time.Instant
import java.util.Date

/** Keeps content readable on tablets by limiting its width and centering it. */
@Composable
fun CenteredContent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 720.dp).fillMaxSize()) { content() }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = border,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = border,
            content = content
        )
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AppCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    extra: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            title,
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            body,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = 20.dp)) { Text(actionLabel) }
        }
        if (extra != null) {
            Box(Modifier.padding(top = 20.dp)) { extra() }
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    EmptyState(
        title = stringResource(R.string.error_title),
        body = message,
        icon = Icons.Default.Warning,
        modifier = modifier,
        actionLabel = if (onRetry != null) stringResource(R.string.retry) else null,
        onAction = { onRetry?.invoke() }
    )
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TagChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun LanguageDot(language: String, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(languageColor(language))
    )
}

@Composable
fun LanguageLabel(language: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LanguageDot(language)
        Text(language, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun healthColor(label: HealthLabel): Color {
    val dark = isSystemInDarkTheme()
    return when (label) {
        HealthLabel.HEALTHY -> if (dark) HealthGreenDark else HealthGreenLight
        HealthLabel.FAIR -> if (dark) HealthAmberDark else HealthAmberLight
        HealthLabel.STALE -> if (dark) HealthRedDark else HealthRedLight
    }
}

@Composable
fun healthLabelText(label: HealthLabel): String = stringResource(
    when (label) {
        HealthLabel.HEALTHY -> R.string.health_healthy
        HealthLabel.FAIR -> R.string.health_fair
        HealthLabel.STALE -> R.string.health_stale
    }
)

@Composable
fun HealthBadge(score: HealthScore, modifier: Modifier = Modifier) {
    val color = healthColor(score.label)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = "${healthLabelText(score.label)} · ${score.value}",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    submitEnabled: Boolean = value.isNotBlank()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { if (submitEnabled) onSearch() }),
        trailingIcon = {
            IconButton(onClick = onSearch, enabled = enabled && submitEnabled) {
                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_button))
            }
        }
    )
}

@Composable
fun errorMessage(error: ProfileError): String = stringResource(
    when (error) {
        ProfileError.INVALID_USERNAME -> R.string.error_invalid_username
        ProfileError.NOT_FOUND -> R.string.error_not_found
        ProfileError.RATE_LIMITED -> R.string.error_rate_limited
        ProfileError.UNAUTHORIZED -> R.string.error_unauthorized
        ProfileError.NETWORK -> R.string.error_network
        ProfileError.SERVER -> R.string.error_server
        ProfileError.UNKNOWN -> R.string.error_unknown
    }
)

fun ProfileError.isRetryable(): Boolean = this != ProfileError.INVALID_USERNAME && this != ProfileError.NOT_FOUND

/** 1250 -> "1.2k", 3_400_000 -> "3.4M". */
fun formatCount(value: Int): String = when {
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000f)
    value >= 10_000 -> "${value / 1_000}k"
    value >= 1_000 -> "%.1fk".format(value / 1_000f)
    else -> value.toString()
}

/** Formats an ISO-8601 timestamp from GitHub as a short localized date, or null if it cannot be parsed. */
fun formatIsoDate(iso: String?): String? =
    iso?.let { runCatching { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date.from(Instant.parse(it))) }.getOrNull() }
