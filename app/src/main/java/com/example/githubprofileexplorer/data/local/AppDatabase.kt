package com.example.githubprofileexplorer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DownloadEntity::class,
        ReadingQueueEntity::class,
        ProfileHistoryEntity::class,
        PortfolioProfileEntity::class,
        PortfolioProjectEntity::class,
        ArchitectureSnapshotEntity::class,
        ArchitectureNodeEntity::class,
        ArchitectureEdgeEntity::class,
        ArchitectureWarningEntity::class,
    ],
    version = 8,
    exportSchema = false
)
@androidx.room.TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao
    abstract fun readingQueueDao(): ReadingQueueDao
    abstract fun profileHistoryDao(): ProfileHistoryDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun architectureDao(): ArchitectureDao

    companion object {
        private const val DATABASE_NAME = "profile_explorer.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .addMigrations(MIGRATION_7_8)
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS portfolio_profiles (
                        profileKey TEXT NOT NULL PRIMARY KEY,
                        githubId INTEGER,
                        login TEXT NOT NULL,
                        name TEXT,
                        bio TEXT,
                        avatarUrl TEXT,
                        followers INTEGER NOT NULL,
                        following INTEGER NOT NULL,
                        publicRepos INTEGER NOT NULL,
                        savedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS portfolio_projects (
                        profileKey TEXT NOT NULL,
                        repositoryKey TEXT NOT NULL,
                        githubId INTEGER,
                        name TEXT NOT NULL,
                        description TEXT,
                        language TEXT,
                        htmlUrl TEXT NOT NULL,
                        topics TEXT NOT NULL,
                        selectedAt INTEGER NOT NULL,
                        PRIMARY KEY(profileKey, repositoryKey),
                        FOREIGN KEY(profileKey) REFERENCES portfolio_profiles(profileKey) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_portfolio_projects_profileKey ON portfolio_projects(profileKey)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS architecture_snapshots (snapshotKey TEXT NOT NULL PRIMARY KEY, repositoryKey TEXT NOT NULL, repositoryId INTEGER NOT NULL, owner TEXT NOT NULL, name TEXT NOT NULL, branch TEXT NOT NULL, commitSha TEXT NOT NULL, scannedAt INTEGER NOT NULL, partial INTEGER NOT NULL, warningCount INTEGER NOT NULL, signalsJson TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS architecture_nodes (snapshotKey TEXT NOT NULL, nodeId TEXT NOT NULL, label TEXT NOT NULL, type TEXT NOT NULL, version TEXT, sourcePath TEXT, PRIMARY KEY(snapshotKey, nodeId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS architecture_edges (snapshotKey TEXT NOT NULL, fromId TEXT NOT NULL, toId TEXT NOT NULL, scope TEXT NOT NULL, sourcePath TEXT, PRIMARY KEY(snapshotKey, fromId, toId, scope))")
                db.execSQL("CREATE TABLE IF NOT EXISTS architecture_warnings (snapshotKey TEXT NOT NULL, message TEXT NOT NULL, sourcePath TEXT NOT NULL, line INTEGER NOT NULL, PRIMARY KEY(snapshotKey, message, sourcePath, line))")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE architecture_snapshots ADD COLUMN completeness TEXT NOT NULL DEFAULT 'complete'")
                db.execSQL("ALTER TABLE architecture_snapshots ADD COLUMN detectedManifests TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE architecture_snapshots ADD COLUMN parsedManifests TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE architecture_snapshots ADD COLUMN unsupportedManifests TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE architecture_nodes ADD COLUMN resolvedVersion TEXT")
                db.execSQL("ALTER TABLE architecture_nodes ADD COLUMN ecosystem TEXT NOT NULL DEFAULT 'GRADLE'")
                db.execSQL("ALTER TABLE architecture_nodes ADD COLUMN projectRoot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE architecture_edges ADD COLUMN sourceKind TEXT NOT NULL DEFAULT 'manifest'")
            }

        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS repository_analysis (
                        analysisId TEXT NOT NULL PRIMARY KEY,
                        snapshotKey TEXT NOT NULL,
                        repositoryKey TEXT NOT NULL,
                        branch TEXT NOT NULL,
                        commitSha TEXT NOT NULL,
                        scannedAt INTEGER NOT NULL,
                        service TEXT NOT NULL,
                        model TEXT,
                        generatedAt INTEGER NOT NULL,
                        schemaVersion INTEGER NOT NULL,
                        evidenceIds TEXT NOT NULL,
                        status TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        findingsJson TEXT NOT NULL,
                        limitationsJson TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_repository_analysis_snapshotKey ON repository_analysis(snapshotKey)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_repository_analysis_snapshotKey")
                db.execSQL("DROP TABLE IF EXISTS repository_analysis")
            }

        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE architecture_snapshots ADD COLUMN scanStatsJson TEXT NOT NULL DEFAULT '{}'")
            }
        }
    }
}
