package com.example.githubprofileexplorer.data.repository

import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.model.GithubUser
import com.example.githubprofileexplorer.data.remote.GithubApi
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** Everything that can go wrong while talking to GitHub, independent of any UI text. */
enum class ProfileError {
    INVALID_USERNAME,
    NOT_FOUND,
    RATE_LIMITED,
    UNAUTHORIZED,
    NETWORK,
    SERVER,
    UNKNOWN
}

class ProfileException(val error: ProfileError) : Exception(error.name)

/**
 * A loaded profile. If the user was found but the repository list could not be fetched,
 * [reposError] is set so the UI can still show the profile and offer a retry.
 */
data class Profile(
    val user: GithubUser,
    val repos: List<GitHubRepo>,
    val reposError: ProfileError? = null
)

class ProfileRepository(private val api: GithubApi) {

    suspend fun loadProfile(rawUsername: String): Result<Profile> {
        val username = rawUsername.trim()
        if (!isValidUsername(username)) {
            return Result.failure(ProfileException(ProfileError.INVALID_USERNAME))
        }

        val user = try {
            val response = api.getUser(username)
            val body = response.body()
            if (response.isSuccessful && body != null) body
            else throw ProfileException(errorForCode(response.code()))
        } catch (e: Exception) {
            return Result.failure(e.toProfileException())
        }

        return try {
            Result.success(Profile(user, fetchAllRepositories(username)))
        } catch (e: Exception) {
            Result.success(Profile(user, emptyList(), e.toProfileException().error))
        }
    }

    suspend fun loadRepositories(username: String): Result<List<GitHubRepo>> =
        try {
            Result.success(fetchAllRepositories(username.trim()))
        } catch (e: Exception) {
            Result.failure(e.toProfileException())
        }

    private suspend fun fetchAllRepositories(username: String): List<GitHubRepo> {
        val all = mutableListOf<GitHubRepo>()
        var page = 1
        while (true) {
            val response = api.getRepositories(username, perPage = PAGE_SIZE, page = page)
            if (!response.isSuccessful) throw ProfileException(errorForCode(response.code()))
            val batch = response.body().orEmpty()
            all += batch
            if (batch.size < PAGE_SIZE) break
            page++
        }
        return all
    }

    companion object {
        const val PAGE_SIZE = 100

        // GitHub logins: alphanumeric or single hyphens, no leading/trailing hyphen, max 39 chars.
        private val USERNAME_REGEX = Regex("^[A-Za-z0-9](?:[A-Za-z0-9]|-(?=[A-Za-z0-9])){0,38}$")

        fun isValidUsername(username: String): Boolean = USERNAME_REGEX.matches(username)

        fun errorForCode(code: Int): ProfileError = when (code) {
            401 -> ProfileError.UNAUTHORIZED
            404 -> ProfileError.NOT_FOUND
            403, 429 -> ProfileError.RATE_LIMITED
            in 500..599 -> ProfileError.SERVER
            else -> ProfileError.UNKNOWN
        }
    }
}

/** Maps any failure to a [ProfileException]; cancellation is always rethrown. */
fun Exception.toProfileException(): ProfileException = when (this) {
    is CancellationException -> throw this
    is ProfileException -> this
    is IOException -> ProfileException(ProfileError.NETWORK)
    else -> ProfileException(ProfileError.UNKNOWN)
}

fun Throwable.asProfileError(): ProfileError =
    (this as? ProfileException)?.error ?: ProfileError.UNKNOWN
