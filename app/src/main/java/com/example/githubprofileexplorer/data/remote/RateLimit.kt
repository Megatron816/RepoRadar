package com.example.githubprofileexplorer.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Tracks the most recent `X-RateLimit-Remaining` value GitHub reported. */
object RateLimitMonitor {
    private val _remaining = MutableStateFlow<Int?>(null)
    val remaining: StateFlow<Int?> = _remaining

    fun update(headerValue: String?) {
        headerValue?.toIntOrNull()?.let { _remaining.value = it }
    }
}
