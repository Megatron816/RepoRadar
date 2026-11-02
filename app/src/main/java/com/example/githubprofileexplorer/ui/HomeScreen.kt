package com.example.githubprofileexplorer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.R

@Composable
fun HomeScreen(
    onSearch: () -> Unit,
    onCompare: () -> Unit,
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
        Text(
            stringResource(R.string.home_body),
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onSearch) {
            Icon(Icons.Default.Search, contentDescription = null)
            Text(stringResource(R.string.home_search), modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(onClick = onCompare, modifier = Modifier.padding(top = 12.dp)) {
            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null)
            Text(stringResource(R.string.home_compare), modifier = Modifier.padding(start = 8.dp))
        }
    }
}
