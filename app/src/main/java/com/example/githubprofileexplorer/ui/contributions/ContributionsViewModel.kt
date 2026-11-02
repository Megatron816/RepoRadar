package com.example.githubprofileexplorer.ui.contributions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubprofileexplorer.data.model.ContributionCalendar
import com.example.githubprofileexplorer.data.repository.ContributionService
import com.example.githubprofileexplorer.data.repository.ProfileError
import com.example.githubprofileexplorer.data.repository.asProfileError
import com.example.githubprofileexplorer.domain.ContributionCalculator
import com.example.githubprofileexplorer.domain.ContributionSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ContributionsState {
    data object Idle : ContributionsState
    data object SignedOut : ContributionsState
    data object Loading : ContributionsState
    data class Ready(val calendar: ContributionCalendar, val summary: ContributionSummary) : ContributionsState
    data class Error(val error: ProfileError) : ContributionsState
}

class ContributionsViewModel(private val service: ContributionService) : ViewModel() {

    private val _state = MutableStateFlow<ContributionsState>(ContributionsState.Idle)
    val state: StateFlow<ContributionsState> = _state.asStateFlow()

    private var loadedLogin: String? = null
    private var job: Job? = null

    fun load(login: String, signedIn: Boolean, force: Boolean = false) {
        if (!signedIn) {
            job?.cancel()
            loadedLogin = null
            _state.value = ContributionsState.SignedOut
            return
        }
        if (!force && loadedLogin == login && _state.value is ContributionsState.Ready) return
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = ContributionsState.Loading
            service.load(login).fold(
                onSuccess = { calendar ->
                    val days = calendar.weeks.flatMap { it.contributionDays }
                    loadedLogin = login
                    _state.value = ContributionsState.Ready(calendar, ContributionCalculator.summarize(days))
                },
                onFailure = { _state.value = ContributionsState.Error(it.asProfileError()) }
            )
        }
    }
}
