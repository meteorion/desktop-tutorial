package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "review_reports", foreignKeys = [ForeignKey(MockInterviewSessionEntity::class, ["id"], ["sessionId"])])
data class ReviewReportEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val overallScore: Double,
    val knowledgeScore: Double,
    val expressionScore: Double,
    val highlights: String,
    val weaknesses: String,
    val suggestions: String,
    val weakKnowledgePointIdsJson: String,
    val createdAt: Long,
)
