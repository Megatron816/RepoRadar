package com.example.githubprofileexplorer.ui.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubprofileexplorer.data.local.ProfileHistoryEntity
import com.example.githubprofileexplorer.data.local.ReadingQueueEntity
import com.example.githubprofileexplorer.data.local.ReadingStatus
import com.example.githubprofileexplorer.data.repository.LocalFeatureRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocalFeaturesViewModel(private val repository: LocalFeatureRepository) : ViewModel() {
    val readingQueue: StateFlow<List<ReadingQueueEntity>> = repository.readingQueue.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val profileHistory: StateFlow<List<ProfileHistoryEntity>> = repository.profileHistory.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    fun saveRepo(repo: com.example.githubprofileexplorer.data.model.GitHubRepo) =
        viewModelScope.launch { repository.saveRepo(repo) }
    fun removeRepo(repo: com.example.githubprofileexplorer.data.model.GitHubRepo) =
        viewModelScope.launch { repository.removeRepo(repo) }
    fun updateStatus(item: ReadingQueueEntity, status: ReadingStatus) =
        viewModelScope.launch { repository.updateStatus(item, status) }
    fun recordProfile(user: com.example.githubprofileexplorer.data.model.GithubUser) =
        viewModelScope.launch { repository.recordProfile(user) }
    fun refresh(item: ProfileHistoryEntity, onComplete: (String) -> Unit) =
        viewModelScope.launch {
            repository.refreshProfile(item).onSuccess { onComplete(it.user.login) }
        }
    fun removeProfile(login: String) = viewModelScope.launch { repository.removeProfile(login) }
    fun clearProfiles() = viewModelScope.launch { repository.clearProfiles() }
}
