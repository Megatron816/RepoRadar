package com.example.githubprofileexplorer.data.model

import com.google.gson.annotations.SerializedName

data class GithubUser(
    val login: String,
    val name: String?,
    val bio: String?,
    val followers: Int,
    val following: Int,
    @SerializedName("public_repos")
    val publicRepos: Int,
    @SerializedName("avatar_url")
    val avatarUrl: String?,
    @SerializedName("html_url")
    val htmlUrl: String? = null,
    val company: String? = null,
    val location: String? = null,
    val blog: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    /** Stable GitHub user identifier, when returned by the API. */
    val id: Long = 0L
)
