package com.interviewcoach.core.storage.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.interviewcoach.core.storage.entity.MasteryRecordEntity

@Dao
interface MasteryDao {
    @Upsert suspend fun upsertMastery(record: MasteryRecordEntity)

    suspend fun upsertMastery(knowledgePointId: String, score: Double) =
        upsertMastery(MasteryRecordEntity(knowledgePointId, score, System.currentTimeMillis()))

    @Query("SELECT * FROM mastery_records WHERE knowledgePointId = :knowledgePointId")
    suspend fun getMasteryForKnowledgePoint(knowledgePointId: String): MasteryRecordEntity?

    @Query("SELECT * FROM mastery_records")
    suspend fun getAllMastery(): List<MasteryRecordEntity>
}
