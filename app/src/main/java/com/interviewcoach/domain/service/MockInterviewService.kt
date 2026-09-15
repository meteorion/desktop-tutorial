package com.interviewcoach.domain.service

import com.interviewcoach.core.llm.LlmProvider
import com.interviewcoach.core.storage.dao.MasteryDao
import com.interviewcoach.core.storage.dao.MockInterviewDao
import com.interviewcoach.core.storage.dao.PlanDao
import com.interviewcoach.core.storage.dao.PositionDao
import com.interviewcoach.core.storage.entity.MockInterviewSessionEntity
import com.interviewcoach.domain.model.InterviewContext
import com.interviewcoach.domain.model.InterviewTurn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

class MockInterviewService @Inject constructor(
    private val masteryDao: MasteryDao,
    private val positionDao: PositionDao,
    private val mockInterviewDao: MockInterviewDao,
    private val llmProvider: LlmProvider,
) {
    suspend fun isUnlocked(positionId: String): Boolean {
        val kps = positionDao.getKnowledgePointsForPosition(positionId)
        if (kps.isEmpty()) return false
        var sum = 0.0
        for (kp in kps) {
            val score = masteryDao.getMasteryForKnowledgePoint(kp.id)?.score ?: 0.0
            sum += score
            if (kp.isCore && score < CORE_THRESHOLD) return false
        }
        return (sum / kps.size) >= OVERALL_THRESHOLD
    }

    /**
     * Unlock is permanent once earned (product doc §2.9: mastery dropping
     * later must NOT re-lock the tab), so the result is written onto
     * PlanEntity.status instead of being recomputed live on every visit. Call
     * this after mastery changes (task completion, free-learning submission);
     * the tab itself should read `plan.status`, not call `isUnlocked` again.
     */
    suspend fun checkAndPersistUnlock(planId: String, positionId: String, planDao: PlanDao): Boolean {
        val plan = planDao.getPlanById(planId)
        if (plan?.status == "unlocked_mock_interview") return true
        val unlocked = isUnlocked(positionId)
        if (unlocked) planDao.updateStatus(planId, "unlocked_mock_interview")
        return unlocked
    }

    suspend fun startSession(planId: String, positionId: String): String {
        val id = UUID.randomUUID().toString()
        mockInterviewDao.insertSession(MockInterviewSessionEntity(id = id, planId = planId, startedAt = System.currentTimeMillis(), transcriptJson = "[]"))
        return id
    }

    private suspend fun loadTranscript(sessionId: String): List<InterviewTurn> {
        val session = mockInterviewDao.getSessionById(sessionId)!!
        return Json.decodeFromString(session.transcriptJson)
    }

    private suspend fun saveTranscript(sessionId: String, turns: List<InterviewTurn>) =
        mockInterviewDao.updateTranscript(sessionId, Json.encodeToString(turns))

    suspend fun askNextQuestion(sessionId: String, positionId: String, weakKnowledgePointIds: List<String>): InterviewTurn {
        val soFar = loadTranscript(sessionId)
        val turn = llmProvider.nextInterviewQuestion(InterviewContext(positionId, weakKnowledgePointIds, soFar))
        saveTranscript(sessionId, soFar + turn)
        return turn
    }

    suspend fun recordUserAnswer(sessionId: String, answerText: String) {
        val soFar = loadTranscript(sessionId)
        saveTranscript(sessionId, soFar + InterviewTurn(role = "user", text = answerText))
    }

    suspend fun getTranscript(sessionId: String): List<InterviewTurn> = loadTranscript(sessionId)

    companion object {
        const val CORE_THRESHOLD = 70.0
        const val OVERALL_THRESHOLD = 80.0
    }
}
