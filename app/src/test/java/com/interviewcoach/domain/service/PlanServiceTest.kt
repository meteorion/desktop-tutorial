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
class PlanServiceTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `generateAndPersistPlan creates a draft plan with daily tasks for every day`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        val service = PlanService(FakeLlmProvider(), db.planDao(), db.dailyTaskDao())

        val plan = service.generateAndPersistPlan(positionId = "p1")

        assertThat(plan.status).isEqualTo("draft")
        val tasks = db.dailyTaskDao().getTasksForPlan(plan.id)
        assertThat(tasks).isNotEmpty()
        assertThat(tasks.all { it.planId == plan.id }).isTrue()
    }

    @Test
    fun `confirmPlan transitions status from draft to confirmed`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        val service = PlanService(FakeLlmProvider(), db.planDao(), db.dailyTaskDao())
        val plan = service.generateAndPersistPlan(positionId = "p1")

        service.confirmPlan(plan.id)

        assertThat(db.planDao().getPlanById(plan.id)?.status).isEqualTo("confirmed")
    }
}
