package com.example.githubprofileexplorer.data.repository

import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.model.GithubUser
import com.example.githubprofileexplorer.data.remote.GithubApi
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ProfileRepositoryTest {

    private val user = GithubUser("octocat", "The Octocat", null, 10, 1, 3, null)

    private fun repo(i: Int) = GitHubRepo("repo$i", null, "Kotlin", "https://github.com/octocat/repo$i", i)

    private fun <T> httpError(code: Int): Response<T> =
        Response.error(code, "".toResponseBody())

    private class FakeApi(
        val userResponse: () -> Response<GithubUser>,
        val repoResponse: (page: Int) -> Response<List<GitHubRepo>>
    ) : GithubApi {
        override suspend fun getUser(username: String) = userResponse()
        override suspend fun getRepositories(
            username: String, perPage: Int, sort: String, page: Int
        ) = repoResponse(page)
    }

    private fun load(api: GithubApi, name: String = "octocat") =
        runBlocking { ProfileRepository(api).loadProfile(name) }

    private fun Result<*>.errorOrNull() = (exceptionOrNull() as? ProfileException)?.error

    @Test
    fun validUsernames() {
        assertTrue(ProfileRepository.isValidUsername("octocat"))
        assertTrue(ProfileRepository.isValidUsername("a-b-c"))
        assertTrue(ProfileRepository.isValidUsername("a".repeat(39)))
    }

    @Test
    fun invalidUsernames() {
        assertFalse(ProfileRepository.isValidUsername(""))
        assertFalse(ProfileRepository.isValidUsername("-abc"))
        assertFalse(ProfileRepository.isValidUsername("abc-"))
        assertFalse(ProfileRepository.isValidUsername("a--b"))
        assertFalse(ProfileRepository.isValidUsername("a b"))
        assertFalse(ProfileRepository.isValidUsername("a/b"))
        assertFalse(ProfileRepository.isValidUsername("a".repeat(40)))
    }

    @Test
    fun invalidUsernameDoesNotHitNetwork() {
        val api = FakeApi({ error("should not be called") }, { error("should not be called") })
        assertEquals(ProfileError.INVALID_USERNAME, load(api, "bad name!").errorOrNull())
    }

    @Test
    fun trimsWhitespace() {
        val api = FakeApi({ Response.success(user) }, { Response.success(emptyList()) })
        assertTrue(load(api, "  octocat \n").isSuccess)
    }

    @Test
    fun notFound() {
        val api = FakeApi({ httpError(404) }, { error("unused") })
        assertEquals(ProfileError.NOT_FOUND, load(api).errorOrNull())
    }

    @Test
    fun rateLimited() {
        val api = FakeApi({ httpError(403) }, { error("unused") })
        assertEquals(ProfileError.RATE_LIMITED, load(api).errorOrNull())
    }

    @Test
    fun unauthorized() {
        val api = FakeApi({ httpError(401) }, { error("unused") })
        assertEquals(ProfileError.UNAUTHORIZED, load(api).errorOrNull())
    }

    @Test
    fun serverError() {
        val api = FakeApi({ httpError(503) }, { error("unused") })
        assertEquals(ProfileError.SERVER, load(api).errorOrNull())
    }

    @Test
    fun networkFailure() {
        val api = FakeApi({ throw IOException("offline") }, { error("unused") })
        assertEquals(ProfileError.NETWORK, load(api).errorOrNull())
    }

    @Test
    fun loadsUserAndRepos() {
        val api = FakeApi({ Response.success(user) }, { Response.success(listOf(repo(1), repo(2))) })
        val profile = load(api).getOrThrow()
        assertEquals("octocat", profile.user.login)
        assertEquals(2, profile.repos.size)
        assertNull(profile.reposError)
    }

    @Test
    fun paginatesUntilShortPage() {
        val pages = mutableListOf<Int>()
        val api = FakeApi({ Response.success(user) }) { page ->
            pages += page
            val size = if (page == 1) ProfileRepository.PAGE_SIZE else 5
            Response.success(List(size) { repo(page * 1000 + it) })
        }
        val profile = load(api).getOrThrow()
        assertEquals(listOf(1, 2), pages)
        assertEquals(ProfileRepository.PAGE_SIZE + 5, profile.repos.size)
    }

    @Test
    fun repoFailureKeepsUser() {
        val api = FakeApi({ Response.success(user) }, { httpError(500) })
        val profile = load(api).getOrThrow()
        assertEquals("octocat", profile.user.login)
        assertTrue(profile.repos.isEmpty())
        assertEquals(ProfileError.SERVER, profile.reposError)
    }

    @Test
    fun retryLoadsRepositoriesOnly() {
        val api = FakeApi({ error("should not be called") }, { Response.success(listOf(repo(1))) })
        val result = runBlocking { ProfileRepository(api).loadRepositories(" octocat ") }
        assertEquals(1, result.getOrThrow().size)
    }
}
