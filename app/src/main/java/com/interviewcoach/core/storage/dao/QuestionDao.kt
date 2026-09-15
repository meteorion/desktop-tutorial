package com.interviewcoach.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.interviewcoach.core.storage.entity.QuestionEntity

@Dao
interface QuestionDao {
    @Insert suspend fun insertQuestion(question: QuestionEntity)

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: String): QuestionEntity?

    @Query("UPDATE questions SET flagCount = flagCount + 1 WHERE id = :id")
    suspend fun incrementFlagCount(id: String)
}
