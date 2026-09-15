package com.interviewcoach.app.navigation

import androidx.lifecycle.ViewModel
import com.interviewcoach.core.storage.dao.DailyTaskDao
import com.interviewcoach.core.storage.dao.PlanDao
import com.interviewcoach.core.storage.dao.PositionDao
import com.interviewcoach.core.storage.dao.SettingsDao
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import com.interviewcoach.domain.service.AdjustmentService
import com.interviewcoach.domain.service.MockInterviewService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Shared coordinator for the main shell's cross-cutting sequences — the
 * things that would otherwise be duplicated between DailyTaskScreen's and
 * CardScreen's completion callbacks (mirrors the Flutter plan's
 * `_MainShellState.onTaskCompleted` / "TaskCompletionCoordinator" note).
 */
@HiltViewModel
class MainShellViewModel @Inject constructor(
    private val positionDao: PositionDao,
    private val dailyTaskDao: DailyTaskDao,
    private val planDao: PlanDao,
    private val adjustmentService: AdjustmentService,
    private val mockInterviewService: MockInterviewService,
    private val settingsDao: SettingsDao,
) : ViewModel() {

    suspend fun getKnowledgePoint(positionId: String, knowledgePointId: String): KnowledgePointEntity? =
        positionDao.getKnowledgePointsForPosition(positionId).firstOrNull { it.id == knowledgePointId }

    /** Marks the task done, re-runs plan adjustment, and re-checks the mock-interview unlock. */
    suspend fun completeTask(taskId: String, planId: String, positionId: String): Boolean {
        dailyTaskDao.markCompleted(taskId)
        val avg = dailyTaskDao.recentAvgTasksPerDay(planId)
        adjustmentService.regenerateFutureTasks(planId, positionId, System.currentTimeMillis(), avg)
        return mockInterviewService.checkAndPersistUnlock(planId, positionId, planDao)
    }

    suspend fun weakKnowledgePointIds(): List<String> {
        val raw = settingsDao.getSetting("weak_points_from_last_review") ?: return emptyList()
        return Json.decodeFromString(raw)
    }

    suspend fun startMockInterviewSession(planId: String, positionId: String): String =
        mockInterviewService.startSession(planId, positionId)
}
