package com.example.githubprofileexplorer.domain

import com.example.githubprofileexplorer.data.model.GitHubLicense
import com.example.githubprofileexplorer.data.model.GitHubRepo
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepoHealthScorerTest {

    private val now = Instant.parse("2026-06-01T00:00:00Z")
    private val scorer = RepoHealthScorer(now)

    @Test
    fun recentLicensedRepoIsHealthy() {
        val repo = GitHubRepo(
            "a", "project", "Kotlin", "a",
            pushedAt = "2026-05-20T00:00:00Z",
            license = GitHubLicense("MIT"),
            topics = listOf("android")
        )
        val score = scorer.score(repo)
        assertEquals(HealthLabel.HEALTHY, score.label)
        assertTrue(HealthReason.UPDATED_RECENTLY in score.reasons)
        assertTrue(HealthReason.HAS_LICENSE in score.reasons)
    }

    @Test
    fun abandonedArchivedRepoIsStale() {
        val repo = GitHubRepo("a", null, null, "a", pushedAt = "2022-01-01T00:00:00Z", archived = true)
        val score = scorer.score(repo)
        assertEquals(HealthLabel.STALE, score.label)
        assertTrue(HealthReason.ARCHIVED in score.reasons)
        assertTrue(HealthReason.NO_PUSH_IN_OVER_A_YEAR in score.reasons)
    }

    @Test
    fun missingPushDateIsReported() {
        val score = scorer.score(GitHubRepo("a", null, null, "a"))
        assertTrue(HealthReason.NO_PUSH_DATE in score.reasons)
        assertTrue(HealthReason.NO_LICENSE in score.reasons)
    }

    @Test
    fun scoreIsBounded() {
        val repo = GitHubRepo(
            "a", "d", null, "a",
            pushedAt = "2020-01-01T00:00:00Z", archived = true, fork = true, openIssuesCount = 99
        )
        assertTrue(scorer.score(repo).value in 0..100)
    }
}
