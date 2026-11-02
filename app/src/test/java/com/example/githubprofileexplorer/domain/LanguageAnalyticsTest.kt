package com.example.githubprofileexplorer.domain

import com.example.githubprofileexplorer.data.model.GitHubRepo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageAnalyticsTest {

    @Test
    fun aggregatesLanguagesByRepository() {
        val repos = listOf(
            GitHubRepo("a", null, "Kotlin", "a"),
            GitHubRepo("b", null, "Kotlin", "b"),
            GitHubRepo("c", null, "Rust", "c")
        )
        val slices = LanguageAnalytics.aggregate(repos)
        assertEquals("Kotlin", slices.first().language)
        assertEquals(2, slices.first().repositories)
        assertEquals(66.666664f, slices.first().percentage, 0.01f)
    }

    @Test
    fun ignoresRepositoriesWithoutLanguage() {
        val repos = listOf(GitHubRepo("a", null, null, "a"), GitHubRepo("b", null, "", "b"))
        assertTrue(LanguageAnalytics.aggregate(repos).isEmpty())
    }

    @Test
    fun breaksTiesAlphabetically() {
        val repos = listOf(GitHubRepo("a", null, "Rust", "a"), GitHubRepo("b", null, "Go", "b"))
        assertEquals(listOf("Go", "Rust"), LanguageAnalytics.aggregate(repos).map { it.language })
    }
}
