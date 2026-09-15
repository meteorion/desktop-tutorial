package com.interviewcoach.domain.service

import com.interviewcoach.core.llm.LlmProvider
import javax.inject.Inject

data class ResumeParseOutcome(
    val suggestedSalaryRange: String?,
    val demonstratedSkills: List<String>,
    val confidence: Double,
    val needsUserConfirmation: Boolean,
)

class ResumeParseService @Inject constructor(private val llmProvider: LlmProvider) {
    suspend fun parseResumeText(resumeText: String): ResumeParseOutcome {
        val result = llmProvider.parseResume(resumeText)
        return ResumeParseOutcome(
            suggestedSalaryRange = result.suggestedSalaryRange,
            demonstratedSkills = result.demonstratedSkills,
            confidence = result.confidence,
            needsUserConfirmation = result.confidence < CONFIRMATION_THRESHOLD,
        )
    }

    companion object { private const val CONFIRMATION_THRESHOLD = 0.5 }
}
