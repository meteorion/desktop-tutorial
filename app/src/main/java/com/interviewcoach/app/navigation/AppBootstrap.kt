package com.interviewcoach.app.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.dao.PlanDao
import com.interviewcoach.core.storage.entity.PlanEntity
import com.interviewcoach.core.storage.loadSeedIfEmpty
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RootUiState {
    data object Loading : RootUiState
    data object NeedsOnboarding : RootUiState
    data class MainShell(val plan: PlanEntity) : RootUiState
}

/**
 * The plan the user is currently working (single in-progress plan for MVP,
 * per architecture doc's "MVP 限制单一进行中岗位计划"). NeedsOnboarding means
 * the user hasn't confirmed a plan yet.
 */
@HiltViewModel
class AppRootViewModel @Inject constructor(
    private val db: AppDatabase,
    private val planDao: PlanDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow<RootUiState>(RootUiState.Loading)
    val state: StateFlow<RootUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val seedJson = context.assets.open("positions_seed.json").bufferedReader().use { it.readText() }
            loadSeedIfEmpty(db, seedJson)
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val activePlan = planDao.getAllPlans().firstOrNull { it.status != "draft" }
            _state.value = if (activePlan == null) RootUiState.NeedsOnboarding else RootUiState.MainShell(activePlan)
        }
    }
}
