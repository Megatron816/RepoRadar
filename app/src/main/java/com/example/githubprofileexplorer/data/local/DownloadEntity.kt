package com.example.githubprofileexplorer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus { DOWNLOADING, COMPLETED, FAILED }

enum class DownloadError { NETWORK, NOT_FOUND, RATE_LIMITED, STORAGE, INTERRUPTED, UNKNOWN }

/** One row of the download history. */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val owner: String,
    val repoName: String,
    val branch: String,
    val htmlUrl: String,
    val fileName: String,
    /** `content://` URI of the saved ZIP in the public Downloads collection. */
    val fileUri: String? = null,
    val sizeBytes: Long = 0,
    val status: DownloadStatus,
    val error: DownloadError? = null,
    val createdAt: Long,
    val completedAt: Long? = null
)

/** `owner/repo`, kept outside the entity so Room only sees real columns. */
val DownloadEntity.fullName: String get() = "$owner/$repoName"
