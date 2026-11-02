package com.example.githubprofileexplorer.data.repository

import com.example.githubprofileexplorer.data.local.ProfileHistoryDao
import com.example.githubprofileexplorer.data.local.ProfileHistoryEntity
import com.example.githubprofileexplorer.data.local.ReadingQueueDao
import com.example.githubprofileexplorer.data.local.ReadingQueueEntity
import com.example.githubprofileexplorer.data.local.ReadingStatus
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.model.GithubUser
import kotlinx.coroutines.flow.Flow

class LocalFeatureRepository(
    private val queueDao: ReadingQueueDao,
    private val historyDao: ProfileHistoryDao,
    private val profiles: ProfileRepository
) {
    val readingQueue: Flow<List<ReadingQueueEntity>> = queueDao.observeAll()
    val profileHistory: Flow<List<ProfileHistoryEntity>> = historyDao.observeAll()

    suspend fun saveRepo(repo: GitHubRepo) {
        queueDao.insert(
            ReadingQueueEntity(
                owner = repo.ownerLogin,
                repoName = repo.name,
                description = repo.description,
                language = repo.language,
                htmlUrl = repo.htmlUrl,
                defaultBranch = repo.defaultBranch,
                stars = repo.stargazersCount,
                forks = repo.forksCount
            )
        )
    }

    suspend fun removeRepo(repo: GitHubRepo) = queueDao.delete(repo.ownerLogin, repo.name)

    suspend fun updateStatus(item: ReadingQueueEntity, status: ReadingStatus) {
        queueDao.update(item.copy(status = status))
    }

    suspend fun recordProfile(user: GithubUser) {
        historyDao.insert(
            ProfileHistoryEntity(
                login = user.login,
                name = user.name,
                bio = user.bio,
                avatarUrl = user.avatarUrl,
                followers = user.followers,
                following = user.following,
                publicRepos = user.publicRepos
            )
        )
    }

    suspend fun refreshProfile(item: ProfileHistoryEntity): Result<Profile> {
        val result = profiles.loadProfile(item.login)
        result.getOrNull()?.let { recordProfile(it.user) }
        return result
    }

    suspend fun removeProfile(login: String) = historyDao.delete(login)
    suspend fun clearProfiles() = historyDao.deleteAll()
}
