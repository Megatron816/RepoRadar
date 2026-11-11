package com.example.githubprofileexplorer.ui.local

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.local.ProfileHistoryEntity
import com.example.githubprofileexplorer.ui.components.AppCard
import com.example.githubprofileexplorer.ui.components.EmptyState

@Composable
fun ProfileHistoryScreen(
    items: List<ProfileHistoryEntity>,
    onOpen: (ProfileHistoryEntity) -> Unit,
    onRefresh: (ProfileHistoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.profile_history_empty_title),
            body = stringResource(R.string.profile_history_empty_body),
            icon = Icons.Default.History,
            modifier = modifier
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.login }) { item ->
            AppCard(Modifier.fillMaxWidth(), onClick = { onOpen(item) }) {
                Row(Modifier.padding(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name ?: item.login, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "@${item.login}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.profile_history_stats, item.followers, item.publicRepos),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onRefresh(item) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_profile))
                    }
                }
            }
        }
    }
}
