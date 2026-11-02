package com.example.githubprofileexplorer.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.repository.Profile
import com.example.githubprofileexplorer.data.repository.ProfileError
import com.example.githubprofileexplorer.data.repository.ProfileRepository
import com.example.githubprofileexplorer.data.repository.ReadmeRepository
import com.example.githubprofileexplorer.data.repository.asProfileError
import com.example.githubprofileexplorer.domain.MarkdownParser
import com.example.githubprofileexplorer.domain.MdBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Success(val profile: Profile, val reloadingRepos: Boolean = false) : SearchState
    data class Error(val error: ProfileError) : SearchState
}

sealed interface ReadmeState {
    data object Idle : ReadmeState
    data object Loading : ReadmeState
    data class Ready(val blocks: List<MdBlock>) : ReadmeState
    data object Unavailable : ReadmeState
    data class Error(val error: ProfileError) : ReadmeState
}

data class ProfileUiState(
    val query: String = "",
    val search: SearchState = SearchState.Idle,
    val selectedRepo: GitHubRepo? = null,
    val readme: ReadmeState = ReadmeState.Idle
) {
    val profile: Profile? get() = (search as? SearchState.Success)?.profile
}

class ProfileViewModel(
    private val profiles: ProfileRepository,
    private val readmes: ReadmeRepository,
    private val onProfileViewed: suspend (com.example.githubprofileexplorer.data.model.GithubUser) -> Unit = {}
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var readmeJob: Job? = null
    private var handledDeepLink: String? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun openUsername(username: String) {
        onQueryChange(username)
        search()
    }

    /** Opens a profile from a `github.com/<user>` link once, even if the activity is recreated. */
    fun handleDeepLink(username: String) {
        if (handledDeepLink == username) return
        handledDeepLink = username
        openUsername(username)
    }

    fun search() {
        val query = _uiState.value.query
        searchJob?.cancel()
        readmeJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(search = SearchState.Loading, selectedRepo = null, readme = ReadmeState.Idle)
            }
            val result = profiles.loadProfile(query)
            _uiState.update {
                it.copy(
                    search = result.fold(
                        onSuccess = { profile -> SearchState.Success(profile) },
                        onFailure = { e -> SearchState.Error(e.asProfileError()) }
                    )
                )
            }
            result.getOrNull()?.let { onProfileViewed(it.user) }
        }
    }

    /** Retries only the repository list for an already loaded user. */
    fun retryRepositories() {
        val current = _uiState.value.search as? SearchState.Success ?: return
        if (current.reloadingRepos) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(search = current.copy(reloadingRepos = true)) }
            val result = profiles.loadRepositories(current.profile.user.login)
            _uiState.update {
                it.copy(
                    search = SearchState.Success(
                        result.fold(
                            onSuccess = { repos -> current.profile.copy(repos = repos, reposError = null) },
                            onFailure = { e -> current.profile.copy(reposError = e.asProfileError()) }
                        )
                    )
                )
            }
        }
    }

    /** Selects a repository for the detail screen (and loads its README), or clears the selection. */
    fun selectRepo(repo: GitHubRepo?) {
        readmeJob?.cancel()
        if (repo == null) {
            _uiState.update { it.copy(selectedRepo = null, readme = ReadmeState.Idle) }
            return
        }
        _uiState.update { it.copy(selectedRepo = repo, readme = ReadmeState.Loading) }
        readmeJob = viewModelScope.launch {
            val state = readmes.load(repo).fold(
                onSuccess = { text ->
                    if (text.isNullOrBlank()) ReadmeState.Unavailable
                    else ReadmeState.Ready(withContext(Dispatchers.Default) { MarkdownParser.parse(text) })
                },
                onFailure = { e -> ReadmeState.Error(e.asProfileError()) }
            )
            _uiState.update { it.copy(readme = state) }
        }
    }
}
