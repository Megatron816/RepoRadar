package com.example.githubprofileexplorer.domain

import com.example.githubprofileexplorer.data.model.GitHubRepo
import java.time.Instant
import java.time.temporal.ChronoUnit

data class TopicSlice(val topic: String, val repositories: Int)

data class RepositoryFilters(
    val language: String? = null,
    val topic: String? = null,
    val minimumStars: Int = 0,
    val updatedWithinDays: Int? = null,
    val includeArchived: Boolean = true,
    val includeForks: Boolean = true
) {
    val isActive: Boolean
        get() = language != null || topic != null || minimumStars > 0 ||
            updatedWithinDays != null || !includeArchived || !includeForks
}

enum class PortfolioRepoSort { UPDATED, STARS, NAME }

object PortfolioAnalytics {
    /**
     * Filters the complete in-memory repository set. Dates that GitHub did not return do not
     * satisfy a recency filter; they are never treated as recently active.
     */
    fun filterAndSort(
        repositories: List<GitHubRepo>,
        filters: RepositoryFilters = RepositoryFilters(),
        sort: PortfolioRepoSort = PortfolioRepoSort.UPDATED,
        now: Instant = Instant.now()
    ): List<GitHubRepo> {
        val cutoff = filters.updatedWithinDays?.let { now.minus(it.toLong(), ChronoUnit.DAYS) }
        val filtered = repositories.filter { repo ->
            (filters.language == null || repo.language.equals(filters.language, ignoreCase = true)) &&
                (filters.topic == null || repo.topics.any { it.equals(filters.topic, ignoreCase = true) }) &&
                repo.stargazersCount >= filters.minimumStars &&
                (filters.includeArchived || !repo.archived) &&
                (filters.includeForks || !repo.fork) &&
                (cutoff == null || repo.pushedAt?.let { parseInstantOrNull(it) }?.let { it >= cutoff } == true)
        }

        return when (sort) {
            PortfolioRepoSort.UPDATED -> filtered.sortedWith(
                compareByDescending<GitHubRepo> { it.pushedAt?.let(::parseInstantOrNull) }
                    .thenBy { it.name.lowercase() }
            )
            PortfolioRepoSort.STARS -> filtered.sortedWith(
                compareByDescending<GitHubRepo> { it.stargazersCount }.thenBy { it.name.lowercase() }
            )
            PortfolioRepoSort.NAME -> filtered.sortedBy { it.name.lowercase() }
        }

    }

    fun topics(repositories: List<GitHubRepo>): List<TopicSlice> =
        repositories.asSequence()
            .flatMap { it.topics.asSequence() }
            .map { it.trim() }
            .filter(String::isNotBlank)
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
            .map { TopicSlice(it.key, it.value) }

    fun parseInstant(value: String): Instant =
        Instant.parse(value)

    private fun parseInstantOrNull(value: String): Instant? =
        runCatching { parseInstant(value) }.getOrNull()
}

@Deprecated("PDF export replaces text portfolio sharing.")
data class PortfolioShareInput(
    val username: String,
    val selectedProjects: List<String>,
    val languages: List<String>
)

@Deprecated("PDF export replaces text portfolio sharing.")
object PortfolioShareText {
    fun create(input: PortfolioShareInput): String = buildString {
        append("GitHub portfolio for @${input.username}\n")
        append("Based on public GitHub data: https://github.com/${input.username}\n")
        input.selectedProjects.distinct().forEach { append("- $it\n") }
    }
}
