package com.example.githubprofileexplorer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingQueueDao {
    @Query("SELECT * FROM reading_queue ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<ReadingQueueEntity>>

    @Query("SELECT * FROM reading_queue WHERE owner = :owner AND repoName = :repoName LIMIT 1")
    suspend fun get(owner: String, repoName: String): ReadingQueueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReadingQueueEntity)

    @Update
    suspend fun update(entity: ReadingQueueEntity)

    @Query("DELETE FROM reading_queue WHERE owner = :owner AND repoName = :repoName")
    suspend fun delete(owner: String, repoName: String)
}

@Dao
interface ProfileHistoryDao {
    @Query("SELECT * FROM profile_history ORDER BY lastViewedAt DESC")
    fun observeAll(): Flow<List<ProfileHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProfileHistoryEntity)

    @Query("DELETE FROM profile_history WHERE login = :login")
    suspend fun delete(login: String)

    @Query("DELETE FROM profile_history")
    suspend fun deleteAll()
}
