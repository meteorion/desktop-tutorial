package com.interviewcoach.feature.mockinterview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.core.storage.dao.MockInterviewDao
import com.interviewcoach.core.storage.dao.ReviewReportDao
import com.interviewcoach.core.storage.entity.ReviewReportEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MockInterviewTabViewModel @Inject constructor(
    private val mockInterviewDao: MockInterviewDao,
    private val reviewReportDao: ReviewReportDao,
) : ViewModel() {
    private val _reports = MutableStateFlow<List<ReviewReportEntity>>(emptyList())
    val reports: StateFlow<List<ReviewReportEntity>> = _reports.asStateFlow()

    fun load(planId: String) {
        viewModelScope.launch {
            val sessionIds = mockInterviewDao.getSessionsForPlan(planId).map { it.id }
            _reports.value = reviewReportDao.getReportsForSessions(sessionIds).sortedByDescending { it.createdAt }
        }
    }
}
