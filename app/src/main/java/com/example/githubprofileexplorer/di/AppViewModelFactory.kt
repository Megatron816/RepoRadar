package com.example.githubprofileexplorer.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.githubprofileexplorer.ui.account.AuthViewModel
import com.example.githubprofileexplorer.ui.compare.CompareViewModel
import com.example.githubprofileexplorer.ui.contributions.ContributionsViewModel
import com.example.githubprofileexplorer.ui.downloads.DownloadsViewModel
import com.example.githubprofileexplorer.ui.profile.ProfileViewModel
import com.example.githubprofileexplorer.ui.local.LocalFeaturesViewModel
import com.example.githubprofileexplorer.ui.portfolio.PortfolioViewModel
import com.example.githubprofileexplorer.ui.architecture.ArchitectureViewModel
import com.example.githubprofileexplorer.ui.gemini.GeminiViewModel

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
            ProfileViewModel(
                container.profileRepository,
                container.readmeRepository,
                container.localFeatureRepository::recordProfile
            )

        modelClass.isAssignableFrom(AuthViewModel::class.java) ->
            AuthViewModel(container.authRepository)

        modelClass.isAssignableFrom(ContributionsViewModel::class.java) ->
            ContributionsViewModel(container.contributionService)

        modelClass.isAssignableFrom(CompareViewModel::class.java) ->
            CompareViewModel(
                container.profileRepository,
                container.contributionService,
                container.geminiRepository,
                isSignedIn = { container.tokenStore.isLoggedIn() }
            )

        modelClass.isAssignableFrom(DownloadsViewModel::class.java) ->
            DownloadsViewModel(container.downloadRepository)

        modelClass.isAssignableFrom(LocalFeaturesViewModel::class.java) ->
            LocalFeaturesViewModel(container.localFeatureRepository)

        modelClass.isAssignableFrom(PortfolioViewModel::class.java) ->
            PortfolioViewModel(container.portfolioRepository, container.portfolioPdfGenerator, container.geminiRepository)

        modelClass.isAssignableFrom(ArchitectureViewModel::class.java) ->
            ArchitectureViewModel(container.architectureRepository, container.architectureAiRepository)

        modelClass.isAssignableFrom(GeminiViewModel::class.java) ->
            GeminiViewModel(container.geminiRepository)

        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    } as T
}
