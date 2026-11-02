package com.example.githubprofileexplorer.domain

import com.example.githubprofileexplorer.data.model.GitHubRepo
import java.time.Instant
import java.time.temporal.ChronoUnit

enum class ActivityLevel { VERY_ACTIVE, ACTIVE, OCCASIONAL, DORMANT, UNKNOWN }

/**
 * Facts derived from a user's public repositories. The UI turns these into localized sentences, so
 * no text is generated here. Everything comes from the GitHub REST API: no AI service is involved.
 */
data class ProfileInsights(
    val primaryLanguage: String?,
    val otherLanguages: List<String>,
    val ownedRepos: Int,
    val forkedRepos: Int,
    val archivedRepos: Int,
    val totalStars: Int,
    val topRepoName: String?,
    val topRepoStars: Int,
    val activity: ActivityLevel,
    val daysSinceLastPush: Int?,
    val topTopics: List<String>,
    val licensedPercent: Int?,
    val averageHealth: Int?
) {
    val hasData: Boolean get() = ownedRepos + forkedRepos > 0
}

object ProfileInsightsGenerator {

    fun generate(repos: List<GitHubRepo>, now: Instant = Instant.now()): ProfileInsights {
        val owned = repos.filterNot { it.fork }
        val forks = repos.filter { it.fork }

        // Prefer languages from original work; fall back to forks for accounts that only fork.
        val languages = LanguageAnalytics.aggregate(owned.ifEmpty { repos }).map { it.language }

        val topRepo = owned.filter { it.stargazersCount > 0 }.maxByOrNull { it.stargazersCount }

        val lastPush = repos
            .mapNotNull { repo -> repo.pushedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } }
            .maxOrNull()
        val daysSince = lastPush?.let { ChronoUnit.DAYS.between(it, now).toInt().coerceAtLeast(0) }
        val activity = when {
            daysSince == null -> ActivityLevel.UNKNOWN
            daysSince <= 30 -> ActivityLevel.VERY_ACTIVE
            daysSince <= 180 -> ActivityLevel.ACTIVE
            daysSince <= 365 -> ActivityLevel.OCCASIONAL
            else -> ActivityLevel.DORMANT
        }

        val topics = repos.flatMap { it.topics }
            .groupingBy { it }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(MAX_TOPICS)
            .map { it.key }

        val scorer = RepoHealthScorer(now)
        return ProfileInsights(
            primaryLanguage = languages.firstOrNull(),
            otherLanguages = languages.drop(1).take(2),
            ownedRepos = owned.size,
            forkedRepos = forks.size,
            archivedRepos = repos.count { it.archived },
            totalStars = owned.sumOf { it.stargazersCount },
            topRepoName = topRepo?.name,
            topRepoStars = topRepo?.stargazersCount ?: 0,
            activity = activity,
            daysSinceLastPush = daysSince,
            topTopics = topics,
            licensedPercent = owned.takeIf { it.isNotEmpty() }
                ?.let { list -> list.count { it.license != null } * 100 / list.size },
            averageHealth = owned.takeIf { it.isNotEmpty() }
                ?.let { list -> list.map { scorer.score(it).value }.average().toInt() }
        )
    }

    private const val MAX_TOPICS = 5
}
