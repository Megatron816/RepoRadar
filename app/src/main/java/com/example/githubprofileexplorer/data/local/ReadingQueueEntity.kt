package com.example.githubprofileexplorer.data.local

import androidx.room.Entity

enum class ReadingStatus { TO_READ, EXPLORING, DONE }

@Entity(tableName = "reading_queue", primaryKeys = ["owner", "repoName"])
data class ReadingQueueEntity(
    val owner: String,
    val repoName: String,
    val description: String?,
    val language: String?,
    val htmlUrl: String,
    val defaultBranch: String,
    val stars: Int,
    val forks: Int,
    val status: ReadingStatus = ReadingStatus.TO_READ,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "profile_history")
data class ProfileHistoryEntity(
    @androidx.room.PrimaryKey val login: String,
    val name: String?,
    val bio: String?,
    val avatarUrl: String?,
    val followers: Int,
    val following: Int,
    val publicRepos: Int,
    val lastViewedAt: Long = System.currentTimeMillis(),
    val lastRefreshedAt: Long = System.currentTimeMillis()
)
