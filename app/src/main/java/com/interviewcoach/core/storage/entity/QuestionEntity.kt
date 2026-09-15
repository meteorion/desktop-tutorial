package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "questions", foreignKeys = [ForeignKey(KnowledgePointEntity::class, ["id"], ["knowledgePointId"])])
data class QuestionEntity(
    @PrimaryKey val id: String,
    val knowledgePointId: String,
    val content: String,
    val referenceAnswer: String,
    val flagCount: Int = 0,
)
