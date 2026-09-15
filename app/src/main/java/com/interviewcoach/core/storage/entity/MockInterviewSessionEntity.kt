package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "mock_interview_sessions", foreignKeys = [ForeignKey(PlanEntity::class, ["id"], ["planId"])])
data class MockInterviewSessionEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    // JSON-encoded list of {role, text, isFollowUp, turnScore, turnFeedback}
    val transcriptJson: String,
)
