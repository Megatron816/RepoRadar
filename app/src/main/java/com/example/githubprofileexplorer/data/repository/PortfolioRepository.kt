package com.example.githubprofileexplorer.data.repository

import com.example.githubprofileexplorer.data.local.PortfolioDao
import com.example.githubprofileexplorer.data.local.PortfolioProfileEntity
import com.example.githubprofileexplorer.data.local.PortfolioProjectEntity
import com.example.githubprofileexplorer.data.local.portfolioKey
import com.example.githubprofileexplorer.data.local.portfolioRepositoryKey
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.model.GithubUser
import kotlinx.coroutines.flow.Flow

interface PortfolioStore {
    val savedProfiles: Flow<List<PortfolioProfileEntity>>
    fun selectedProjects(user: GithubUser): Flow<List<PortfolioProjectEntity>>
    suspend fun isSaved(user: GithubUser): Boolean
    suspend fun saveProfile(user: GithubUser)
    suspend fun removeProfile(user: GithubUser)
    suspend fun selectProject(user: GithubUser, repo: GitHubRepo)
    suspend fun deselectProject(user: GithubUser, repo: GitHubRepo)
}

class PortfolioRepository(private val dao: PortfolioDao) : PortfolioStore {
    override val savedProfiles: Flow<List<PortfolioProfileEntity>> = dao.observeProfiles()

    override fun selectedProjects(user: GithubUser): Flow<List<PortfolioProjectEntity>> =
        dao.observeProjects(user.portfolioKey())

    override suspend fun isSaved(user: GithubUser): Boolean = dao.isProfileSaved(user.portfolioKey())

    override suspend fun saveProfile(user: GithubUser) {
        dao.replaceProfile(
            user.toEntity()
        )
    }

    override suspend fun removeProfile(user: GithubUser) = dao.deleteProfile(user.portfolioKey())

    override suspend fun selectProject(user: GithubUser, repo: GitHubRepo) {
        require(user.login.isNotBlank() && repo.name.isNotBlank() && repo.htmlUrl.isNotBlank())
        dao.selectProject(
            profile = user.toEntity(),
            project = PortfolioProjectEntity(
                profileKey = user.portfolioKey(),
                repositoryKey = repo.portfolioRepositoryKey(),
                githubId = repo.id.takeIf { it > 0 },
                name = repo.name,
                description = repo.description,
                language = repo.language,
                htmlUrl = repo.htmlUrl,
                topics = repo.topics
            )
        )
    }

    override suspend fun deselectProject(user: GithubUser, repo: GitHubRepo) =
        dao.deselectProject(user.portfolioKey(), repo.portfolioRepositoryKey())

    private fun GithubUser.toEntity() = PortfolioProfileEntity(
        profileKey = portfolioKey(),
        githubId = id.takeIf { it > 0 },
        login = login,
        name = name,
        bio = bio,
        avatarUrl = avatarUrl,
        followers = followers,
        following = following,
        publicRepos = publicRepos
    )
}
