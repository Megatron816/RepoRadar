package com.example.githubprofileexplorer.data.auth

import com.example.githubprofileexplorer.BuildConfig
import com.example.githubprofileexplorer.data.model.OAuthTokenResponse
import com.example.githubprofileexplorer.data.remote.OAuthApi
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

enum class AuthFailure { NOT_CONFIGURED, ACCESS_DENIED, EXPIRED, NETWORK, UNKNOWN }

class AuthException(val failure: AuthFailure) : Exception(failure.name)

class AuthRepository(
    private val store: SecureTokenStore,
    private val api: OAuthApi,
    private val clientId: String = BuildConfig.GITHUB_OAUTH_CLIENT_ID
) {
    data class DeviceSession(
        val deviceCode: String,
        val userCode: String,
        val verificationUrl: String,
        val intervalSeconds: Long,
        val expiresInSeconds: Long
    )

    val isConfigured: Boolean get() = clientId.isNotBlank()

    fun isSignedIn(): Boolean = store.isLoggedIn()

    fun signOut() = store.clear()

    suspend fun requestDeviceSession(): Result<DeviceSession> = attempt {
        if (!isConfigured) throw AuthException(AuthFailure.NOT_CONFIGURED)
        val response = api.requestDeviceCode(clientId)
        val body = response.body()
        if (!response.isSuccessful || body == null) throw AuthException(AuthFailure.UNKNOWN)
        val deviceCode = body.device_code ?: throw AuthException(AuthFailure.UNKNOWN)
        val userCode = body.user_code ?: throw AuthException(AuthFailure.UNKNOWN)
        DeviceSession(
            deviceCode = deviceCode,
            userCode = userCode,
            verificationUrl = body.verification_uri ?: DEFAULT_VERIFICATION_URL,
            intervalSeconds = body.interval,
            expiresInSeconds = body.expires_in
        )
    }

    suspend fun completeSignIn(session: DeviceSession): Result<Unit> = attempt {
        val token = DeviceFlowPoller(
            poll = {
                api.pollToken(clientId, session.deviceCode).body()
                    ?: OAuthTokenResponse(access_token = null, error = "empty_response")
            }
        ).awaitToken(session.intervalSeconds, session.expiresInSeconds)
        store.save(token)
    }

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toAuthException())
        }

    private fun Exception.toAuthException(): AuthException = when (this) {
        is AuthException -> this
        is DeviceFlowException -> AuthException(
            when (message) {
                "access_denied" -> AuthFailure.ACCESS_DENIED
                "expired_token" -> AuthFailure.EXPIRED
                else -> AuthFailure.UNKNOWN
            }
        )
        is IOException -> AuthException(AuthFailure.NETWORK)
        else -> AuthException(AuthFailure.UNKNOWN)
    }

    private companion object {
        const val DEFAULT_VERIFICATION_URL = "https://github.com/login/device"
    }
}
