package com.example.githubprofileexplorer.data.model

data class GraphQlRequest(val query: String, val variables: Map<String, String> = emptyMap())

data class GraphQlResponse(val data: ContributionData?, val errors: List<GraphQlError>?)

data class GraphQlError(val message: String?)

data class ContributionData(val user: ContributionUser?)

data class ContributionUser(val contributionsCollection: ContributionsCollection?)

data class ContributionsCollection(val contributionCalendar: ContributionCalendar?)

data class ContributionCalendar(
    val totalContributions: Int,
    val weeks: List<ContributionWeek>
)

data class ContributionWeek(val contributionDays: List<ContributionDay>)

/** [contributionLevel] is one of NONE, FIRST_QUARTILE, SECOND_QUARTILE, THIRD_QUARTILE, FOURTH_QUARTILE. */
data class ContributionDay(val date: String, val contributionCount: Int, val contributionLevel: String)
