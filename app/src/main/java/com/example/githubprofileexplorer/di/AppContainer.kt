package com.example.githubprofileexplorer.di

import android.content.Context
import com.example.githubprofileexplorer.data.auth.AuthRepository
import com.example.githubprofileexplorer.data.auth.SecureTokenStore
import com.example.githubprofileexplorer.data.local.AppDatabase
import com.example.githubprofileexplorer.data.remote.NetworkModule
import com.example.githubprofileexplorer.data.repository.ContributionService
import com.example.githubprofileexplorer.data.repository.DownloadRepository
import com.example.githubprofileexplorer.data.repository.ProfileRepository
import com.example.githubprofileexplorer.data.repository.ReadmeRepository
import com.example.githubprofileexplorer.data.repository.LocalFeatureRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Hand-rolled dependency container; one instance lives for the whole process. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val tokenStore: SecureTokenStore by lazy { SecureTokenStore(appContext) }
    private val network: NetworkModule by lazy { NetworkModule(appContext, tokenStore) }
    private val database: AppDatabase by lazy { AppDatabase.create(appContext) }

    val profileRepository: ProfileRepository by lazy { ProfileRepository(network.githubApi) }
    val readmeRepository: ReadmeRepository by lazy { ReadmeRepository(network.repoApi) }
    val contributionService: ContributionService by lazy { ContributionService(network.graphQlApi) }
    val authRepository: AuthRepository by lazy { AuthRepository(tokenStore, network.oauthApi) }
    val downloadRepository: DownloadRepository by lazy {
        DownloadRepository(appContext, database.downloadDao(), network.downloadApi, applicationScope)
    }
    val localFeatureRepository: LocalFeatureRepository by lazy {
        LocalFeatureRepository(database.readingQueueDao(), database.profileHistoryDao(), profileRepository)
    }

    fun onAppStart() {
        applicationScope.launch { downloadRepository.markInterrupted() }
    }
}
