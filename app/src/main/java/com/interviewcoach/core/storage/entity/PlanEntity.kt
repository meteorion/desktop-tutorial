package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "plans", foreignKeys = [ForeignKey(PositionEntity::class, ["id"], ["positionId"])])
data class PlanEntity(
    @PrimaryKey val id: String,
    val positionId: String,
    val startDate: Long, // epoch millis
    val periodDays: Int,
    // draft | confirmed | active | unlocked_mock_interview
    val status: String,
)
