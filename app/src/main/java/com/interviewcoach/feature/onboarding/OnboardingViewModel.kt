package com.interviewcoach.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.core.llm.LlmProviderRegistry
import com.interviewcoach.core.storage.dao.PositionDao
import com.interviewcoach.core.storage.entity.PositionEntity
import com.interviewcoach.domain.service.ResumeParseOutcome
import com.interviewcoach.domain.service.ResumeParseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val positions: List<PositionEntity> = emptyList(),
    val selectedPositionId: String? = null,
    val salaryRange: String = "15K-25K",
    val parsing: Boolean = false,
    val parseOutcome: ResumeParseOutcome? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val positionDao: PositionDao,
    private val llmProviderRegistry: LlmProviderRegistry,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.value = _state.value.copy(positions = positionDao.getAllPositions()) }
    }

    fun onPositionSelected(positionId: String) { _state.value = _state.value.copy(selectedPositionId = positionId) }

    fun onResumeTextChanged(resumeText: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(parsing = true)
            val outcome = ResumeParseService(llmProviderRegistry.current()).parseResumeText(resumeText)
            _state.value = _state.value.copy(
                parseOutcome = outcome,
                salaryRange = outcome.suggestedSalaryRange ?: _state.value.salaryRange,
                parsing = false,
            )
        }
    }
}
