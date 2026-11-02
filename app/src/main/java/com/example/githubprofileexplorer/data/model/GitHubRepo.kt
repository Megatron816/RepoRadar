package com.example.githubprofileexplorer.data.model

import com.google.gson.annotations.SerializedName

data class GitHubRepo(
    val name: String,
    val description: String?,
    val language: String?,
    @SerializedName("html_url")
    val htmlUrl: String,
    @SerializedName("stargazers_count")
    val stargazersCount: Int = 0,
    @SerializedName("pushed_at")
    val pushedAt: String? = null,
    @SerializedName("open_issues_count")
    val openIssuesCount: Int = 0,
    val license: GitHubLicense? = null,
    val archived: Boolean = false,
    val fork: Boolean = false,
    val topics: List<String> = emptyList(),
    @SerializedName("forks_count")
    val forksCount: Int = 0,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("default_branch")
    val defaultBranch: String = "main",
    @SerializedName("full_name")
    val fullName: String? = null,
    val owner: GitHubOwner? = null,
    /** Repository size in kilobytes, as reported by GitHub. */
    val size: Long = 0
) {
    /** Login of the account that owns this repository. */
    val ownerLogin: String
        get() = owner?.login
            ?: fullName?.substringBefore('/')
            ?: htmlUrl.trimEnd('/').substringBeforeLast('/').substringAfterLast('/')
}

data class GitHubOwner(val login: String)

data class GitHubLicense(
    val name: String? = null,
    @SerializedName("spdx_id") val spdxId: String? = null
)

data class ReadmeResponse(val content: String?, val encoding: String?)
