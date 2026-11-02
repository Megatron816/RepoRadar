package com.example.githubprofileexplorer.domain

import com.example.githubprofileexplorer.data.model.GitHubRepo

data class LanguageSlice(val language: String, val repositories: Int, val percentage: Float)

object LanguageAnalytics {

    /** Groups repositories by primary language, most used first. Repositories without a language are skipped. */
    fun aggregate(repositories: List<GitHubRepo>): List<LanguageSlice> {
        val counts = repositories.mapNotNull { it.language?.takeIf(String::isNotBlank) }
            .groupingBy { it }.eachCount()
        val total = counts.values.sum().coerceAtLeast(1)
        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { LanguageSlice(it.key, it.value, it.value * 100f / total) }
    }
}
