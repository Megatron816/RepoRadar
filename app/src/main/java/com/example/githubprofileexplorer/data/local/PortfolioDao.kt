package com.example.githubprofileexplorer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolio_profiles ORDER BY savedAt DESC")
    fun observeProfiles(): Flow<List<PortfolioProfileEntity>>

    @Query("SELECT * FROM portfolio_projects WHERE profileKey = :profileKey ORDER BY selectedAt DESC")
    fun observeProjects(profileKey: String): Flow<List<PortfolioProjectEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM portfolio_profiles WHERE profileKey = :profileKey)")
    suspend fun isProfileSaved(profileKey: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM portfolio_projects WHERE profileKey = :profileKey AND repositoryKey = :repositoryKey)")
    suspend fun isProjectSelected(profileKey: String, repositoryKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: PortfolioProfileEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfileIfMissing(profile: PortfolioProfileEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProject(project: PortfolioProjectEntity)

    @Transaction
    suspend fun selectProject(profile: PortfolioProfileEntity, project: PortfolioProjectEntity) {
        insertProfileIfMissing(profile)
        insertProject(project)
    }

    @Query("DELETE FROM portfolio_projects WHERE profileKey = :profileKey AND repositoryKey = :repositoryKey")
    suspend fun deselectProject(profileKey: String, repositoryKey: String)

    @Query("DELETE FROM portfolio_profiles WHERE profileKey = :profileKey")
    suspend fun deleteProfile(profileKey: String)

    @Transaction
    suspend fun replaceProfile(profile: PortfolioProfileEntity) {
        saveProfile(profile)
    }
}
