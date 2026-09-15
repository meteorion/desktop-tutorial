package com.interviewcoach.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.core.storage.dao.MasteryDao
import com.interviewcoach.core.storage.dao.PositionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KnowledgePointMastery(val id: String, val name: String, val isCore: Boolean, val score: Double)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val positionDao: PositionDao,
    private val masteryDao: MasteryDao,
) : ViewModel() {
    private val _items = MutableStateFlow<List<KnowledgePointMastery>>(emptyList())
    val items: StateFlow<List<KnowledgePointMastery>> = _items.asStateFlow()

    fun load(positionId: String) {
        viewModelScope.launch {
            _items.value = positionDao.getKnowledgePointsForPosition(positionId).map { kp ->
                KnowledgePointMastery(kp.id, kp.name, kp.isCore, masteryDao.getMasteryForKnowledgePoint(kp.id)?.score ?: 0.0)
            }
        }
    }
}
