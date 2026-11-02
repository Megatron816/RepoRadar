package com.example.githubprofileexplorer.data.repository

import com.example.githubprofileexplorer.data.model.ContributionCalendar
import com.example.githubprofileexplorer.data.model.GraphQlRequest
import com.example.githubprofileexplorer.data.remote.GithubGraphQlApi

/** Loads the contribution calendar through GitHub's GraphQL API (needs a signed-in token). */
class ContributionService(private val api: GithubGraphQlApi) {

    suspend fun load(username: String): Result<ContributionCalendar> =
        try {
            val response = api.graphql(GraphQlRequest(QUERY, mapOf("login" to username)))
            if (!response.isSuccessful) {
                Result.failure(ProfileException(ProfileRepository.errorForCode(response.code())))
            } else {
                val user = response.body()?.data?.user
                val calendar = user?.contributionsCollection?.contributionCalendar
                when {
                    calendar != null -> Result.success(calendar)
                    user == null -> Result.failure(ProfileException(ProfileError.NOT_FOUND))
                    else -> Result.failure(ProfileException(ProfileError.UNKNOWN))
                }
            }
        } catch (e: Exception) {
            Result.failure(e.toProfileException())
        }

    private companion object {
        val QUERY = """
            query(${'$'}login: String!) {
              user(login: ${'$'}login) {
                contributionsCollection {
                  contributionCalendar {
                    totalContributions
                    weeks { contributionDays { date contributionCount contributionLevel } }
                  }
                }
              }
            }
        """.trimIndent()
    }
}
