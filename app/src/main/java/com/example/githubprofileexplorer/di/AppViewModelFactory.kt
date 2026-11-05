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
                isSignedIn = { container.tokenStore.isLoggedIn() }
            )

        modelClass.isAssignableFrom(DownloadsViewModel::class.java) ->
            DownloadsViewModel(container.downloadRepository)

        modelClass.isAssignableFrom(LocalFeaturesViewModel::class.java) ->
            LocalFeaturesViewModel(container.localFeatureRepository)

        modelClass.isAssignableFrom(PortfolioViewModel::class.java) ->
            PortfolioViewModel(container.portfolioRepository, container.portfolioPdfGenerator)

        modelClass.isAssignableFrom(ArchitectureViewModel::class.java) ->
            ArchitectureViewModel(container.architectureRepository)

        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    } as T
}
