package com.example.githubprofileexplorer.data.repository

import com.example.githubprofileexplorer.BuildConfig
import com.example.githubprofileexplorer.data.model.GeminiContent
import com.example.githubprofileexplorer.data.model.GeminiGenerateContentRequest
import com.example.githubprofileexplorer.data.model.GeminiPart
import com.example.githubprofileexplorer.data.remote.GeminiApi
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.Response
import com.example.githubprofileexplorer.data.model.GeminiGenerateContentResponse

sealed class GeminiError : Exception() {
    data object MissingApiKey : GeminiError()
    data object InvalidApiKey : GeminiError()
    data object NoInternet : GeminiError()
    data object RateLimited : GeminiError()
    data object InvalidRequest : GeminiError()
    data object PermissionDenied : GeminiError()
    data object ModelNotFound : GeminiError()
    data object Server : GeminiError()
    data object Timeout : GeminiError()
    data object EmptyResponse : GeminiError()
    data object Unknown : GeminiError()
}

class GeminiRepository(private val api: GeminiApi) {

    suspend fun generate(prompt: String): Result<String> {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isBlank()) return Result.failure(GeminiError.MissingApiKey)

        return try {
            val response = requestWithRetry(prompt.trim(), apiKey)
            if (!response.isSuccessful) {
                Result.failure(response.errorToGeminiError())
            } else {
                val body = response.body()
                val candidate = body?.candidates?.firstOrNull()
                val text = candidate?.content?.parts
                    ?.asSequence()
                    ?.map { it.text.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.joinToString("\n")
                    ?.trim()
                if (text.isNullOrEmpty()) {
                    Result.failure(GeminiError.EmptyResponse)
                } else {
                    Result.success(text)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: SocketTimeoutException) {
            Result.failure(GeminiError.Timeout)
        } catch (_: IOException) {
            Result.failure(GeminiError.NoInternet)
        } catch (_: Exception) {
            Result.failure(GeminiError.Unknown)
        }
    }

    private suspend fun requestWithRetry(
        prompt: String,
        apiKey: String
    ): Response<GeminiGenerateContentResponse> {
        val request = GeminiGenerateContentRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt))))
        )
        var response: Response<GeminiGenerateContentResponse>? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            response = api.generateContent(MODEL, apiKey, request)
            val code = response?.code() ?: return@repeat
            if (code !in TRANSIENT_HTTP_CODES || attempt == MAX_ATTEMPTS - 1) return@repeat
            delay(RETRY_DELAY_MS)
        }
        return response ?: error("Gemini request did not produce a response")
    }

    private fun retrofit2.Response<*>.errorToGeminiError(): GeminiError {
        val details = errorBody()?.string().orEmpty()
        return when {
            details.contains("API_KEY_INVALID", ignoreCase = true) ||
                details.contains("INVALID_ARGUMENT", ignoreCase = true) && code() == 400 ||
                code() == 401 -> GeminiError.InvalidApiKey
            code() == 400 -> GeminiError.InvalidRequest
            code() == 403 -> GeminiError.PermissionDenied
            code() == 404 -> GeminiError.ModelNotFound
            code() == 429 -> GeminiError.RateLimited
            code() in 500..599 -> GeminiError.Server
            else -> GeminiError.Unknown
        }
    }

    private companion object {
        const val MODEL = "gemini-3.5-flash-lite"
        const val MAX_ATTEMPTS = 2
        const val RETRY_DELAY_MS = 750L
        val TRANSIENT_HTTP_CODES = setOf(429, 500, 502, 503, 504)
    }
}
