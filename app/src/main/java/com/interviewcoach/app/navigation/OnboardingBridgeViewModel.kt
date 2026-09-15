package com.interviewcoach.app.navigation

import androidx.lifecycle.ViewModel
import com.interviewcoach.domain.service.PlanService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * A small seam so `OnboardingFlow` (a plain @Composable, not itself a
 * ViewModel) can reach a Hilt-injected PlanService via hiltViewModel().
 */
@HiltViewModel
class OnboardingBridgeViewModel @Inject constructor(val planService: PlanService) : ViewModel()
