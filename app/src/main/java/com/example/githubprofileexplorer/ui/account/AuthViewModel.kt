package com.example.githubprofileexplorer.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubprofileexplorer.data.auth.AuthException
import com.example.githubprofileexplorer.data.auth.AuthFailure
import com.example.githubprofileexplorer.data.auth.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object SignedOut : AuthState
    data object Starting : AuthState
    data class AwaitingVerification(val userCode: String, val url: String) : AuthState
    data object SignedIn : AuthState
    data class Error(val failure: AuthFailure) : AuthState
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(
        if (repository.isSignedIn()) AuthState.SignedIn else AuthState.SignedOut
    )
    val state: StateFlow<AuthState> = _state.asStateFlow()

    val isConfigured: Boolean get() = repository.isConfigured

    private var job: Job? = null

    fun signIn() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = AuthState.Starting
            val session = repository.requestDeviceSession().getOrElse {
                _state.value = AuthState.Error(it.toFailure())
                return@launch
            }
            _state.value = AuthState.AwaitingVerification(session.userCode, session.verificationUrl)
            repository.completeSignIn(session).fold(
                onSuccess = { _state.value = AuthState.SignedIn },
                onFailure = { _state.value = AuthState.Error(it.toFailure()) }
            )
        }
    }

    fun cancel() {
        job?.cancel()
        _state.value = AuthState.SignedOut
    }

    fun signOut() {
        job?.cancel()
        repository.signOut()
        _state.value = AuthState.SignedOut
    }

    private fun Throwable.toFailure(): AuthFailure =
        (this as? AuthException)?.failure ?: AuthFailure.UNKNOWN
}
