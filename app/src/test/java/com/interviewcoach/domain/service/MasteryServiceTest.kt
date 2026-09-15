package com.interviewcoach.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.entity.AttemptEntity
import com.interviewcoach.core.storage.entity.QuestionEntity
import com.interviewcoach.core.storage.loadSeedIfEmpty
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

private const val SEED_JSON = """
[
  {"id": "p1", "name": "后端开发-Java", "knowledgePoints": [
    {"id": "kp1", "name": "Java 基础", "weight": 2, "difficulty": 2, "isCore": false}
  ]}
]
"""

@RunWith(RobolectricTestRunner::class)
class MasteryServiceTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `recalculateForKnowledgePoint averages scores weighted by question difficulty`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        val service = MasteryService(db.attemptDao(), db.masteryDao())
        db.questionDao().insertQuestion(QuestionEntity(id = "q1", knowledgePointId = "kp1", content = "c", referenceAnswer = "a"))
        db.attemptDao().insertAttempt(AttemptEntity(UUID.randomUUID().toString(), "q1", "kp1", "answer 1", 80.0, "ok", "daily_task", 0L))
        db.attemptDao().insertAttempt(AttemptEntity(UUID.randomUUID().toString(), "q1", "kp1", "answer 2", 60.0, "ok", "daily_task", 0L))

        service.recalculateForKnowledgePoint("kp1")

        // Both attempts share the same knowledge point difficulty, so this is a plain average.
        assertThat(db.masteryDao().getMasteryForKnowledgePoint("kp1")?.score).isWithin(0.01).of(70.0)

        // Recalculating again must update, not duplicate, the record.
        service.recalculateForKnowledgePoint("kp1")
        assertThat(db.masteryDao().getAllMastery()).hasSize(1)
    }

    @Test
    fun `recalculateForKnowledgePoint with no attempts leaves mastery at 0`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        val service = MasteryService(db.attemptDao(), db.masteryDao())

        service.recalculateForKnowledgePoint("kp1")

        assertThat(db.masteryDao().getMasteryForKnowledgePoint("kp1")?.score).isEqualTo(0.0)
    }
}
