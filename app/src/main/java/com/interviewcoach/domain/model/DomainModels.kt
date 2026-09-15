package com.interviewcoach.domain.model

import kotlinx.serialization.Serializable

data class ResumeParseResult(
    val suggestedPositionId: String? = null,
    val suggestedSalaryRange: String? = null,
    val demonstratedSkills: List<String>,
    val confidence: Double, // 0.0-1.0; UI shows "以下为推测填写,请确认" below a threshold
)

data class PlanGenerationInput(
    val positionId: String,
    val demonstratedSkillKnowledgePointIds: List<String> = emptyList(),
)

data class DailyTaskDraft(val knowledgePointId: String, val taskType: String) // practice | card

data class PlanDraft(
    val periodDays: Int,
    val summary: String,
    val tasksByDayIndex: Map<Int, List<DailyTaskDraft>>, // 0-based day index
)

data class QuestionDraft(val content: String, val referenceAnswer: String)

data class AnswerFeedback(val score: Double, val feedback: String) // score 0-100

@Serializable
data class InterviewTurn(
    val role: String, // ai | user
    val text: String,
    val isFollowUp: Boolean = false,
    val turnScore: Double? = null,
    val turnFeedback: String? = null,
)

data class InterviewContext(
    val positionId: String,
    val weakKnowledgePointIds: List<String>,
    val transcriptSoFar: List<InterviewTurn>,
)

data class InterviewTranscript(val turns: List<InterviewTurn>)

data class ReviewReportDraft(
    val overallScore: Double,
    val knowledgeScore: Double,
    val expressionScore: Double,
    val highlights: String,
    val weaknesses: String,
    val suggestions: String,
    val weakKnowledgePointIds: List<String>,
)
