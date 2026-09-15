package com.interviewcoach.core.llm

import com.interviewcoach.domain.model.AnswerFeedback
import com.interviewcoach.domain.model.InterviewContext
import com.interviewcoach.domain.model.InterviewTranscript
import com.interviewcoach.domain.model.InterviewTurn
import com.interviewcoach.domain.model.PlanDraft
import com.interviewcoach.domain.model.PlanGenerationInput
import com.interviewcoach.domain.model.QuestionDraft
import com.interviewcoach.domain.model.ResumeParseResult
import com.interviewcoach.domain.model.ReviewReportDraft

/**
 * Everything a domain service needs from "the LLM", scoped to the operations
 * this app performs — not a generic chat passthrough. See architecture doc §4.
 */
interface LlmProvider {
    suspend fun parseResume(resumeText: String): ResumeParseResult
    suspend fun generatePlan(input: PlanGenerationInput): PlanDraft
    suspend fun generateQuestion(knowledgePointName: String, isCore: Boolean): QuestionDraft
    suspend fun gradeAnswer(questionContent: String, referenceAnswer: String, answerText: String): AnswerFeedback
    suspend fun nextInterviewQuestion(context: InterviewContext): InterviewTurn
    suspend fun generateReview(transcript: InterviewTranscript): ReviewReportDraft
}

class LlmRequestFailure(message: String) : Exception(message)
