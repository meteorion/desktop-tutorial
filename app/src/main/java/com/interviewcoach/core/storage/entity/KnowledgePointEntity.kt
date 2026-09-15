package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "knowledge_points",
    foreignKeys = [ForeignKey(PositionEntity::class, ["id"], ["positionId"])],
    indices = [Index("positionId")],
)
data class KnowledgePointEntity(
    @PrimaryKey val id: String,
    val positionId: String,
    val name: String,
    val weight: Int,
    val difficulty: Int,
    val isCore: Boolean = false,
)
