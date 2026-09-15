package com.interviewcoach.feature.freelearning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.app.widgets.AiActionState
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import com.interviewcoach.core.storage.entity.QuestionEntity
import com.interviewcoach.domain.model.AnswerFeedback
import com.interviewcoach.domain.service.FreeLearningService
import com.interviewcoach.domain.service.LearningSessionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FreeLearningUiState(
    val loadingPick: Boolean = true,
    val knowledgePoint: KnowledgePointEntity? = null,
    val question: QuestionEntity? = null,
    val feedback: AnswerFeedback? = null,
    val actionState: AiActionState = AiActionState.IDLE,
)

@HiltViewModel
class FreeLearningViewModel @Inject constructor(
    private val freeLearningService: FreeLearningService,
    private val sessionService: LearningSessionService,
) : ViewModel() {
    private val _state = MutableStateFlow(FreeLearningUiState())
    val state: StateFlow<FreeLearningUiState> = _state.asStateFlow()

    fun pick(positionId: String, excludeId: String? = null) {
        viewModelScope.launch {
            _state.value = FreeLearningUiState(loadingPick = true)
            val kp = freeLearningService.selectKnowledgePointToReview(positionId, excludeId)
            if (kp == null) {
                _state.value = FreeLearningUiState(loadingPick = false, knowledgePoint = null)
                return@launch
            }
            val question = sessionService.getOrCreateQuestion(kp.id, kp.name, kp.isCore)
            _state.value = FreeLearningUiState(loadingPick = false, knowledgePoint = kp, question = question)
        }
    }

    fun submit(answerText: String) {
        val kp = _state.value.knowledgePoint ?: return
        val question = _state.value.question ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(actionState = AiActionState.LOADING)
            try {
                val feedback = sessionService.submitAnswer(question, kp.id, answerText, "free_learning")
                _state.value = _state.value.copy(feedback = feedback, actionState = AiActionState.IDLE)
            } catch (e: Exception) {
                _state.value = _state.value.copy(actionState = AiActionState.ERROR)
            }
        }
    }
}
