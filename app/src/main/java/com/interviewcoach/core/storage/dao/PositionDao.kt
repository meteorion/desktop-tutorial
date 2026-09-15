package com.interviewcoach.core.storage.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import com.interviewcoach.core.storage.entity.PositionEntity

@Dao
interface PositionDao {
    @Upsert suspend fun upsertPosition(position: PositionEntity)
    @Upsert suspend fun upsertKnowledgePoint(kp: KnowledgePointEntity)

    @Query("SELECT * FROM positions")
    suspend fun getAllPositions(): List<PositionEntity>

    @Query("SELECT * FROM knowledge_points WHERE positionId = :positionId")
    suspend fun getKnowledgePointsForPosition(positionId: String): List<KnowledgePointEntity>

    @Query("SELECT * FROM knowledge_points")
    suspend fun getAllKnowledgePoints(): List<KnowledgePointEntity>
}
