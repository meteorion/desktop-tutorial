package com.interviewcoach.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.interviewcoach.core.storage.entity.DailyTaskEntity
import java.util.concurrent.TimeUnit

@Dao
interface DailyTaskDao {
    @Insert suspend fun insertTask(task: DailyTaskEntity)

    @Query("SELECT * FROM daily_tasks WHERE planId = :planId")
    suspend fun getTasksForPlan(planId: String): List<DailyTaskEntity>

    @Query("SELECT * FROM daily_tasks WHERE planId = :planId AND date = :dateStartOfDay")
    suspend fun getTasksForDate(planId: String, dateStartOfDay: Long): List<DailyTaskEntity>

    // Deletes not-yet-completed future tasks so AdjustmentService can
    // regenerate them without touching history.
    @Query("DELETE FROM daily_tasks WHERE planId = :planId AND completed = 0 AND date >= :fromDate")
    suspend fun deleteIncompleteFutureTasks(planId: String, fromDate: Long)

    @Query("UPDATE daily_tasks SET completed = 1 WHERE id = :taskId")
    suspend fun markCompleted(taskId: String)

    @Query("SELECT COUNT(*) FROM daily_tasks WHERE planId = :planId AND completed = 1 AND date >= :sinceMillis")
    suspend fun countCompletedSince(planId: String, sinceMillis: Long): Int

    suspend fun recentAvgTasksPerDay(planId: String, lookbackDays: Int = 3): Double {
        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(lookbackDays.toLong())
        return countCompletedSince(planId, since).toDouble() / lookbackDays
    }
}
