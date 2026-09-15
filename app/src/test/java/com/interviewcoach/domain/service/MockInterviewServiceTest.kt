package com.interviewcoach.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.llm.FakeLlmProvider
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.entity.PlanEntity
import com.interviewcoach.core.storage.loadSeedIfEmpty
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val SEED_JSON = """
[
  {"id": "p1", "name": "后端开发-Java", "knowledgePoints": [
    {"id": "kp-core", "name": "系统设计", "weight": 4, "difficulty": 3, "isCore": true},
    {"id": "kp-normal", "name": "Java 基础", "weight": 2, "difficulty": 1, "isCore": false}
  ]}
]
"""

@RunWith(RobolectricTestRunner::class)
class MockInterviewServiceTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun service() = MockInterviewService(db.masteryDao(), db.positionDao(), db.mockInterviewDao(), FakeLlmProvider())

    @Test
    fun `locked when the core point is below 70 percent even if the average looks fine`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-core", 50.0) // below core threshold
        db.masteryDao().upsertMastery("kp-normal", 100.0) // average = 75, still fails overall too

        assertThat(service().isUnlocked("p1")).isFalse()
    }

    @Test
    fun `unlocked only when core points clear 70 percent and the overall average clears 80 percent`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-core", 75.0)
        db.masteryDao().upsertMastery("kp-normal", 90.0)

        assertThat(service().isUnlocked("p1")).isTrue()
    }

    @Test
    fun `checkAndPersistUnlock persists the unlock and stays unlocked after mastery drops`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-core", 75.0)
        db.masteryDao().upsertMastery("kp-normal", 90.0)
        db.planDao().insertPlan(PlanEntity(id = "plan-unlock", positionId = "p1", startDate = 0L, periodDays = 7, status = "confirmed"))
        val service = service()

        val unlockedNow = service.checkAndPersistUnlock(planId = "plan-unlock", positionId = "p1", planDao = db.planDao())
        assertThat(unlockedNow).isTrue()
        assertThat(db.planDao().getPlanById("plan-unlock")?.status).isEqualTo("unlocked_mock_interview")

        // Mastery drops back below threshold — must stay unlocked (product doc §2.9).
        db.masteryDao().upsertMastery("kp-core", 20.0)
        val stillUnlocked = service.checkAndPersistUnlock(planId = "plan-unlock", positionId = "p1", planDao = db.planDao())
        assertThat(stillUnlocked).isTrue()
    }

    @Test
    fun `startSession then askNextQuestion then recordUserAnswer builds a persisted transcript`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        val service = service()

        val sessionId = service.startSession(planId = "plan1", positionId = "p1")
        val firstQuestion = service.askNextQuestion(sessionId = sessionId, positionId = "p1", weakKnowledgePointIds = listOf("kp-core"))
        service.recordUserAnswer(sessionId = sessionId, answerText = "my answer")

        val session = db.mockInterviewDao().getSessionById(sessionId)
        val transcript = Json.decodeFromString<List<com.interviewcoach.domain.model.InterviewTurn>>(session!!.transcriptJson)
        assertThat(transcript).hasSize(2)
        assertThat(transcript.first().role).isEqualTo("ai")
        assertThat(transcript.first().text).isEqualTo(firstQuestion.text)
        assertThat(transcript.last().role).isEqualTo("user")
        assertThat(transcript.last().text).isEqualTo("my answer")
    }
}
