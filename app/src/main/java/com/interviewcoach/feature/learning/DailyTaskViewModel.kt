package com.interviewcoach.feature.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.app.widgets.AiActionState
import com.interviewcoach.core.storage.entity.QuestionEntity
import com.interviewcoach.domain.model.AnswerFeedback
import com.interviewcoach.domain.service.LearningSessionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailyTaskUiState(
    val question: QuestionEntity? = null,
    val feedback: AnswerFeedback? = null,
    val actionState: AiActionState = AiActionState.IDLE,
)

@HiltViewModel
class DailyTaskViewModel @Inject constructor(
    private val sessionService: LearningSessionService,
) : ViewModel() {
    private val _state = MutableStateFlow(DailyTaskUiState())
    val state: StateFlow<DailyTaskUiState> = _state.asStateFlow()

    fun load(knowledgePointId: String, knowledgePointName: String, isCore: Boolean) {
        viewModelScope.launch {
            val question = sessionService.getOrCreateQuestion(knowledgePointId, knowledgePointName, isCore)
            _state.value = _state.value.copy(question = question)
        }
    }

    fun submit(knowledgePointId: String, answerText: String) {
        val question = _state.value.question ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(actionState = AiActionState.LOADING)
            try {
                val feedback = sessionService.submitAnswer(question, knowledgePointId, answerText, "daily_task")
                _state.value = _state.value.copy(feedback = feedback, actionState = AiActionState.IDLE)
            } catch (e: Exception) {
                _state.value = _state.value.copy(actionState = AiActionState.ERROR)
            }
        }
    }

    fun flagQuestion() {
        val question = _state.value.question ?: return
        viewModelScope.launch { sessionService.flagQuestion(question.id) }
    }
}
