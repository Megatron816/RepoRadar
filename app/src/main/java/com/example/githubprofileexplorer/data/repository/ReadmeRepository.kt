package com.example.githubprofileexplorer.data.repository

import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.model.ReadmeResponse
import com.example.githubprofileexplorer.data.remote.GithubRepoApi
import java.util.Base64

class ReadmeRepository(private val api: GithubRepoApi) {

    /** Returns the README text, or `null` when the repository has no README. */
    suspend fun load(repo: GitHubRepo): Result<String?> =
        try {
            val response = api.getReadme(repo.ownerLogin, repo.name)
            when {
                response.code() == 404 -> Result.success(null)
                !response.isSuccessful -> Result.failure(ProfileException(ProfileRepository.errorForCode(response.code())))
                else -> Result.success(response.body()?.let(::decode))
            }
        } catch (e: Exception) {
            Result.failure(e.toProfileException())
        }

    companion object {
        /** GitHub returns README content Base64-encoded with line breaks. */
        fun decode(response: ReadmeResponse): String? {
            val content = response.content ?: return null
            if (response.encoding != "base64") return content
            return runCatching {
                String(Base64.getMimeDecoder().decode(content), Charsets.UTF_8)
            }.getOrNull()
        }
    }
}
