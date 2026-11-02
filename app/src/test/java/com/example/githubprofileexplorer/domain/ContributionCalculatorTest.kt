package com.example.githubprofileexplorer.domain

import com.example.githubprofileexplorer.data.model.ContributionDay
import org.junit.Assert.assertEquals
import org.junit.Test

class ContributionCalculatorTest {

    private fun days(vararg counts: Int) = counts.mapIndexed { index, count ->
        ContributionDay("2026-01-%02d".format(index + 1), count, "LEVEL")
    }

    @Test
    fun calculatesTotalsAndLongestStreak() {
        val summary = ContributionCalculator.summarize(days(1, 2, 0))
        assertEquals(3, summary.total)
        assertEquals(2, summary.longestStreak)
        assertEquals(2, summary.activeDays)
    }

    @Test
    fun currentStreakCountsBackFromLastDay() {
        assertEquals(3, ContributionCalculator.summarize(days(0, 1, 1, 1)).currentStreak)
    }

    @Test
    fun currentStreakSurvivesAnEmptyToday() {
        assertEquals(2, ContributionCalculator.summarize(days(0, 1, 1, 0)).currentStreak)
    }

    @Test
    fun currentStreakBreaksAfterTwoEmptyDays() {
        assertEquals(0, ContributionCalculator.summarize(days(1, 1, 0, 0)).currentStreak)
    }

    @Test
    fun handlesNoData() {
        val summary = ContributionCalculator.summarize(emptyList())
        assertEquals(0, summary.total)
        assertEquals(0, summary.longestStreak)
        assertEquals(0, summary.currentStreak)
    }
}
