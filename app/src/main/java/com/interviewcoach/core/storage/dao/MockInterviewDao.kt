package com.interviewcoach.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.interviewcoach.core.storage.entity.MockInterviewSessionEntity

@Dao
interface MockInterviewDao {
    @Insert suspend fun insertSession(session: MockInterviewSessionEntity)

    @Query("SELECT * FROM mock_interview_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): MockInterviewSessionEntity?

    @Query("UPDATE mock_interview_sessions SET transcriptJson = :transcriptJson WHERE id = :id")
    suspend fun updateTranscript(id: String, transcriptJson: String)

    @Query("UPDATE mock_interview_sessions SET endedAt = :endedAt WHERE id = :id")
    suspend fun markEnded(id: String, endedAt: Long)

    @Query("SELECT * FROM mock_interview_sessions WHERE planId = :planId")
    suspend fun getSessionsForPlan(planId: String): List<MockInterviewSessionEntity>
}
