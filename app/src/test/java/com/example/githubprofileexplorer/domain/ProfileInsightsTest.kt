package com.example.githubprofileexplorer.domain

import com.example.githubprofileexplorer.data.model.GitHubLicense
import com.example.githubprofileexplorer.data.model.GitHubRepo
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileInsightsTest {

    private val now = Instant.parse("2026-06-01T00:00:00Z")

    private val repos = listOf(
        GitHubRepo(
            "app", "An app", "Kotlin", "u/app", stargazersCount = 10,
            pushedAt = "2026-05-20T00:00:00Z", license = GitHubLicense("MIT"),
            topics = listOf("android", "kotlin")
        ),
        GitHubRepo(
            "lib", null, "Kotlin", "u/lib", stargazersCount = 3,
            pushedAt = "2026-01-01T00:00:00Z", topics = listOf("android")
        ),
        GitHubRepo("fork", null, "Rust", "u/fork", stargazersCount = 99, fork = true, pushedAt = "2025-01-01T00:00:00Z")
    )

    @Test
    fun summarisesOwnedWork() {
        val insights = ProfileInsightsGenerator.generate(repos, now)
        assertTrue(insights.hasData)
        assertEquals("Kotlin", insights.primaryLanguage)
        assertTrue(insights.otherLanguages.isEmpty())
        assertEquals(2, insights.ownedRepos)
        assertEquals(1, insights.forkedRepos)
        // Stars on forks must not count towards the user's own total.
        assertEquals(13, insights.totalStars)
        assertEquals("app", insights.topRepoName)
        assertEquals(listOf("android", "kotlin"), insights.topTopics)
        assertEquals(50, insights.licensedPercent)
        assertEquals(85, insights.averageHealth)
    }

    @Test
    fun classifiesActivity() {
        assertEquals(ActivityLevel.VERY_ACTIVE, ProfileInsightsGenerator.generate(repos, now).activity)
        val old = listOf(GitHubRepo("x", null, null, "x", pushedAt = "2024-01-01T00:00:00Z"))
        assertEquals(ActivityLevel.DORMANT, ProfileInsightsGenerator.generate(old, now).activity)
        val undated = listOf(GitHubRepo("x", null, null, "x"))
        assertEquals(ActivityLevel.UNKNOWN, ProfileInsightsGenerator.generate(undated, now).activity)
    }

    @Test
    fun fallsBackToForksForLanguages() {
        val onlyForks = listOf(GitHubRepo("f", null, "Go", "f", fork = true))
        val insights = ProfileInsightsGenerator.generate(onlyForks, now)
        assertEquals("Go", insights.primaryLanguage)
        assertNull(insights.licensedPercent)
        assertNull(insights.averageHealth)
    }

    @Test
    fun emptyAccountHasNoData() {
        assertFalse(ProfileInsightsGenerator.generate(emptyList(), now).hasData)
    }
}
