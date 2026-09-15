package com.interviewcoach.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.interviewcoach.core.storage.entity.ReviewReportEntity

@Dao
interface ReviewReportDao {
    @Insert suspend fun insertReport(report: ReviewReportEntity)

    @Query("SELECT * FROM review_reports WHERE sessionId IN (:sessionIds)")
    suspend fun getReportsForSessions(sessionIds: List<String>): List<ReviewReportEntity>
}
