package com.interviewcoach.domain.service

import com.interviewcoach.core.storage.dao.AttemptDao
import com.interviewcoach.core.storage.dao.MasteryDao
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import javax.inject.Inject

class MasteryService @Inject constructor(
    private val attemptDao: AttemptDao,
    private val masteryDao: MasteryDao,
) {
    suspend fun recalculateForKnowledgePoint(knowledgePointId: String, difficultyWeight: Int = 1) {
        val attempts = attemptDao.getAttemptsForKnowledgePoint(knowledgePointId)
        if (attempts.isEmpty()) {
            masteryDao.upsertMastery(knowledgePointId, 0.0)
            return
        }
        val total = attempts.sumOf { it.score * difficultyWeight }
        val weightTotal = attempts.size * difficultyWeight
        masteryDao.upsertMastery(knowledgePointId, total / weightTotal)
    }

    /**
     * Overall mastery = simple average across all knowledge points that have
     * at least one attempt-derived record (product doc §2.9 unlock condition
     * also needs the per-core-point view — see MockInterviewService).
     */
    suspend fun overallMastery(allKnowledgePoints: List<KnowledgePointEntity>): Double {
        if (allKnowledgePoints.isEmpty()) return 0.0
        var sum = 0.0
        for (kp in allKnowledgePoints) sum += masteryDao.getMasteryForKnowledgePoint(kp.id)?.score ?: 0.0
        return sum / allKnowledgePoints.size
    }
}
