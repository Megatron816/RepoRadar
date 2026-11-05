package com.example.githubprofileexplorer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class ArchitectureSnapshotBundle(
    val snapshot: ArchitectureSnapshotEntity,
    val nodes: List<ArchitectureNodeEntity>,
    val edges: List<ArchitectureEdgeEntity>,
    val warnings: List<ArchitectureWarningEntity>
)

@Dao
abstract class ArchitectureDao {
    @Query("SELECT * FROM architecture_snapshots ORDER BY scannedAt DESC")
    abstract fun observeSnapshots(): Flow<List<ArchitectureSnapshotEntity>>

    @Query("SELECT * FROM architecture_snapshots WHERE snapshotKey = :key")
    abstract suspend fun snapshot(key: String): ArchitectureSnapshotEntity?

    @Query("SELECT * FROM architecture_nodes WHERE snapshotKey = :key")
    abstract suspend fun nodes(key: String): List<ArchitectureNodeEntity>

    @Query("SELECT * FROM architecture_edges WHERE snapshotKey = :key")
    abstract suspend fun edges(key: String): List<ArchitectureEdgeEntity>

    @Query("SELECT * FROM architecture_warnings WHERE snapshotKey = :key")
    abstract suspend fun warnings(key: String): List<ArchitectureWarningEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSnapshot(value: ArchitectureSnapshotEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertNodes(value: List<ArchitectureNodeEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertEdges(value: List<ArchitectureEdgeEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertWarnings(value: List<ArchitectureWarningEntity>)

    @Query("DELETE FROM architecture_snapshots WHERE snapshotKey = :key")
    abstract suspend fun deleteSnapshot(key: String)
    @Query("DELETE FROM architecture_nodes WHERE snapshotKey = :key")
    protected abstract suspend fun deleteNodes(key: String)
    @Query("DELETE FROM architecture_edges WHERE snapshotKey = :key")
    protected abstract suspend fun deleteEdges(key: String)
    @Query("DELETE FROM architecture_warnings WHERE snapshotKey = :key")
    protected abstract suspend fun deleteWarnings(key: String)

    @Transaction
    open suspend fun save(bundle: ArchitectureSnapshotBundle) {
        deleteNodes(bundle.snapshot.snapshotKey)
        deleteEdges(bundle.snapshot.snapshotKey)
        deleteWarnings(bundle.snapshot.snapshotKey)
        insertSnapshot(bundle.snapshot)
        insertNodes(bundle.nodes)
        insertEdges(bundle.edges)
        insertWarnings(bundle.warnings)
    }

    @Transaction
    open suspend fun load(key: String): ArchitectureSnapshotBundle? =
        snapshot(key)?.let { ArchitectureSnapshotBundle(it, nodes(key), edges(key), warnings(key)) }
}
