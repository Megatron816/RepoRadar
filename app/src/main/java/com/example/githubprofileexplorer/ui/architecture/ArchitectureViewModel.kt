package com.example.githubprofileexplorer.ui.architecture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubprofileexplorer.data.repository.ArchitectureError
import com.example.githubprofileexplorer.data.repository.ArchitectureRepository
import com.example.githubprofileexplorer.data.repository.ArchitectureScanResult
import com.example.githubprofileexplorer.domain.ArchitectureGraph
import com.example.githubprofileexplorer.domain.ArchitectureEcosystem
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
    val ecosystem: ArchitectureEcosystem? = null
)

class ArchitectureViewModel(private val repository: ArchitectureRepository) : ViewModel() {
    private val _state = MutableStateFlow(ArchitectureUiState())
    val state: StateFlow<ArchitectureUiState> = _state.asStateFlow()
    private var scanJob: Job? = null

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
            repository.load(snapshotKey)?.let { _state.update { state -> state.copy(state = ArchitectureState.Success(it)) } }
        }
    }

    fun delete(snapshotKey: String) { viewModelScope.launch { repository.delete(snapshotKey) } }
    fun setQuery(value: String) { _state.update { it.copy(query = value) } }
    fun setModulesOnly(value: Boolean) { _state.update { it.copy(modulesOnly = value) } }
    fun setEcosystem(value: ArchitectureEcosystem?) { _state.update { it.copy(ecosystem = value) } }

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
