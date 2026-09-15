package com.interviewcoach.domain.service

import com.interviewcoach.core.llm.LlmProvider
import com.interviewcoach.core.storage.dao.MockInterviewDao
import com.interviewcoach.core.storage.dao.ReviewReportDao
import com.interviewcoach.core.storage.dao.SettingsDao
import com.interviewcoach.core.storage.entity.ReviewReportEntity
import com.interviewcoach.domain.model.InterviewTranscript
import com.interviewcoach.domain.model.InterviewTurn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

class ReviewService @Inject constructor(
    private val llmProvider: LlmProvider,
    private val mockInterviewDao: MockInterviewDao,
    private val reviewReportDao: ReviewReportDao,
    private val settingsDao: SettingsDao,
) {
    private fun backfillTurnScores(turns: List<InterviewTurn>): List<InterviewTurn> {
        val userAnswerLengths = turns.filter { it.role == "user" }.map { it.text.length }
        if (userAnswerLengths.isEmpty()) return turns
        val maxLen = userAnswerLengths.max().coerceAtLeast(1)

        return turns.map { turn ->
            if (turn.role != "user") return@map turn
            val len = turn.text.length
            val score = 40.0 + (len.toDouble() / maxLen) * 55.0
            turn.copy(turnScore = score, turnFeedback = if (len < 20) "回答可以再展开一些细节" else "回答比较完整")
        }
    }

    suspend fun finalizeSession(sessionId: String): ReviewReportEntity {
        val session = mockInterviewDao.getSessionById(sessionId)!!
        val rawTurns = Json.decodeFromString<List<InterviewTurn>>(session.transcriptJson)
        val scoredTurns = backfillTurnScores(rawTurns)
        mockInterviewDao.updateTranscript(sessionId, Json.encodeToString(scoredTurns))
        mockInterviewDao.markEnded(sessionId, System.currentTimeMillis())

        val draft = llmProvider.generateReview(InterviewTranscript(scoredTurns))

        val reportId = UUID.randomUUID().toString()
        val report = ReviewReportEntity(
            id = reportId, sessionId = sessionId, overallScore = draft.overallScore,
            knowledgeScore = draft.knowledgeScore, expressionScore = draft.expressionScore,
            highlights = draft.highlights, weaknesses = draft.weaknesses, suggestions = draft.suggestions,
            weakKnowledgePointIdsJson = Json.encodeToString(draft.weakKnowledgePointIds),
            createdAt = System.currentTimeMillis(),
        )
        reviewReportDao.insertReport(report)

        // Closed loop: weak points feed AdjustmentService's next regeneration via
        // the same settings-backed mechanism as the "too hard" signal.
        settingsDao.setSetting("weak_points_from_last_review", Json.encodeToString(draft.weakKnowledgePointIds))

        return report
    }
}
