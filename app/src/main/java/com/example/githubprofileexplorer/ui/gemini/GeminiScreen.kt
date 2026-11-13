package com.example.githubprofileexplorer.ui.gemini

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.repository.GeminiError
import com.example.githubprofileexplorer.ui.components.CenteredContent
import com.example.githubprofileexplorer.ui.components.ErrorState
import com.example.githubprofileexplorer.ui.components.SectionCard
import com.example.githubprofileexplorer.ui.components.sanitizeNormalText

@Composable
fun GeminiScreen(
    state: GeminiUiState,
    onPromptChange: (String) -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenteredContent(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(
                title = stringResource(R.string.gemini_title),
                subtitle = stringResource(R.string.gemini_subtitle)
            ) {
                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = onPromptChange,
                    label = { Text(stringResource(R.string.gemini_prompt_label)) },
                    placeholder = { Text(stringResource(R.string.gemini_prompt_hint)) },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                )
                Button(
                    onClick = onGenerate,
                    enabled = state.prompt.isNotBlank() && !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isLoading) "Generating Response..." else stringResource(R.string.gemini_generate))
                }
                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
            }

            state.error?.let { error ->
                ErrorState(geminiErrorMessage(error))
            }

            state.response?.let { response ->
                SectionCard(title = stringResource(R.string.gemini_response_title)) {
                    Text(
                        text = sanitizeNormalText(response),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun geminiErrorMessage(error: GeminiError): String =
    when (error) {
        GeminiError.MissingApiKey ->
            stringResource(R.string.gemini_error_missing_key)
        GeminiError.InvalidApiKey ->
            stringResource(R.string.gemini_error_invalid_key)
        GeminiError.NoInternet ->
            stringResource(R.string.gemini_error_network)
        GeminiError.RateLimited ->
            stringResource(R.string.gemini_error_rate_limit)
        GeminiError.InvalidRequest ->
            stringResource(R.string.gemini_error_bad_request)
        GeminiError.PermissionDenied ->
            stringResource(R.string.gemini_error_permission)
        GeminiError.ModelNotFound ->
            stringResource(R.string.gemini_error_model)
        GeminiError.Server ->
            stringResource(R.string.gemini_error_server)
        GeminiError.Timeout ->
            stringResource(R.string.gemini_error_timeout)
        GeminiError.EmptyResponse ->
            stringResource(R.string.gemini_error_empty)
        GeminiError.Unknown ->
            stringResource(R.string.gemini_error_unknown)
    }
