package com.example.githubprofileexplorer.ui.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.auth.AuthFailure
import com.example.githubprofileexplorer.ui.components.CenteredContent
import com.example.githubprofileexplorer.ui.components.SectionCard
import com.example.githubprofileexplorer.ui.components.SkeletonBox

@Composable
fun AccountScreen(
    state: AuthState,
    isConfigured: Boolean,
    remainingRequests: Int?,
    onSignIn: () -> Unit,
    onCancel: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenteredContent(modifier) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state) {
                AuthState.SignedIn -> SectionCard(
                    title = stringResource(R.string.account_signed_in_title),
                    subtitle = stringResource(R.string.account_signed_in_body)
                ) {
                    OutlinedButton(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
                }

                AuthState.Starting -> SectionCard(title = stringResource(R.string.account_starting)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SkeletonBox(Modifier.fillMaxWidth(0.6f).height(16.dp))
                        SkeletonBox(Modifier.fillMaxWidth().height(36.dp), RoundedCornerShape(8.dp))
                    }
                }

                is AuthState.AwaitingVerification -> VerificationCard(state, onCancel)

                AuthState.SignedOut, is AuthState.Error -> {
                    if (state is AuthState.Error) {
                        Text(
                            text = authFailureText(state.failure),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    SectionCard(
                        title = stringResource(R.string.account_signed_out_title),
                        subtitle = stringResource(R.string.account_signed_out_body)
                    ) {
                        if (!isConfigured) {
                            Text(
                                stringResource(R.string.account_not_configured),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = onSignIn, enabled = isConfigured) {
                            Text(stringResource(R.string.sign_in))
                        }
                    }
                }
            }

            SectionCard(title = stringResource(R.string.account_api_title)) {
                Text(
                    text = remainingRequests?.let { stringResource(R.string.api_requests_remaining, it) }
                        ?: stringResource(R.string.api_requests_unknown),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    stringResource(R.string.account_api_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VerificationCard(state: AuthState.AwaitingVerification, onCancel: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    SectionCard(
        title = stringResource(R.string.account_verify_title),
        subtitle = stringResource(R.string.account_verify_body)
    ) {
        Text(
            text = state.userCode,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { copyCode(context, state.userCode) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.copy_code))
            }
            Button(
                onClick = { runCatching { uriHandler.openUri(state.url) } },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.open_github)) }
        }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.5.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.account_waiting),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

private fun copyCode(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("GitHub device code", code))
}

@Composable
private fun authFailureText(failure: AuthFailure): String = stringResource(
    when (failure) {
        AuthFailure.NOT_CONFIGURED -> R.string.auth_error_not_configured
        AuthFailure.ACCESS_DENIED -> R.string.auth_error_denied
        AuthFailure.EXPIRED -> R.string.auth_error_expired
        AuthFailure.NETWORK -> R.string.error_network
        AuthFailure.UNKNOWN -> R.string.auth_error_unknown
    }
)
