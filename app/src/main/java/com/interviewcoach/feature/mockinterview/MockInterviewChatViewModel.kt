package com.interviewcoach.feature.mockinterview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.app.widgets.AiActionState
import com.interviewcoach.core.storage.entity.ReviewReportEntity
import com.interviewcoach.domain.model.InterviewTurn
import com.interviewcoach.domain.service.MockInterviewService
import com.interviewcoach.domain.service.ReviewService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(val turns: List<InterviewTurn> = emptyList(), val actionState: AiActionState = AiActionState.IDLE)

@HiltViewModel
class MockInterviewChatViewModel @Inject constructor(
    private val mockInterviewService: MockInterviewService,
    private val reviewService: ReviewService,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun start(sessionId: String, positionId: String, weakKnowledgePointIds: List<String>) = askNext(sessionId, positionId, weakKnowledgePointIds)

    private fun askNext(sessionId: String, positionId: String, weakKnowledgePointIds: List<String>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionState = AiActionState.LOADING)
            try {
                val turn = mockInterviewService.askNextQuestion(sessionId, positionId, weakKnowledgePointIds)
                _state.value = _state.value.copy(turns = _state.value.turns + turn, actionState = AiActionState.IDLE)
            } catch (e: Exception) {
                _state.value = _state.value.copy(actionState = AiActionState.ERROR)
            }
        }
    }

    fun sendAnswer(sessionId: String, positionId: String, weakKnowledgePointIds: List<String>, text: String) {
        if (text.isEmpty()) return
        _state.value = _state.value.copy(turns = _state.value.turns + InterviewTurn(role = "user", text = text))
        viewModelScope.launch {
            mockInterviewService.recordUserAnswer(sessionId, text)
            askNext(sessionId, positionId, weakKnowledgePointIds)
        }
    }

    suspend fun endSession(sessionId: String): ReviewReportEntity = reviewService.finalizeSession(sessionId)
}
