package com.interviewcoach.domain.service

import com.interviewcoach.core.storage.dao.MasteryDao
import com.interviewcoach.core.storage.dao.PositionDao
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import javax.inject.Inject

class FreeLearningService @Inject constructor(
    private val masteryDao: MasteryDao,
    private val positionDao: PositionDao,
) {
    suspend fun selectKnowledgePointToReview(positionId: String, excludeId: String? = null): KnowledgePointEntity? {
        val candidates = positionDao.getKnowledgePointsForPosition(positionId)
            .filter { it.id != excludeId }
            .mapNotNull { kp -> masteryDao.getMasteryForKnowledgePoint(kp.id)?.let { kp to it } }
        if (candidates.isEmpty()) return null

        return candidates.sortedWith(compareBy({ it.second.score }, { it.second.updatedAt })).first().first
    }
}
