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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException

class ClaudeLlmProvider(
    private val apiKey: String,
    baseUrl: String = "https://api.anthropic.com/",
    client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().addHeader("x-api-key", apiKey).build())
        }.build(),
) : LlmProvider {
    private val json = Json { ignoreUnknownKeys = true }
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ClaudeApi::class.java)

    private suspend fun send(prompt: String, retriesLeft: Int = 1): String {
        return try {
            val response = api.sendMessage(
                ClaudeRequest(model = MODEL, maxTokens = 1024, messages = listOf(ClaudeMessage("user", prompt))),
            )
            response.content.first().text
        } catch (e: IOException) {
            if (retriesLeft > 0) send(prompt, retriesLeft - 1) else throw LlmRequestFailure(e.message ?: "request failed")
        } catch (e: retrofit2.HttpException) {
            if (retriesLeft > 0) send(prompt, retriesLeft - 1) else throw LlmRequestFailure(e.message())
        }
    }

    override suspend fun gradeAnswer(questionContent: String, referenceAnswer: String, answerText: String): AnswerFeedback {
        val prompt = "题目:$questionContent\n参考答案:$referenceAnswer\n用户作答:$answerText\n" +
            """请以 JSON 格式返回 {"score": 0-100 的数字, "feedback": "一句话点评"},不要输出其他内容。"""
        val raw = json.parseToJsonElement(send(prompt)).jsonObject
        return AnswerFeedback(score = raw["score"]!!.jsonPrimitive.double, feedback = raw["feedback"]!!.jsonPrimitive.content)
    }

    override suspend fun parseResume(resumeText: String): ResumeParseResult {
        val prompt = "以下是一份简历文本,请提取推荐目标薪资区间和已展示的技能关键词," +
            """以 JSON 格式返回 {"suggestedSalaryRange": "如 15K-25K", "demonstratedSkills": ["技能1","技能2"], "confidence": 0-1 的数字}。""" +
            "\n\n$resumeText"
        val raw = json.parseToJsonElement(send(prompt)).jsonObject
        return ResumeParseResult(
            suggestedSalaryRange = raw["suggestedSalaryRange"]?.jsonPrimitive?.content,
            demonstratedSkills = raw["demonstratedSkills"]!!.jsonArrayOfStrings(),
            confidence = raw["confidence"]!!.jsonPrimitive.double,
        )
    }

    override suspend fun generatePlan(input: PlanGenerationInput): PlanDraft {
        val prompt = "为岗位 ${input.positionId} 生成一份学习计划,用户已具备的知识点:" +
            "${input.demonstratedSkillKnowledgePointIds.joinToString(",")}。" +
            """以 JSON 格式返回 {"periodDays": 数字, "summary": "一句话说明", "tasksByDayIndex": {"0": [{"knowledgePointId": "...", "taskType": "practice"}], ...}}。"""
        val raw = json.parseToJsonElement(send(prompt)).jsonObject
        val tasks = raw["tasksByDayIndex"]!!.jsonObject.entries.associate { (dayIndex, list) ->
            dayIndex.toInt() to list.jsonArrayOfObjects().map {
                DailyTaskDraft(it["knowledgePointId"]!!.jsonPrimitive.content, it["taskType"]!!.jsonPrimitive.content)
            }
        }
        return PlanDraft(periodDays = raw["periodDays"]!!.jsonPrimitive.content.toInt(), summary = raw["summary"]!!.jsonPrimitive.content, tasksByDayIndex = tasks)
    }

    override suspend fun generateQuestion(knowledgePointName: String, isCore: Boolean): QuestionDraft {
        val prompt = "为知识点「$knowledgePointName」生成一道面试练习题," +
            """以 JSON 格式返回 {"content": "题目", "referenceAnswer": "参考答案要点"}。"""
        val raw = json.parseToJsonElement(send(prompt)).jsonObject
        return QuestionDraft(content = raw["content"]!!.jsonPrimitive.content, referenceAnswer = raw["referenceAnswer"]!!.jsonPrimitive.content)
    }

    override suspend fun nextInterviewQuestion(context: InterviewContext): InterviewTurn {
        val history = context.transcriptSoFar.joinToString("\n") { "${it.role}: ${it.text}" }
        val prompt = "你是面试官,正在面试岗位 ${context.positionId}。" +
            "需要重点考察的薄弱知识点:${context.weakKnowledgePointIds.joinToString(",")}。历史对话:\n$history\n\n" +
            """请给出下一个问题(可以是追问),以 JSON 格式返回 {"text": "问题内容", "isFollowUp": true/false}。"""
        val raw = json.parseToJsonElement(send(prompt)).jsonObject
        return InterviewTurn(role = "ai", text = raw["text"]!!.jsonPrimitive.content, isFollowUp = raw["isFollowUp"]?.jsonPrimitive?.content?.toBoolean() ?: false)
    }

    override suspend fun generateReview(transcript: InterviewTranscript): ReviewReportDraft {
        val history = transcript.turns.joinToString("\n") { "${it.role}: ${it.text}" }
        val prompt = "以下是一场模拟面试的完整对话记录:\n$history\n\n请从专业知识/技能准确度和表达与逻辑结构两个维度评分并给出建议," +
            """以 JSON 格式返回 {"overallScore": 0-100, "knowledgeScore": 0-100, "expressionScore": 0-100, "highlights": "亮点", "weaknesses": "不足", "suggestions": "建议", "weakKnowledgePointIds": ["知识点id", ...]}。"""
        val raw = json.parseToJsonElement(send(prompt)).jsonObject
        return ReviewReportDraft(
            overallScore = raw["overallScore"]!!.jsonPrimitive.double,
            knowledgeScore = raw["knowledgeScore"]!!.jsonPrimitive.double,
            expressionScore = raw["expressionScore"]!!.jsonPrimitive.double,
            highlights = raw["highlights"]!!.jsonPrimitive.content,
            weaknesses = raw["weaknesses"]!!.jsonPrimitive.content,
            suggestions = raw["suggestions"]!!.jsonPrimitive.content,
            weakKnowledgePointIds = raw["weakKnowledgePointIds"]!!.jsonArrayOfStrings(),
        )
    }

    companion object { private const val MODEL = "claude-sonnet-5" }
}
