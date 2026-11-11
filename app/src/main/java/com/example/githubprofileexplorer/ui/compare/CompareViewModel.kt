package com.example.githubprofileexplorer.ui.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.repository.ContributionService
import com.example.githubprofileexplorer.data.repository.GeminiError
import com.example.githubprofileexplorer.data.repository.GeminiRepository
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

    val totalForks: Int? get() = profile?.repos?.sumOf { it.forksCount }

    val totalOpenIssues: Int? get() = profile?.repos?.sumOf { it.openIssuesCount }

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

    val topStarredRepo: GitHubRepo?
        get() = profile?.repos?.maxByOrNull { it.stargazersCount }
}

data class CompareAiState(
    val prompt: String = "",
    val response: String? = null,
    val isLoading: Boolean = false,
    val error: GeminiError? = null
)

data class CompareResult(
    val left: CompareSide,
    val right: CompareSide,
    val includesContributions: Boolean
)

data class CompareUiState(
    val left: String = "",
    val right: String = "",
    val loading: Boolean = false,
    val result: CompareResult? = null,
    val ai: CompareAiState = CompareAiState()
) {
    val canCompare: Boolean get() = left.isNotBlank() && right.isNotBlank() && !loading
}

class CompareViewModel(
    private val profiles: ProfileRepository,
    private val contributionService: ContributionService,
    private val geminiRepository: GeminiRepository,
    private val isSignedIn: () -> Boolean
) : ViewModel() {

    private val _state = MutableStateFlow(CompareUiState())
    val state: StateFlow<CompareUiState> = _state.asStateFlow()

    private var job: Job? = null
    private var aiJob: Job? = null
    private val aiResponseCache = mutableMapOf<String, String>()

    fun onLeftChange(value: String) = _state.update { it.copy(left = value) }

    fun onRightChange(value: String) = _state.update { it.copy(right = value) }

    fun onAiPromptChange(value: String) {
        _state.update { it.copy(ai = it.ai.copy(prompt = value, error = null)) }
    }

    fun compare() {
        val snapshot = _state.value
        if (!snapshot.canCompare) return
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, ai = CompareAiState()) }
            val signedIn = isSignedIn()
            val (left, right) = coroutineScope {
                val first = async { loadSide(snapshot.left, signedIn) }
                val second = async { loadSide(snapshot.right, signedIn) }
                first.await() to second.await()
            }
            _state.update {
                it.copy(
                    loading = false,
                    result = CompareResult(left, right, includesContributions = signedIn)
                )
            }
        }
    }

    fun generateAiComparison() {
        val result = _state.value.result ?: return
        val cacheKey = "${result.left.login}:${result.right.login}"
        aiResponseCache[cacheKey]?.let { cached ->
            _state.update { it.copy(ai = it.ai.copy(response = cached)) }
            return
        }
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _state.update { it.copy(ai = it.ai.copy(isLoading = true, response = null, error = null)) }
            val prompt = buildComparisonPrompt(result.left, result.right, result.includesContributions)
            geminiRepository.generate(prompt).fold(
                onSuccess = { response ->
                    aiResponseCache[cacheKey] = response
                    _state.update { it.copy(ai = it.ai.copy(isLoading = false, response = response)) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            ai = it.ai.copy(
                                isLoading = false,
                                error = error as? GeminiError ?: GeminiError.Unknown
                            )
                        )
                    }
                }
            )
        }
    }

    fun askAi() {
        val result = _state.value.result ?: return
        val question = _state.value.ai.prompt.trim()
        if (question.isEmpty() || _state.value.ai.isLoading) return
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _state.update { it.copy(ai = it.ai.copy(isLoading = true, response = null, error = null)) }
            val prompt = buildString {
                appendLine(buildComparisonPrompt(result.left, result.right, result.includesContributions))
                appendLine()
                appendLine("User question regarding this comparison: $question")
            }
            geminiRepository.generate(prompt).fold(
                onSuccess = { response ->
                    _state.update { it.copy(ai = it.ai.copy(isLoading = false, response = response)) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            ai = it.ai.copy(
                                isLoading = false,
                                error = error as? GeminiError ?: GeminiError.Unknown
                            )
                        )
                    }
                }
            )
        }
    }

    private fun buildComparisonPrompt(left: CompareSide, right: CompareSide, includesContributions: Boolean): String {
        val lUser = left.profile?.user
        val rUser = right.profile?.user
        return buildString {
            appendLine("Compare these two GitHub developers in detail based on their public profile data, repositories, stats, and achievements.")
            appendLine("Provide the output strictly separated into two distinct sections starting with:")
            appendLine("DEVELOPER 1: @${left.login}")
            appendLine("and")
            appendLine("DEVELOPER 2: @${right.login}")
            appendLine()
            appendLine("Developer 1 Data (@${left.login}):")
            if (lUser != null) {
                appendLine("- Name: ${lUser.name ?: lUser.login}")
                appendLine("- Bio: ${lUser.bio ?: "N/A"}")
                appendLine("- Company: ${lUser.company ?: "N/A"}, Location: ${lUser.location ?: "N/A"}")
                appendLine("- Followers: ${lUser.followers}, Following: ${lUser.following}")
                appendLine("- Public Repositories: ${lUser.publicRepos}")
                appendLine("- Total Stars: ${left.totalStars ?: 0}")
                appendLine("- Total Forks: ${left.totalForks ?: 0}")
                appendLine("- Open Issues: ${left.totalOpenIssues ?: 0}")
                appendLine("- Average Repo Health Score: ${left.averageHealth ?: "N/A"}")
                if (includesContributions) appendLine("- Total Contributions: ${left.contributions ?: 0}")
                appendLine("- Top Languages: ${left.topLanguages.joinToString(", ").ifBlank { "None" }}")
                left.topStarredRepo?.let {
                    appendLine("- Top Starred Repo: ${it.name} (${it.stargazersCount} stars) - ${it.description ?: ""}")
                }
            } else {
                appendLine("- Profile failed to load.")
            }

            appendLine()
            appendLine("Developer 2 Data (@${right.login}):")
            if (rUser != null) {
                appendLine("- Name: ${rUser.name ?: rUser.login}")
                appendLine("- Bio: ${rUser.bio ?: "N/A"}")
                appendLine("- Company: ${rUser.company ?: "N/A"}, Location: ${rUser.location ?: "N/A"}")
                appendLine("- Followers: ${rUser.followers}, Following: ${rUser.following}")
                appendLine("- Public Repositories: ${rUser.publicRepos}")
                appendLine("- Total Stars: ${right.totalStars ?: 0}")
                appendLine("- Total Forks: ${right.totalForks ?: 0}")
                appendLine("- Open Issues: ${right.totalOpenIssues ?: 0}")
                appendLine("- Average Repo Health Score: ${right.averageHealth ?: "N/A"}")
                if (includesContributions) appendLine("- Total Contributions: ${right.contributions ?: 0}")
                appendLine("- Top Languages: ${right.topLanguages.joinToString(", ").ifBlank { "None" }}")
                right.topStarredRepo?.let {
                    appendLine("- Top Starred Repo: ${it.name} (${it.stargazersCount} stars) - ${it.description ?: ""}")
                }
            } else {
                appendLine("- Profile failed to load.")
            }

            appendLine()
            appendLine("For each developer, detail their technical strengths, language focus, open source impact, and summary. Do not use special markdown symbols like asterisks or hashtags.")
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
