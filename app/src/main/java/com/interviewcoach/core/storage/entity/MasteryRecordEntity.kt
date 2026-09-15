package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "mastery_records")
data class MasteryRecordEntity(
    @PrimaryKey val knowledgePointId: String,
    val score: Double = 0.0,
    val updatedAt: Long,
)
