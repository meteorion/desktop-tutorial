package com.interviewcoach.feature.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.core.storage.dao.DailyTaskDao
import com.interviewcoach.core.storage.entity.DailyTaskEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(private val dailyTaskDao: DailyTaskDao) : ViewModel() {
    private val _tasks = MutableStateFlow<List<DailyTaskEntity>>(emptyList())
    val tasks: StateFlow<List<DailyTaskEntity>> = _tasks.asStateFlow()

    fun load(planId: String) {
        viewModelScope.launch {
            val startOfDay = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            _tasks.value = dailyTaskDao.getTasksForDate(planId, startOfDay)
        }
    }
}
