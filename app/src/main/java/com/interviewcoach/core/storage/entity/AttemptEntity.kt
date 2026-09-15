package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "attempts",
    foreignKeys = [
        ForeignKey(QuestionEntity::class, ["id"], ["questionId"]),
        ForeignKey(KnowledgePointEntity::class, ["id"], ["knowledgePointId"]),
    ],
)
data class AttemptEntity(
    @PrimaryKey val id: String,
    val questionId: String,
    val knowledgePointId: String,
    val answerText: String,
    val score: Double,
    val feedback: String,
    // daily_task | free_learning
    val source: String,
    val createdAt: Long,
)
