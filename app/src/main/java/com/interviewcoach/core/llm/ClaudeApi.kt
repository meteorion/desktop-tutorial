package com.interviewcoach.core.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ClaudeApi {
    @Headers("content-type: application/json", "anthropic-version: 2023-06-01")
    @POST("v1/messages")
    suspend fun sendMessage(@Body body: ClaudeRequest): ClaudeResponse
}

@Serializable
data class ClaudeRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<ClaudeMessage>,
)

@Serializable
data class ClaudeMessage(val role: String, val content: String)

@Serializable
data class ClaudeResponse(val content: List<ClaudeContentBlock>)

@Serializable
data class ClaudeContentBlock(val type: String, val text: String)
