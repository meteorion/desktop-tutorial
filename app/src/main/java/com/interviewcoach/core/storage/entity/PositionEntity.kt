package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "positions")
data class PositionEntity(@PrimaryKey val id: String, val name: String)
