package com.interviewcoach.domain.service

import com.interviewcoach.core.llm.LlmProvider
import com.interviewcoach.core.storage.dao.DailyTaskDao
import com.interviewcoach.core.storage.dao.PlanDao
import com.interviewcoach.core.storage.entity.DailyTaskEntity
import com.interviewcoach.core.storage.entity.PlanEntity
import com.interviewcoach.domain.model.PlanGenerationInput
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PlanService @Inject constructor(
    private val llmProvider: LlmProvider,
    private val planDao: PlanDao,
    private val dailyTaskDao: DailyTaskDao,
) {
    suspend fun generateAndPersistPlan(
        positionId: String,
        demonstratedSkillKnowledgePointIds: List<String> = emptyList(),
    ): PlanEntity {
        val draft = llmProvider.generatePlan(PlanGenerationInput(positionId, demonstratedSkillKnowledgePointIds))

        val planId = UUID.randomUUID().toString()
        val startDate = System.currentTimeMillis()
        planDao.insertPlan(PlanEntity(id = planId, positionId = positionId, startDate = startDate, periodDays = draft.periodDays, status = "draft"))

        for ((dayIndex, taskDrafts) in draft.tasksByDayIndex) {
            val date = startDate + TimeUnit.DAYS.toMillis(dayIndex.toLong())
            for (taskDraft in taskDrafts) {
                dailyTaskDao.insertTask(
                    DailyTaskEntity(
                        id = UUID.randomUUID().toString(),
                        planId = planId,
                        date = date,
                        knowledgePointId = taskDraft.knowledgePointId,
                        taskType = taskDraft.taskType,
                    ),
                )
            }
        }
        return planDao.getPlanById(planId)!!
    }

    suspend fun confirmPlan(planId: String) = planDao.updateStatus(planId, "confirmed")
}
