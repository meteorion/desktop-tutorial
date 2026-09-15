package com.interviewcoach.feature.planconfirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.core.storage.dao.DailyTaskDao
import com.interviewcoach.core.storage.entity.DailyTaskEntity
import com.interviewcoach.domain.service.PlanService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanConfirmUiState(val tasks: List<DailyTaskEntity> = emptyList(), val loading: Boolean = true)

@HiltViewModel
class PlanConfirmViewModel @Inject constructor(
    private val planService: PlanService,
    private val dailyTaskDao: DailyTaskDao,
) : ViewModel() {
    private val _state = MutableStateFlow(PlanConfirmUiState())
    val state: StateFlow<PlanConfirmUiState> = _state.asStateFlow()

    fun load(planId: String) {
        viewModelScope.launch { _state.value = PlanConfirmUiState(tasks = dailyTaskDao.getTasksForPlan(planId), loading = false) }
    }

    fun confirm(planId: String, onConfirmed: () -> Unit) {
        viewModelScope.launch { planService.confirmPlan(planId); onConfirmed() }
    }
}
