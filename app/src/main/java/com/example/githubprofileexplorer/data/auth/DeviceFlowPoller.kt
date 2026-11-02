package com.example.githubprofileexplorer.data.auth

import com.example.githubprofileexplorer.data.model.OAuthTokenResponse
import kotlinx.coroutines.delay

class DeviceFlowException(message: String) : Exception(message)

/** Polls GitHub for the device-flow token, honouring `slow_down` and expiry. */
class DeviceFlowPoller(
    private val poll: suspend () -> OAuthTokenResponse,
    private val wait: suspend (Long) -> Unit = { delay(it * 1000) }
) {
    suspend fun awaitToken(intervalSeconds: Long, expiresInSeconds: Long): String {
        var interval = intervalSeconds
        var elapsed = 0L
        while (elapsed < expiresInSeconds) {
            val response = poll()
            response.access_token?.let { return it }
            when (response.error) {
                "authorization_pending" -> Unit
                "slow_down" -> interval += 5
                "access_denied" -> throw DeviceFlowException("access_denied")
                "expired_token" -> throw DeviceFlowException("expired_token")
                else -> throw DeviceFlowException(response.error ?: "oauth_error")
            }
            wait(interval)
            elapsed += interval
        }
        throw DeviceFlowException("expired_token")
    }
}
