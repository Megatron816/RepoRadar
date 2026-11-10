package com.example.githubprofileexplorer.data.model

import com.google.gson.annotations.SerializedName

data class RepositoryMetadata(
    val id: Long = 0,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("default_branch") val defaultBranch: String = "main",
    @SerializedName("pushed_at") val pushedAt: String? = null,
    @SerializedName("open_issues_count") val openIssuesCount: Int = 0,
    @SerializedName("stargazers_count") val stars: Int = 0,
    @SerializedName("watchers_count") val watchers: Int = 0,
    val size: Long = 0,
    val description: String? = null,
    val language: String? = null,
    val archived: Boolean = false,
    val fork: Boolean = false,
    @SerializedName("html_url") val htmlUrl: String? = null,
    val license: GitHubLicense? = null
)

data class GitTreeResponse(
    val sha: String = "",
    val tree: List<GitTreeEntry> = emptyList(),
    val truncated: Boolean = false
)

data class GitTreeEntry(
    val path: String,
    val mode: String? = null,
    val type: String? = null,
    val sha: String? = null,
    val size: Long? = null,
    val url: String? = null
)

data class GitContentResponse(
    val type: String? = null,
    val encoding: String? = null,
    val content: String? = null,
    val size: Long? = null,
    val path: String? = null
)

data class GitHubCommit(
    val sha: String = "",
    val commit: GitHubCommitDetails? = null,
    val files: List<GitHubCommitFile>? = null
)

data class GitHubCommitDetails(val message: String? = null, val author: GitHubCommitAuthor? = null)
data class GitHubCommitAuthor(val date: String? = null)
data class GitHubCommitFile(val filename: String? = null)
