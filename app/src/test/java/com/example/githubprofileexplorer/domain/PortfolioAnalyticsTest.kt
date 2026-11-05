package com.example.githubprofileexplorer.domain

import com.example.githubprofileexplorer.data.model.GitHubRepo
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortfolioAnalyticsTest {
    private fun repo(
        name: String,
        language: String? = "Kotlin",
        stars: Int = 0,
        pushedAt: String? = "2026-10-01T00:00:00Z",
        topics: List<String> = emptyList(),
        archived: Boolean = false,
        fork: Boolean = false
    ) = GitHubRepo(
        name = name,
        description = null,
        language = language,
        htmlUrl = "https://github.com/octocat/$name",
        stargazersCount = stars,
        pushedAt = pushedAt,
        topics = topics,
        archived = archived,
        fork = fork
    )

    @Test
    fun filtersCompleteSetAndSortsByStars() {
        val repos = listOf(repo("low"), repo("high", stars = 10), repo("fork", stars = 20, fork = true))
        val result = PortfolioAnalytics.filterAndSort(
            repos,
            RepositoryFilters(minimumStars = 5, includeForks = false),
            PortfolioRepoSort.STARS,
            Instant.parse("2026-11-01T00:00:00Z")
        )
        assertEquals(listOf("high"), result.map { it.name })
    }

    @Test
    fun missingPushDateDoesNotMatchRecentFilter() {
        val result = PortfolioAnalytics.filterAndSort(
            listOf(repo("missing", pushedAt = null)),
            RepositoryFilters(updatedWithinDays = 30),
            now = Instant.parse("2026-11-01T00:00:00Z")
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun aggregatesTopicsCaseSensitivelyAsReportedAndSortsByCount() {
        val result = PortfolioAnalytics.topics(listOf(repo("a", topics = listOf("android", "kotlin")), repo("b", topics = listOf("android"))))
        assertEquals(TopicSlice("android", 2), result.first())
    }

    @Test
    fun shareTextDisclosesPublicSource() {
        val text = PortfolioShareText.create(
            PortfolioShareInput("octocat", listOf("demo"), listOf("Kotlin"))
        )
        assertTrue(text.contains("Based on public GitHub data"))
        assertTrue(text.contains("https://github.com/octocat"))
        assertTrue(text.contains("demo"))
    }
}
