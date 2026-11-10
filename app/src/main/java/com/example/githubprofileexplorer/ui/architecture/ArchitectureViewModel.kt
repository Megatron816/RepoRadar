package com.example.githubprofileexplorer.ui.architecture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubprofileexplorer.data.repository.ArchitectureError
import com.example.githubprofileexplorer.data.repository.ArchitectureRepository
import com.example.githubprofileexplorer.data.repository.ArchitectureScanResult
import com.example.githubprofileexplorer.data.repository.ArchitectureAiRepository
import com.example.githubprofileexplorer.data.repository.GeminiError
import com.example.githubprofileexplorer.domain.ArchitectureGraph
import com.example.githubprofileexplorer.domain.ArchitectureEcosystem
import com.example.githubprofileexplorer.data.repository.toAiContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ArchitectureState {
    data object Idle : ArchitectureState
    data object Loading : ArchitectureState
    data class Success(val result: ArchitectureScanResult) : ArchitectureState
    data class Error(val error: ArchitectureError) : ArchitectureState
}

data class ArchitectureUiState(
    val state: ArchitectureState = ArchitectureState.Idle,
    val snapshots: List<com.example.githubprofileexplorer.data.local.ArchitectureSnapshotEntity> = emptyList(),
    val query: String = "",
    val modulesOnly: Boolean = false,
    val ecosystem: ArchitectureEcosystem? = null,
    val ai: ArchitectureAiState = ArchitectureAiState()
)

data class ArchitectureAiState(
    val prompt: String = "",
    val response: String? = null,
    val isLoading: Boolean = false,
    val error: GeminiError? = null
)

class ArchitectureViewModel(
    private val repository: ArchitectureRepository,
    private val aiRepository: ArchitectureAiRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ArchitectureUiState())
    val state: StateFlow<ArchitectureUiState> = _state.asStateFlow()
    private var scanJob: Job? = null
    private var aiJob: Job? = null
    private val responseCache = mutableMapOf<String, String>()

    init {
        viewModelScope.launch { repository.observeSnapshots().collect { snapshots -> _state.update { it.copy(snapshots = snapshots) } } }
    }

    fun scan(owner: String, name: String, branch: String? = null) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _state.update { it.copy(state = ArchitectureState.Loading) }
            val result = repository.scan(owner, name, branch)
            _state.update {
                it.copy(
                    state = result.fold(ArchitectureState::Success) {
                        ArchitectureState.Error((it as? com.example.githubprofileexplorer.data.repository.ArchitectureException)?.error ?: ArchitectureError.UNKNOWN)
                    }
                )
            }
        }
    }

    fun open(snapshotKey: String) {
        viewModelScope.launch {
            repository.load(snapshotKey)?.let {
                _state.update { state -> state.copy(state = ArchitectureState.Success(it), ai = ArchitectureAiState()) }
            }
        }
    }

    fun delete(snapshotKey: String) { viewModelScope.launch { repository.delete(snapshotKey) } }
    fun setQuery(value: String) { _state.update { it.copy(query = value) } }
    fun setModulesOnly(value: Boolean) { _state.update { it.copy(modulesOnly = value) } }
    fun setEcosystem(value: ArchitectureEcosystem?) { _state.update { it.copy(ecosystem = value) } }
    fun onAiPromptChange(value: String) { _state.update { it.copy(ai = it.ai.copy(prompt = value, error = null)) } }

    fun generateAiInsights() {
        val result = (_state.value.state as? ArchitectureState.Success)?.result ?: return
        val key = "${result.graph.repository.key}:${result.graph.commitSha}:overview"
        responseCache[key]?.let { cached ->
            _state.update { it.copy(ai = ArchitectureAiState(response = cached)) }
            return
        }
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _state.update { it.copy(ai = it.ai.copy(isLoading = true, response = null, error = null)) }
            aiRepository.overview(result.toAiContext()).fold(
                onSuccess = { response ->
                    responseCache[key] = response
                    _state.update { it.copy(ai = it.ai.copy(isLoading = false, response = response)) }
                },
                onFailure = { error ->
                    _state.update { it.copy(ai = it.ai.copy(isLoading = false, error = error as? GeminiError ?: GeminiError.Unknown)) }
                }
            )
        }
    }

    fun askAi() {
        val result = (_state.value.state as? ArchitectureState.Success)?.result ?: return
        val question = _state.value.ai.prompt.trim()
        if (question.isEmpty() || _state.value.ai.isLoading) return
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _state.update { it.copy(ai = it.ai.copy(isLoading = true, response = null, error = null)) }
            aiRepository.question(result.toAiContext(), question).fold(
                onSuccess = { response -> _state.update { it.copy(ai = it.ai.copy(isLoading = false, response = response)) } },
                onFailure = { error -> _state.update { it.copy(ai = it.ai.copy(isLoading = false, error = error as? GeminiError ?: GeminiError.Unknown)) } }
            )
        }
    }

    fun filteredGraph(): ArchitectureGraph? {
        val result = (_state.value.state as? ArchitectureState.Success)?.result ?: return null
        val query = _state.value.query.trim().lowercase()
        val keep = result.graph.nodes.filter {
            (!_state.value.modulesOnly || it.type == com.example.githubprofileexplorer.domain.ArchitectureNodeType.MODULE) &&
                (_state.value.ecosystem == null || it.ecosystem == _state.value.ecosystem) &&
                (query.isBlank() || it.label.lowercase().contains(query))
        }.map { it.id }.toSet()
        return result.graph.copy(
            nodes = result.graph.nodes.filter { it.id in keep },
            edges = result.graph.edges.filter { it.from in keep && it.to in keep }
        )
    }
}
