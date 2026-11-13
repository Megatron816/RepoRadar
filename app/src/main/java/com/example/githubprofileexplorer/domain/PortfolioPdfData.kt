package com.example.githubprofileexplorer.domain

import com.example.githubprofileexplorer.data.local.PortfolioProjectEntity
import com.example.githubprofileexplorer.data.local.portfolioRepositoryKey
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.repository.Profile
import java.text.DateFormat
import java.util.Date
import java.util.Locale

data class PortfolioPdfData(
    val profile: PortfolioPdfProfile,
    val repositories: List<PortfolioPdfRepository>,
    val aiPortfolioWriting: String?,
    val generatedAt: Long,
    val fetchedAt: Long?,
    val isStale: Boolean
)

data class PortfolioPdfProfile(
    val username: String,
    val displayName: String?,
    val githubId: Long?,
    val avatarUrl: String?,
    val profileUrl: String,
    val bio: String?,
    val company: String?,
    val location: String?,
    val blog: String?,
    val createdAt: String?
)

data class PortfolioPdfRepository(
    val name: String,
    val fullName: String,
    val githubId: Long?,
    val url: String,
    val description: String?,
    val language: String?,
    val topics: List<String>,
    val stars: Int?,
    val forks: Int?,
    val openIssues: Int?,
    val licenseName: String?,
    val licenseSpdxId: String?,
    val defaultBranch: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val pushedAt: String?,
    val archived: Boolean?,
    val fork: Boolean?,
    val readmeUrl: String?
)

object PortfolioPdfDataMapper {
    const val STALE_AFTER_MILLIS = 15 * 60 * 1000L

    fun map(
        profile: Profile,
        selectedProjects: List<PortfolioProjectEntity>,
        fetchedAt: Long?,
        generatedAt: Long = System.currentTimeMillis(),
        aiPortfolioWriting: String? = null
    ): PortfolioPdfData {
        val reposByKey = profile.repos.associateBy { it.portfolioRepositoryKey() }
        val repositories = selectedProjects.map { selected ->
            val repo = reposByKey[selected.repositoryKey]
            repo?.toPdfRepository() ?: selected.toPdfRepository()
        }
        return PortfolioPdfData(
            profile = profile.user.toPdfProfile(),
            repositories = repositories,
            aiPortfolioWriting = aiPortfolioWriting,
            generatedAt = generatedAt,
            fetchedAt = fetchedAt,
            isStale = fetchedAt == null || generatedAt - fetchedAt > STALE_AFTER_MILLIS
        )
    }

    fun formatTimestamp(value: String?): String =
        value?.takeIf(String::isNotBlank) ?: "Not provided"

    fun formatLocalTimestamp(value: Long?): String =
        value?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()).format(Date(it)) }
            ?: "Not available"

    fun sanitizeFilename(username: String): String {
        val safe = username.trim().replace(Regex("[^A-Za-z0-9._-]"), "-")
            .replace(Regex("[.-]{2,}"), "-")
            .trim('.', '-', '_').ifBlank { "profile" }
        return "github-portfolio-$safe-${java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.pdf"
    }

    private fun com.example.githubprofileexplorer.data.model.GithubUser.toPdfProfile() =
        PortfolioPdfProfile(
            username = login,
            displayName = name,
            githubId = id.takeIf { it > 0 },
            avatarUrl = avatarUrl,
            profileUrl = htmlUrl ?: "https://github.com/$login",
            bio = bio,
            company = company,
            location = location,
            blog = blog,
            createdAt = createdAt
        )

    private fun GitHubRepo.toPdfRepository() = PortfolioPdfRepository(
        name = name,
        fullName = fullName ?: "$ownerLogin/$name",
        githubId = id.takeIf { it > 0 },
        url = htmlUrl,
        description = description,
        language = language,
        topics = topics,
        stars = stargazersCount,
        forks = forksCount,
        openIssues = openIssuesCount,
        licenseName = license?.name,
        licenseSpdxId = license?.spdxId,
        defaultBranch = defaultBranch,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pushedAt = pushedAt,
        archived = archived,
        fork = fork,
        readmeUrl = "$htmlUrl/blob/${defaultBranch.ifBlank { "main" }}/README.md"
    )

    private fun PortfolioProjectEntity.toPdfRepository() = PortfolioPdfRepository(
        name = name,
        fullName = htmlUrl.substringAfter("github.com/").trimEnd('/'),
        githubId = githubId,
        url = htmlUrl,
        description = description,
        language = language,
        topics = topics,
        stars = null,
        forks = null,
        openIssues = null,
        licenseName = null,
        licenseSpdxId = null,
        defaultBranch = null,
        createdAt = null,
        updatedAt = null,
        pushedAt = null,
        archived = null,
        fork = null,
        readmeUrl = null
    )
}
