package com.interviewcoach.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.llm.FakeLlmProvider
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.loadSeedIfEmpty
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val SEED_JSON = """
[
  {"id": "p1", "name": "后端开发-Java", "knowledgePoints": [
    {"id": "kp1", "name": "Java 基础", "weight": 2, "difficulty": 1, "isCore": false}
  ]}
]
"""

@RunWith(RobolectricTestRunner::class)
class LearningSessionServiceTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun service() = LearningSessionService(
        FakeLlmProvider(), db.questionDao(), db.attemptDao(),
        MasteryService(db.attemptDao(), db.masteryDao()),
    )

    @Test
    fun `submitAnswer generates a question, records an attempt, and updates mastery`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        val service = service()

        val question = service.getOrCreateQuestion(knowledgePointId = "kp1", knowledgePointName = "Java 基础", isCore = false)
        val feedback = service.submitAnswer(
            question = question, knowledgePointId = "kp1",
            answerText = "a detailed answer about generics and collections", source = "daily_task",
        )

        assertThat(feedback.score).isGreaterThan(0.0)
        assertThat(db.attemptDao().getAttemptsForKnowledgePoint("kp1")).hasSize(1)
        assertThat(db.masteryDao().getMasteryForKnowledgePoint("kp1")?.score).isEqualTo(feedback.score)
    }

    @Test
    fun `flagQuestion increments the flag count`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        val service = service()
        val question = service.getOrCreateQuestion(knowledgePointId = "kp1", knowledgePointName = "Java 基础", isCore = false)

        service.flagQuestion(question.id)

        assertThat(db.questionDao().getQuestionById(question.id)?.flagCount).isEqualTo(1)
    }
}
