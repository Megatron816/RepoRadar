package com.example.githubprofileexplorer.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubprofileexplorer.data.export.PortfolioPdfGenerator
import com.example.githubprofileexplorer.data.local.PortfolioProjectEntity
import com.example.githubprofileexplorer.data.local.portfolioKey
import com.example.githubprofileexplorer.data.local.portfolioRepositoryKey
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.model.GithubUser
import com.example.githubprofileexplorer.data.repository.PortfolioStore
import com.example.githubprofileexplorer.domain.PortfolioRepoSort
import com.example.githubprofileexplorer.domain.PortfolioPdfDataMapper
import com.example.githubprofileexplorer.domain.RepositoryFilters
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class PortfolioUiState(
    val filters: RepositoryFilters = RepositoryFilters(includeArchived = false, includeForks = false),
    val sort: PortfolioRepoSort = PortfolioRepoSort.UPDATED,
    val minimumStarsText: String = "",
    val recentDaysText: String = "",
    val saved: Boolean = false,
    val lastFetchedAt: Long? = null,
    val exportState: PortfolioExportState = PortfolioExportState.Idle,
    val message: PortfolioMessage? = null
)

enum class PortfolioMessage {
    INVALID_SELECTION,
    SAVE_FAILED,
    REMOVE_FAILED
}

sealed interface PortfolioExportState {
    data object Idle : PortfolioExportState
    data object Generating : PortfolioExportState
    data class Ready(val file: java.io.File) : PortfolioExportState
    data object Error : PortfolioExportState
}

class PortfolioViewModel(
    private val portfolios: PortfolioStore,
    private val pdfGenerator: PortfolioPdfGenerator
) : ViewModel() {
    private val _state = MutableStateFlow(PortfolioUiState())
    val state: StateFlow<PortfolioUiState> = _state.asStateFlow()
    private val _selectedProjects = MutableStateFlow<List<PortfolioProjectEntity>>(emptyList())
    val selectedProjects: StateFlow<List<PortfolioProjectEntity>> = _selectedProjects.asStateFlow()
    private var selectedUser: GithubUser? = null
    private var projectsJob: Job? = null
    private val projectActionsInFlight = mutableSetOf<String>()

    fun open(user: GithubUser, fetchedAt: Long = System.currentTimeMillis()) {
        if (selectedUser?.portfolioKey() == user.portfolioKey()) {
            _state.update { it.copy(lastFetchedAt = fetchedAt) }
            return
        }
        selectedUser = user
        _state.update { it.copy(lastFetchedAt = fetchedAt) }
        projectsJob?.cancel()
        projectsJob = viewModelScope.launch {
            try {
                _state.update { it.copy(saved = portfolios.isSaved(user)) }
                portfolios.selectedProjects(user).collect { _selectedProjects.value = it }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(message = PortfolioMessage.SAVE_FAILED) }
            }
        }
    }

    fun setLanguage(value: String) = _state.update { it.copy(filters = it.filters.copy(language = value.trim().ifBlank { null })) }
    fun setTopic(value: String) = _state.update { it.copy(filters = it.filters.copy(topic = value.trim().ifBlank { null })) }
    fun setMinimumStars(value: String) {
        _state.update {
            it.copy(
                minimumStarsText = value.filter(Char::isDigit),
                filters = it.filters.copy(minimumStars = value.toIntOrNull()?.coerceAtLeast(0) ?: 0)
            )
        }
    }
    fun setRecentDays(value: String) {
        _state.update {
            it.copy(
                recentDaysText = value.filter(Char::isDigit),
                filters = it.filters.copy(updatedWithinDays = value.toIntOrNull()?.takeIf { days -> days > 0 })
            )
        }
    }
    fun setIncludeArchived(value: Boolean) = _state.update { it.copy(filters = it.filters.copy(includeArchived = value)) }
    fun setIncludeForks(value: Boolean) = _state.update { it.copy(filters = it.filters.copy(includeForks = value)) }
    fun setSort(value: PortfolioRepoSort) = _state.update { it.copy(sort = value) }
    fun clearFilters() = _state.update {
        it.copy(
            filters = RepositoryFilters(includeArchived = false, includeForks = false),
            minimumStarsText = "",
            recentDaysText = ""
        )
    }

    fun toggleSaved() {
        val user = selectedUser ?: return
        viewModelScope.launch {
            runCatching {
                if (_state.value.saved) portfolios.removeProfile(user) else portfolios.saveProfile(user)
            }.onSuccess {
                _state.update { it.copy(saved = !it.saved) }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _state.update {
                    it.copy(
                        message = if (it.saved) PortfolioMessage.REMOVE_FAILED else PortfolioMessage.SAVE_FAILED
                    )
                }
            }
        }
    }

    fun toggleProject(repo: GitHubRepo) {
        val user = selectedUser ?: run {
            _state.update { it.copy(message = PortfolioMessage.INVALID_SELECTION) }
            return
        }
        val key = repoKey(repo)
        if (key in projectActionsInFlight) return
        projectActionsInFlight += key
        viewModelScope.launch {
            try {
                val selected = _selectedProjects.value.any { it.repositoryKey == key }
                if (selected) portfolios.deselectProject(user, repo) else portfolios.selectProject(user, repo)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                _state.update { it.copy(message = PortfolioMessage.INVALID_SELECTION) }
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        message = if (_selectedProjects.value.any { project -> project.repositoryKey == key }) {
                            PortfolioMessage.REMOVE_FAILED
                        } else {
                            PortfolioMessage.SAVE_FAILED
                        }
                    )
                }
            } finally {
                projectActionsInFlight -= key
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun export(profile: com.example.githubprofileexplorer.data.repository.Profile) {
        if (_state.value.exportState is PortfolioExportState.Generating) return
        val projects = _selectedProjects.value
        _state.update { it.copy(exportState = PortfolioExportState.Generating) }
        viewModelScope.launch {
            runCatching {
                val data = PortfolioPdfDataMapper.map(profile, projects, _state.value.lastFetchedAt)
                pdfGenerator.generate(data)
            }.onSuccess { file ->
                _state.update { it.copy(exportState = PortfolioExportState.Ready(file)) }
            }.onFailure {
                _state.update { it.copy(exportState = PortfolioExportState.Error) }
            }
        }
    }

    fun clearExportState() {
        _state.update { it.copy(exportState = PortfolioExportState.Idle) }
    }

    private fun repoKey(repo: GitHubRepo): String =
        repo.portfolioRepositoryKey()
}
