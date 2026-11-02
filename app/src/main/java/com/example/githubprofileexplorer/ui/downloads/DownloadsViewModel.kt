package com.example.githubprofileexplorer.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubprofileexplorer.data.local.DownloadEntity
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.repository.DownloadProgress
import com.example.githubprofileexplorer.data.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-off messages for the UI layer (snackbars, opening a file). */
sealed interface DownloadsEvent {
    data class Started(val repoName: String) : DownloadsEvent
    data class OpenFile(val uri: String) : DownloadsEvent
    data class ShareFile(val uri: String) : DownloadsEvent
    data object FileMissing : DownloadsEvent
}

class DownloadsViewModel(private val repository: DownloadRepository) : ViewModel() {

    val history: StateFlow<List<DownloadEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val progress: StateFlow<Map<Long, DownloadProgress>> = repository.progress

    private val _events = MutableSharedFlow<DownloadsEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DownloadsEvent> = _events.asSharedFlow()

    fun download(repo: GitHubRepo) {
        viewModelScope.launch {
            repository.enqueue(repo)
            _events.emit(DownloadsEvent.Started(repo.name))
        }
    }

    fun retry(id: Long) {
        viewModelScope.launch { repository.retry(id) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }

    fun open(entity: DownloadEntity) {
        viewModelScope.launch {
            val uri = entity.fileUri
            if (uri != null && repository.isFileAvailable(uri)) {
                _events.emit(DownloadsEvent.OpenFile(uri))
            } else {
                _events.emit(DownloadsEvent.FileMissing)
            }
        }
    }

    fun share(entity: DownloadEntity) {
        viewModelScope.launch {
            val uri = entity.fileUri
            if (uri != null && repository.isFileAvailable(uri)) {
                _events.emit(DownloadsEvent.ShareFile(uri))
            } else {
                _events.emit(DownloadsEvent.FileMissing)
            }
        }
    }
}
