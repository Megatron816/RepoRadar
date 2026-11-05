package com.example.githubprofileexplorer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.model.GithubUser

@Entity(tableName = "portfolio_profiles")
data class PortfolioProfileEntity(
    @androidx.room.PrimaryKey val profileKey: String,
    val githubId: Long?,
    val login: String,
    val name: String?,
    val bio: String?,
    val avatarUrl: String?,
    val followers: Int,
    val following: Int,
    val publicRepos: Int,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "portfolio_projects",
    primaryKeys = ["profileKey", "repositoryKey"],
    foreignKeys = [
        ForeignKey(
            entity = PortfolioProfileEntity::class,
            parentColumns = ["profileKey"],
            childColumns = ["profileKey"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("profileKey")]
)
data class PortfolioProjectEntity(
    val profileKey: String,
    val repositoryKey: String,
    val githubId: Long?,
    val name: String,
    val description: String?,
    val language: String?,
    val htmlUrl: String,
    val topics: List<String>,
    val selectedAt: Long = System.currentTimeMillis()
)

fun GithubUser.portfolioKey(): String =
    if (id > 0) "github-user:$id" else "login:${login.lowercase()}"

fun GitHubRepo.portfolioRepositoryKey(): String =
    if (id > 0) "github-repo:$id" else "url:${htmlUrl.trimEnd('/').lowercase()}"
