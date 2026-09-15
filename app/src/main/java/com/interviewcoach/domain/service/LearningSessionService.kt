package com.interviewcoach.domain.service

import com.interviewcoach.core.llm.LlmProvider
import com.interviewcoach.core.storage.dao.AttemptDao
import com.interviewcoach.core.storage.dao.QuestionDao
import com.interviewcoach.core.storage.entity.AttemptEntity
import com.interviewcoach.core.storage.entity.QuestionEntity
import com.interviewcoach.domain.model.AnswerFeedback
import java.util.UUID
import javax.inject.Inject

class LearningSessionService @Inject constructor(
    private val llmProvider: LlmProvider,
    private val questionDao: QuestionDao,
    private val attemptDao: AttemptDao,
    private val masteryService: MasteryService,
) {
    suspend fun getOrCreateQuestion(knowledgePointId: String, knowledgePointName: String, isCore: Boolean): QuestionEntity {
        val draft = llmProvider.generateQuestion(knowledgePointName, isCore)
        val id = UUID.randomUUID().toString()
        questionDao.insertQuestion(QuestionEntity(id = id, knowledgePointId = knowledgePointId, content = draft.content, referenceAnswer = draft.referenceAnswer))
        return questionDao.getQuestionById(id)!!
    }

    suspend fun submitAnswer(
        question: QuestionEntity,
        knowledgePointId: String,
        answerText: String,
        source: String, // daily_task | free_learning
    ): AnswerFeedback {
        val feedback = llmProvider.gradeAnswer(question.content, question.referenceAnswer, answerText)
        attemptDao.insertAttempt(
            AttemptEntity(
                id = UUID.randomUUID().toString(), questionId = question.id, knowledgePointId = knowledgePointId,
                answerText = answerText, score = feedback.score, feedback = feedback.feedback, source = source,
                createdAt = System.currentTimeMillis(),
            ),
        )
        masteryService.recalculateForKnowledgePoint(knowledgePointId)
        return feedback
    }

    suspend fun flagQuestion(questionId: String) = questionDao.incrementFlagCount(questionId)
}
