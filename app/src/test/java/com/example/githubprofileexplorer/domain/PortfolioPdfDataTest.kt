package com.example.githubprofileexplorer.domain

import com.example.githubprofileexplorer.data.local.PortfolioProjectEntity
import com.example.githubprofileexplorer.data.local.portfolioKey
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.model.GithubUser
import com.example.githubprofileexplorer.data.repository.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortfolioPdfDataTest {
    @Test
    fun mapIncludesStableIdsAndOnlySelectedRepositories() {
        val user = GithubUser("octocat", "Octo", null, 1, 2, 1, null, id = 99L)
        val selected = PortfolioProjectEntity(
            profileKey = user.portfolioKey(),
            repositoryKey = "github-repo:7",
            githubId = 7L,
            name = "selected",
            description = null,
            language = "Kotlin",
            htmlUrl = "https://github.com/octocat/selected",
            topics = emptyList()
        )
        val profile = Profile(
            user,
            listOf(
                GitHubRepo("selected", null, "Kotlin", "https://github.com/octocat/selected", id = 7L),
                GitHubRepo("not-selected", null, null, "https://github.com/octocat/not-selected", id = 8L)
            )
        )

        val data = PortfolioPdfDataMapper.map(profile, listOf(selected), fetchedAt = 1_000L, generatedAt = 2_000L)

        assertEquals(99L, data.profile.githubId)
        assertEquals(1, data.repositories.size)
        assertEquals(7L, data.repositories.single().githubId)
        assertEquals(0, data.repositories.single().stars)
        assertFalse(data.isStale)
    }

    @Test
    fun emptySelectionIsRepresentedAndOldFetchIsStale() {
        val user = GithubUser("octocat", null, null, 0, 0, 0, null)
        val data = PortfolioPdfDataMapper.map(Profile(user, emptyList()), emptyList(), 0L, PortfolioPdfDataMapper.STALE_AFTER_MILLIS + 1)

        assertTrue(data.repositories.isEmpty())
        assertTrue(data.isStale)
        assertEquals("github-portfolio-octocat-${java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())}.pdf",
            PortfolioPdfDataMapper.sanitizeFilename("octocat"))
    }

    @Test
    fun unsafeFilenameCharactersAreReplaced() {
        assertEquals(
            "github-portfolio-a-b-2026-11-05.pdf",
            PortfolioPdfDataMapper.sanitizeFilename("a/../b").replace(Regex("\\d{4}-\\d{2}-\\d{2}"), "2026-11-05")
        )
    }
}
