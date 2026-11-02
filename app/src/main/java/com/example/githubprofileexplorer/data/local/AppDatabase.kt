package com.example.githubprofileexplorer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DownloadEntity::class, ReadingQueueEntity::class, ProfileHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao
    abstract fun readingQueueDao(): ReadingQueueDao
    abstract fun profileHistoryDao(): ProfileHistoryDao

    companion object {
        private const val DATABASE_NAME = "profile_explorer.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2)
                .build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reading_queue (
                        owner TEXT NOT NULL,
                        repoName TEXT NOT NULL,
                        description TEXT,
                        language TEXT,
                        htmlUrl TEXT NOT NULL,
                        defaultBranch TEXT NOT NULL,
                        stars INTEGER NOT NULL,
                        forks INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        addedAt INTEGER NOT NULL,
                        PRIMARY KEY(owner, repoName)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS profile_history (
                        login TEXT NOT NULL PRIMARY KEY,
                        name TEXT,
                        bio TEXT,
                        avatarUrl TEXT,
                        followers INTEGER NOT NULL,
                        following INTEGER NOT NULL,
                        publicRepos INTEGER NOT NULL,
                        lastViewedAt INTEGER NOT NULL,
                        lastRefreshedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
