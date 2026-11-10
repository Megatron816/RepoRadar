package com.example.githubprofileexplorer.ui.gemini

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubprofileexplorer.data.repository.GeminiError
import com.example.githubprofileexplorer.data.repository.GeminiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GeminiUiState(
    val prompt: String = "",
    val response: String? = null,
    val isLoading: Boolean = false,
    val error: GeminiError? = null
)

class GeminiViewModel(private val repository: GeminiRepository) : ViewModel() {
    private val _state = MutableStateFlow(GeminiUiState())
    val state: StateFlow<GeminiUiState> = _state.asStateFlow()
    private var requestJob: Job? = null

    fun onPromptChange(prompt: String) {
        _state.update { it.copy(prompt = prompt, error = null) }
    }

    fun generate() {
        val prompt = _state.value.prompt.trim()
        if (prompt.isEmpty() || _state.value.isLoading) return
        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, response = null, error = null) }
            repository.generate(prompt).fold(
                onSuccess = { response ->
                    _state.update { it.copy(isLoading = false, response = response) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error as? GeminiError ?: GeminiError.Unknown)
                    }
                }
            )
        }
    }
}
