package com.interviewcoach.core.storage.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.entity.DailyTaskEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DailyTaskDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `recentAvgTasksPerDay averages completed tasks over the lookback window`() = runTest {
        val today = System.currentTimeMillis()
        repeat(3) { i ->
            db.dailyTaskDao().insertTask(DailyTaskEntity("completed-$i", "plan1", today, "kp1", taskType = "practice", completed = true))
        }
        db.dailyTaskDao().insertTask(DailyTaskEntity("not-completed", "plan1", today, "kp1", taskType = "practice"))

        val avg = db.dailyTaskDao().recentAvgTasksPerDay("plan1", lookbackDays = 3)

        assertThat(avg).isWithin(0.01).of(1.0) // 3 completed / 3-day window
    }
}
