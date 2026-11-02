package com.example.githubprofileexplorer.ui.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubprofileexplorer.data.repository.ContributionService
import com.example.githubprofileexplorer.data.repository.Profile
import com.example.githubprofileexplorer.data.repository.ProfileError
import com.example.githubprofileexplorer.data.repository.ProfileRepository
import com.example.githubprofileexplorer.data.repository.asProfileError
import com.example.githubprofileexplorer.domain.LanguageAnalytics
import com.example.githubprofileexplorer.domain.RepoHealthScorer
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompareSide(
    val login: String,
    val profile: Profile?,
    val error: ProfileError?,
    val contributions: Int?
) {
    val totalStars: Int? get() = profile?.repos?.sumOf { it.stargazersCount }

    val averageHealth: Int?
        get() = profile?.repos
            ?.takeIf { it.isNotEmpty() }
            ?.let { repos ->
                val scorer = RepoHealthScorer()
                repos.map { scorer.score(it).value }.average().toInt()
            }

    val topLanguages: List<String>
        get() = profile?.let { LanguageAnalytics.aggregate(it.repos).take(3).map { slice -> slice.language } }
            .orEmpty()
}

data class CompareResult(
    val left: CompareSide,
    val right: CompareSide,
    val includesContributions: Boolean
)

data class CompareUiState(
    val left: String = "",
    val right: String = "",
    val loading: Boolean = false,
    val result: CompareResult? = null
) {
    val canCompare: Boolean get() = left.isNotBlank() && right.isNotBlank() && !loading
}

class CompareViewModel(
    private val profiles: ProfileRepository,
    private val contributionService: ContributionService,
    private val isSignedIn: () -> Boolean
) : ViewModel() {

    private val _state = MutableStateFlow(CompareUiState())
    val state: StateFlow<CompareUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun onLeftChange(value: String) = _state.update { it.copy(left = value) }

    fun onRightChange(value: String) = _state.update { it.copy(right = value) }

    fun compare() {
        val snapshot = _state.value
        if (!snapshot.canCompare) return
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val signedIn = isSignedIn()
            val (left, right) = coroutineScope {
                val first = async { loadSide(snapshot.left, signedIn) }
                val second = async { loadSide(snapshot.right, signedIn) }
                first.await() to second.await()
            }
            _state.update {
                it.copy(loading = false, result = CompareResult(left, right, includesContributions = signedIn))
            }
        }
    }

    private suspend fun loadSide(rawLogin: String, signedIn: Boolean): CompareSide {
        val result = profiles.loadProfile(rawLogin)
        val profile = result.getOrNull()
        val contributions = if (signedIn && profile != null) {
            contributionService.load(profile.user.login).getOrNull()?.totalContributions
        } else {
            null
        }
        return CompareSide(
            login = rawLogin.trim(),
            profile = profile,
            error = result.exceptionOrNull()?.asProfileError(),
            contributions = contributions
        )
    }
}
