package com.example.githubprofileexplorer.data.local

import androidx.room.Entity

@Entity(tableName = "architecture_snapshots")
data class ArchitectureSnapshotEntity(
    @androidx.room.PrimaryKey val snapshotKey: String,
    val repositoryKey: String,
    val repositoryId: Long,
    val owner: String,
    val name: String,
    val branch: String,
    val commitSha: String,
    val scannedAt: Long,
    val partial: Boolean,
    val warningCount: Int,
    val signalsJson: String,
    val completeness: String = "complete",
    val detectedManifests: String = "",
    val parsedManifests: String = "",
    val unsupportedManifests: String = ""
)

@Entity(primaryKeys = ["snapshotKey", "nodeId"], tableName = "architecture_nodes")
data class ArchitectureNodeEntity(
    val snapshotKey: String,
    val nodeId: String,
    val label: String,
    val type: String,
    val version: String?,
    val sourcePath: String?,
    val resolvedVersion: String? = null,
    val ecosystem: String = "GRADLE",
    val projectRoot: String = ""
)

@Entity(primaryKeys = ["snapshotKey", "fromId", "toId", "scope"], tableName = "architecture_edges")
data class ArchitectureEdgeEntity(
    val snapshotKey: String,
    val fromId: String,
    val toId: String,
    val scope: String,
    val sourcePath: String?,
    val sourceKind: String = "manifest"
)

@Entity(primaryKeys = ["snapshotKey", "message", "sourcePath", "line"], tableName = "architecture_warnings")
data class ArchitectureWarningEntity(
    val snapshotKey: String,
    val message: String,
    val sourcePath: String = "",
    val line: Int = 0
)
