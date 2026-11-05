package com.example.githubprofileexplorer.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.model.GithubUser
import com.example.githubprofileexplorer.data.repository.PortfolioRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PortfolioDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: PortfolioDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()
        dao = db.portfolioDao()
    }

    @After
    fun closeDb() = db.close()

    private val profile = PortfolioProfileEntity(
        profileKey = "github-user:1",
        githubId = 1,
        login = "octocat",
        name = "Octocat",
        bio = null,
        avatarUrl = null,
        followers = 1,
        following = 1,
        publicRepos = 1
    )

    private val project = PortfolioProjectEntity(
        profileKey = profile.profileKey,
        repositoryKey = "github-repo:2",
        githubId = 2,
        name = "demo",
        description = null,
        language = "Kotlin",
        htmlUrl = "https://github.com/octocat/demo",
        topics = listOf("android")
    )

    @Test
    fun duplicateSelectionIsIgnoredAndDeletingProfileCascades() = runBlocking {
        dao.saveProfile(profile)
        dao.selectProject(profile, project)
        dao.selectProject(profile, project)
        assertEquals(1, dao.observeProjects(profile.profileKey).first().size)
        dao.deleteProfile(profile.profileKey)
        assertEquals(emptyList<PortfolioProjectEntity>(), dao.observeProjects(profile.profileKey).first())
    }

    @Test
    fun selectingProjectCreatesMissingParentProfile() = runBlocking {
        val repository = PortfolioRepository(dao)
        repository.selectProject(
            GithubUser(
                login = "octocat",
                name = "Octocat",
                bio = null,
                followers = 1,
                following = 1,
                publicRepos = 1,
                avatarUrl = null,
                id = 1
            ),
            GitHubRepo(
                name = "demo",
                description = null,
                language = "Kotlin",
                htmlUrl = project.htmlUrl,
                topics = listOf("android"),
                id = 2
            )
        )

        assertEquals(true, dao.isProfileSaved(profile.profileKey))
        assertEquals(listOf(project), dao.observeProjects(profile.profileKey).first())
    }

    @Test
    fun invalidRepositoryIsRejectedWithoutWriting() = runBlocking {
        val repository = PortfolioRepository(dao)
        val user = GithubUser(
            login = "octocat",
            name = null,
            bio = null,
            followers = 0,
            following = 0,
            publicRepos = 0,
            avatarUrl = null,
            id = 1
        )
        val invalidRepo = GitHubRepo(
            name = "",
            description = null,
            language = null,
            htmlUrl = "",
            id = 2
        )

        val rejected = runCatching { repository.selectProject(user, invalidRepo) }.isFailure

        assertTrue(rejected)
        assertEquals(emptyList<PortfolioProjectEntity>(), dao.observeProjects(user.portfolioKey()).first())
    }
}
