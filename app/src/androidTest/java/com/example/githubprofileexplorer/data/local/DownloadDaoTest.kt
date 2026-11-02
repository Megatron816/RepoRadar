package com.example.githubprofileexplorer.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DownloadDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.downloadDao()
    }

    @After
    fun closeDb() = db.close()

    private fun entity(repo: String, createdAt: Long, status: DownloadStatus = DownloadStatus.DOWNLOADING) =
        DownloadEntity(
            owner = "octocat",
            repoName = repo,
            branch = "main",
            htmlUrl = "https://github.com/octocat/$repo",
            fileName = "octocat-$repo-main.zip",
            status = status,
            createdAt = createdAt
        )

    @Test
    fun historyIsNewestFirst() = runBlocking {
        dao.insert(entity("old", 1))
        dao.insert(entity("new", 2))
        assertEquals(listOf("new", "old"), dao.observeAll().first().map { it.repoName })
    }

    @Test
    fun findsActiveDownloadOfSameRepo() = runBlocking {
        dao.insert(entity("hello", 1))
        assertNotNull(dao.findByStatus("octocat", "hello", DownloadStatus.DOWNLOADING))
        assertNull(dao.findByStatus("octocat", "other", DownloadStatus.DOWNLOADING))
    }

    @Test
    fun interruptedDownloadsAreMarkedFailed() = runBlocking {
        val id = dao.insert(entity("hello", 1))
        dao.insert(entity("done", 2, DownloadStatus.COMPLETED))
        dao.changeStatus(DownloadStatus.DOWNLOADING, DownloadStatus.FAILED, DownloadError.INTERRUPTED)

        val interrupted = dao.get(id)
        assertEquals(DownloadStatus.FAILED, interrupted?.status)
        assertEquals(DownloadError.INTERRUPTED, interrupted?.error)
        assertEquals(1, dao.getAll().count { it.status == DownloadStatus.COMPLETED })
    }

    @Test
    fun deleteRemovesEntries() = runBlocking {
        val id = dao.insert(entity("a", 1))
        dao.insert(entity("b", 2))
        dao.delete(id)
        assertEquals(1, dao.getAll().size)
        dao.deleteAll()
        assertEquals(0, dao.getAll().size)
    }
}
