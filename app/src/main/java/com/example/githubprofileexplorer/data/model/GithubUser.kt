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
    val avatarUrl: String?
)
