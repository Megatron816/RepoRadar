package com.example.githubprofileexplorer.domain

import com.example.githubprofileexplorer.data.model.GitHubRepo
import java.time.Instant
import java.time.temporal.ChronoUnit

enum class HealthLabel { HEALTHY, FAIR, STALE }

/** Why a repository received its score; the UI maps each one to a localized sentence. */
enum class HealthReason {
    UPDATED_RECENTLY,
    UPDATED_THIS_YEAR,
    NO_PUSH_IN_OVER_A_YEAR,
    NO_PUSH_DATE,
    HAS_LICENSE,
    NO_LICENSE,
    HAS_CONTEXT,
    MANY_OPEN_ISSUES,
    COMMUNITY_INTEREST,
    ARCHIVED,
    FORKED
}

data class HealthScore(val value: Int, val label: HealthLabel, val reasons: List<HealthReason>)

class RepoHealthScorer(private val now: Instant = Instant.now()) {

    fun score(repo: GitHubRepo): HealthScore {
        var value = 50
        val reasons = mutableListOf<HealthReason>()

        val pushed = repo.pushedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val daysSincePush = pushed?.let { ChronoUnit.DAYS.between(it, now) }
        when {
            daysSincePush == null -> reasons += HealthReason.NO_PUSH_DATE
            daysSincePush <= 180 -> { value += 20; reasons += HealthReason.UPDATED_RECENTLY }
            daysSincePush > 365 -> { value -= 20; reasons += HealthReason.NO_PUSH_IN_OVER_A_YEAR }
            else -> reasons += HealthReason.UPDATED_THIS_YEAR
        }

        if (repo.license != null) {
            value += 10; reasons += HealthReason.HAS_LICENSE
        } else {
            reasons += HealthReason.NO_LICENSE
        }
        if (repo.description?.isNotBlank() == true || repo.topics.isNotEmpty()) {
            value += 10; reasons += HealthReason.HAS_CONTEXT
        }
        if (repo.openIssuesCount > 20) { value -= 5; reasons += HealthReason.MANY_OPEN_ISSUES }
        if (repo.stargazersCount > 10 || repo.forksCount > 3) { value += 5; reasons += HealthReason.COMMUNITY_INTEREST }
        if (repo.archived) { value -= 30; reasons += HealthReason.ARCHIVED }
        if (repo.fork) { value -= 5; reasons += HealthReason.FORKED }

        val bounded = value.coerceIn(0, 100)
        val label = when {
            bounded >= 70 -> HealthLabel.HEALTHY
            bounded >= 40 -> HealthLabel.FAIR
            else -> HealthLabel.STALE
        }
        return HealthScore(bounded, label, reasons)
    }
}
