package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "daily_tasks",
    foreignKeys = [
        ForeignKey(PlanEntity::class, ["id"], ["planId"]),
        ForeignKey(KnowledgePointEntity::class, ["id"], ["knowledgePointId"]),
    ],
    indices = [Index("planId"), Index("knowledgePointId")],
)
data class DailyTaskEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val date: Long,
    val knowledgePointId: String,
    val questionId: String? = null,
    // practice | card
    val taskType: String,
    val completed: Boolean = false,
)
