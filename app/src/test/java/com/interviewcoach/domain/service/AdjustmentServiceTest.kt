package com.interviewcoach.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.loadSeedIfEmpty
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

private const val SEED_JSON = """
[
  {"id": "p1", "name": "后端开发-Java", "knowledgePoints": [
    {"id": "kp-weak-core", "name": "系统设计", "weight": 4, "difficulty": 3, "isCore": true},
    {"id": "kp-mastered", "name": "Java 基础", "weight": 2, "difficulty": 1, "isCore": false}
  ]}
]
"""

@RunWith(RobolectricTestRunner::class)
class AdjustmentServiceTest {
    private lateinit var db: AppDatabase
    private val day0 = TimeUnit.DAYS.toMillis(20000) // arbitrary fixed reference day

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun service() = AdjustmentService(db.dailyTaskDao(), db.masteryDao(), db.positionDao(), db.settingsDao())

    @Test
    fun `mastery signal schedules more tasks for a weak core point than a mastered one`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-weak-core", 30.0)
        db.masteryDao().upsertMastery("kp-mastered", 95.0)
        val service = service()

        service.regenerateFutureTasks(planId = "plan1", positionId = "p1", fromDate = day0, recentAvgTasksPerDay = 3.0)

        val tasks = db.dailyTaskDao().getTasksForPlan("plan1")
        val weakCount = tasks.count { it.knowledgePointId == "kp-weak-core" }
        val masteredCount = tasks.count { it.knowledgePointId == "kp-mastered" }
        assertThat(weakCount).isGreaterThan(masteredCount)
    }

    @Test
    fun `pace signal reduces tasks scheduled per day when recent completion is slow`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-weak-core", 30.0)
        val service = service()

        service.regenerateFutureTasks(planId = "plan-slow", positionId = "p1", fromDate = day0, recentAvgTasksPerDay = 0.5)

        val tasksOnFirstDay = db.dailyTaskDao().getTasksForPlan("plan-slow").count { it.date == day0 }
        assertThat(tasksOnFirstDay).isAtMost(2)
    }

    @Test
    fun `recording too-hard feedback inserts a card warm-up before the next practice task`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-weak-core", 30.0)
        val service = service()

        service.recordDifficultyFeedback(knowledgePointId = "kp-weak-core", tooHard = true)
        service.regenerateFutureTasks(planId = "plan-fb", positionId = "p1", fromDate = day0, recentAvgTasksPerDay = 3.0)

        val tasks = db.dailyTaskDao().getTasksForPlan("plan-fb").sortedBy { it.date }
        val firstWeakCoreTask = tasks.first { it.knowledgePointId == "kp-weak-core" }
        assertThat(firstWeakCoreTask.taskType).isEqualTo("card")
    }
}
