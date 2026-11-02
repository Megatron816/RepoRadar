package com.example.githubprofileexplorer.domain

import com.example.githubprofileexplorer.data.model.ContributionDay

data class ContributionSummary(
    val total: Int,
    val longestStreak: Int,
    val currentStreak: Int,
    val activeDays: Int
)

object ContributionCalculator {

    fun summarize(days: List<ContributionDay>): ContributionSummary {
        val sorted = days.sortedBy { it.date }

        var running = 0
        var longest = 0
        sorted.forEach {
            if (it.contributionCount > 0) {
                running++
                longest = maxOf(longest, running)
            } else {
                running = 0
            }
        }

        // A streak is still alive if today has no contributions yet but yesterday had some.
        var index = sorted.lastIndex
        if (index >= 0 && sorted[index].contributionCount == 0) index--
        var current = 0
        while (index >= 0 && sorted[index].contributionCount > 0) {
            current++
            index--
        }

        return ContributionSummary(
            total = sorted.sumOf { it.contributionCount },
            longestStreak = longest,
            currentStreak = current,
            activeDays = sorted.count { it.contributionCount > 0 }
        )
    }
}
