package com.interviewcoach.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.interviewcoach.core.storage.entity.AttemptEntity

@Dao
interface AttemptDao {
    @Insert suspend fun insertAttempt(attempt: AttemptEntity)

    @Query("SELECT * FROM attempts WHERE knowledgePointId = :knowledgePointId")
    suspend fun getAttemptsForKnowledgePoint(knowledgePointId: String): List<AttemptEntity>
}
