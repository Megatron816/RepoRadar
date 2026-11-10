package com.example.githubprofileexplorer.data.repository

import com.example.githubprofileexplorer.domain.ArchitectureAiPrompts
import com.example.githubprofileexplorer.domain.ArchitectureScanResultLike

class ArchitectureAiRepository(private val gemini: GeminiRepository) {
    suspend fun overview(context: ArchitectureScanResultLike): Result<String> =
        gemini.generate(ArchitectureAiPrompts.overview(context))

    suspend fun question(context: ArchitectureScanResultLike, question: String): Result<String> {
        if (question.isBlank()) return Result.failure(IllegalArgumentException("Question is empty"))
        return gemini.generate(ArchitectureAiPrompts.question(context, question))
    }
}
