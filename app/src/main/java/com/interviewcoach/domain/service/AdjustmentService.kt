package com.interviewcoach.domain.service

import com.interviewcoach.core.storage.dao.DailyTaskDao
import com.interviewcoach.core.storage.dao.MasteryDao
import com.interviewcoach.core.storage.dao.PositionDao
import com.interviewcoach.core.storage.dao.SettingsDao
import com.interviewcoach.core.storage.entity.DailyTaskEntity
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AdjustmentService @Inject constructor(
    private val dailyTaskDao: DailyTaskDao,
    private val masteryDao: MasteryDao,
    private val positionDao: PositionDao,
    private val settingsDao: SettingsDao,
) {
    private fun difficultyPrefKey(knowledgePointId: String) = "difficulty_pref_$knowledgePointId"

    suspend fun recordDifficultyFeedback(knowledgePointId: String, tooHard: Boolean) =
        settingsDao.setSetting(difficultyPrefKey(knowledgePointId), if (tooHard) "too_hard" else "too_easy")

    /**
     * Mastery signal: how many practice slots a knowledge point gets in the
     * upcoming pool. Low mastery + core gets the most; anything at/above the
     * mastery threshold gets none (already learned, don't keep repeating it).
     */
    private fun masteryWeight(kp: KnowledgePointEntity, score: Double): Int = when {
        score >= 85 -> 0
        score >= 60 -> 1
        else -> if (kp.isCore) 3 else 2
    }

    /**
     * Pace signal: tasks per day, derived from how many the user has actually
     * been completing recently — this only changes quantity, never content.
     */
    private fun tasksPerDayFromPace(recentAvgTasksPerDay: Double): Int = when {
        recentAvgTasksPerDay < 1.0 -> 1
        recentAvgTasksPerDay < 2.0 -> 2
        else -> 3
    }

    suspend fun regenerateFutureTasks(planId: String, positionId: String, fromDate: Long, recentAvgTasksPerDay: Double) {
        dailyTaskDao.deleteIncompleteFutureTasks(planId, fromDate)

        val knowledgePoints = positionDao.getKnowledgePointsForPosition(positionId)
        val pool = mutableListOf<KnowledgePointEntity>()
        for (kp in knowledgePoints) {
            val mastery = masteryDao.getMasteryForKnowledgePoint(kp.id)
            var weight = masteryWeight(kp, mastery?.score ?: 0.0)
            val pref = settingsDao.getSetting(difficultyPrefKey(kp.id))
            if (pref == "too_easy" && weight > 0) weight -= 1
            repeat(weight) { pool.add(kp) }
        }
        if (pool.isEmpty()) return

        val tasksPerDay = tasksPerDayFromPace(recentAvgTasksPerDay)
        val insertedCardFor = mutableSetOf<String>()

        var day = 0
        var i = 0
        while (i < pool.size) {
            val date = fromDate + TimeUnit.DAYS.toMillis(day.toLong())
            var slot = 0
            while (slot < tasksPerDay && i < pool.size) {
                val kp = pool[i]
                val pref = settingsDao.getSetting(difficultyPrefKey(kp.id))
                if (pref == "too_hard" && kp.id !in insertedCardFor) {
                    dailyTaskDao.insertTask(DailyTaskEntity(UUID.randomUUID().toString(), planId, date, kp.id, taskType = "card"))
                    insertedCardFor.add(kp.id)
                } else {
                    dailyTaskDao.insertTask(DailyTaskEntity(UUID.randomUUID().toString(), planId, date, kp.id, taskType = "practice"))
                    i++
                }
                slot++
            }
            day++
        }
    }
}
