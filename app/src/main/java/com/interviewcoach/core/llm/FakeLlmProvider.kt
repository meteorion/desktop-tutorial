package com.interviewcoach.core.llm

import com.interviewcoach.domain.model.AnswerFeedback
import com.interviewcoach.domain.model.DailyTaskDraft
import com.interviewcoach.domain.model.InterviewContext
import com.interviewcoach.domain.model.InterviewTranscript
import com.interviewcoach.domain.model.InterviewTurn
import com.interviewcoach.domain.model.PlanDraft
import com.interviewcoach.domain.model.PlanGenerationInput
import com.interviewcoach.domain.model.QuestionDraft
import com.interviewcoach.domain.model.ResumeParseResult
import com.interviewcoach.domain.model.ReviewReportDraft
import javax.inject.Inject
import kotlin.math.min

class FakeLlmProvider @Inject constructor() : LlmProvider {

    override suspend fun parseResume(resumeText: String) = ResumeParseResult(
        suggestedPositionId = null,
        suggestedSalaryRange = "15K-25K",
        demonstratedSkills = if (resumeText.isEmpty()) emptyList() else listOf("Java 基础"),
        confidence = if (resumeText.length > 50) 0.8 else 0.3,
    )

    override suspend fun generatePlan(input: PlanGenerationInput): PlanDraft {
        val periodDays = 7
        val tasks = (0 until periodDays).associateWith { listOf(DailyTaskDraft(knowledgePointId = "kp1", taskType = "practice")) }
        return PlanDraft(periodDays = periodDays, summary = "根据你的起点,我们安排了 $periodDays 天的学习计划。", tasksByDayIndex = tasks)
    }

    override suspend fun generateQuestion(knowledgePointName: String, isCore: Boolean) = QuestionDraft(
        content = "请说明你对「$knowledgePointName」的理解,并举一个实际例子。",
        referenceAnswer = "$knowledgePointName 的核心要点包括定义、适用场景和常见陷阱。",
    )

    override suspend fun gradeAnswer(questionContent: String, referenceAnswer: String, answerText: String): AnswerFeedback {
        val length = answerText.trim().length
        val score = if (length == 0) 0.0 else min(95.0, 40.0 + length * 1.5)
        return AnswerFeedback(score = score, feedback = if (length == 0) "还没有作答内容。" else "回答有一定思路,可以参考标准答案补充细节。")
    }

    override suspend fun nextInterviewQuestion(context: InterviewContext): InterviewTurn {
        val isFollowUp = context.transcriptSoFar.isNotEmpty() && context.transcriptSoFar.size % 2 == 1
        return InterviewTurn(
            role = "ai",
            text = if (isFollowUp) "能再展开说说细节吗?" else "说说你在这个领域最有代表性的一次实践。",
            isFollowUp = isFollowUp,
        )
    }

    override suspend fun generateReview(transcript: InterviewTranscript): ReviewReportDraft {
        val userTurns = transcript.turns.count { it.role == "user" }
        val overall = min(95.0, 50.0 + userTurns * 5.0)
        return ReviewReportDraft(
            overallScore = overall,
            knowledgeScore = overall,
            expressionScore = overall - 5,
            highlights = "回答思路清晰,能结合实际项目举例。",
            weaknesses = "部分细节展开不够充分。",
            suggestions = "建议针对薄弱知识点做进一步强化。",
            weakKnowledgePointIds = listOf("kp1"),
        )
    }
}
