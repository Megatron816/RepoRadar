package com.example.githubprofileexplorer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.local.ProfileHistoryEntity
import com.example.githubprofileexplorer.ui.components.AppCard

@Composable
fun HomeScreen(
    onSearch: () -> Unit,
    onPortfolio: () -> Unit,
    onCompare: () -> Unit,
    recentProfiles: List<ProfileHistoryEntity>,
    onOpenRecentProfile: (ProfileHistoryEntity) -> Unit,
    onSeeAllRecentProfiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
        Button(
            onClick = onSearch,
            modifier = Modifier.padding(top = 28.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Text(stringResource(R.string.home_search), modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(onClick = onPortfolio, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.home_portfolio))
        }
        OutlinedButton(onClick = onCompare, modifier = Modifier.padding(top = 12.dp)) {
            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null)
            Text(stringResource(R.string.home_compare), modifier = Modifier.padding(start = 8.dp))
        }
        if (recentProfiles.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.home_recent_profiles),
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onSeeAllRecentProfiles) {
                        Text(stringResource(R.string.see_all))
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recentProfiles.take(3), key = { it.login }) { profile ->
                        AppCard(
                            modifier = Modifier.padding(bottom = 4.dp),
                            onClick = { onOpenRecentProfile(profile) }
                        ) {
                            Column(
                                Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    profile.name ?: profile.login,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "@${profile.login}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
