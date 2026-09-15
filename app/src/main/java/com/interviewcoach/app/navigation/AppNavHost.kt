package com.interviewcoach.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import com.interviewcoach.core.storage.entity.PlanEntity
import com.interviewcoach.core.storage.entity.ReviewReportEntity
import com.interviewcoach.domain.service.PlanService
import com.interviewcoach.feature.dashboard.DashboardScreen
import com.interviewcoach.feature.learning.CardScreen
import com.interviewcoach.feature.learning.DailyTaskScreen
import com.interviewcoach.feature.learning.LearningTabScreen
import com.interviewcoach.feature.mockinterview.MockInterviewChatScreen
import com.interviewcoach.feature.mockinterview.MockInterviewTabScreen
import com.interviewcoach.feature.mockinterview.ReviewReportScreen
import com.interviewcoach.feature.onboarding.OnboardingScreen
import com.interviewcoach.feature.planconfirm.PlanConfirmScreen
import com.interviewcoach.feature.profile.ProfileScreen
import kotlinx.coroutines.launch

@Composable
fun RootShell(rootViewModel: AppRootViewModel = hiltViewModel()) {
    val state by rootViewModel.state.collectAsState()

    when (val s = state) {
        is RootUiState.Loading -> Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        is RootUiState.NeedsOnboarding -> OnboardingFlow(onPlanConfirmed = { rootViewModel.refresh() })
        is RootUiState.MainShell -> MainShellNav(plan = s.plan, onPlanChanged = { rootViewModel.refresh() })
    }
}

@Composable
private fun OnboardingFlow(onPlanConfirmed: () -> Unit) {
    var draftPlan by remember { mutableStateOf<PlanEntity?>(null) }
    val planService: PlanService = hiltViewModel<OnboardingBridgeViewModel>().planService
    val scope = rememberCoroutineScope()

    if (draftPlan == null) {
        OnboardingScreen(onConfirmed = { positionId, _ ->
            scope.launch { draftPlan = planService.generateAndPersistPlan(positionId) }
        })
    } else {
        PlanConfirmScreen(plan = draftPlan!!, onConfirmed = onPlanConfirmed)
    }
}

private enum class MainTab(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "首页", Icons.Outlined.Home),
    Learning("learning", "学习", Icons.Outlined.MenuBook),
    Profile("profile", "我的", Icons.Outlined.Person),
}

/** Resolves a knowledge point by id before rendering its (task/card) destination. */
@Composable
private fun ResolvedKnowledgePoint(
    shellViewModel: MainShellViewModel,
    positionId: String,
    knowledgePointId: String,
    content: @Composable (KnowledgePointEntity) -> Unit,
) {
    var kp by remember(knowledgePointId) { mutableStateOf<KnowledgePointEntity?>(null) }
    LaunchedEffect(knowledgePointId) { kp = shellViewModel.getKnowledgePoint(positionId, knowledgePointId) }
    val currentKp = kp
    if (currentKp != null) {
        content(currentKp)
    } else {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
    }
}

@Composable
private fun MainShellNav(plan: PlanEntity, onPlanChanged: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val shellViewModel: MainShellViewModel = hiltViewModel()
    var selectedReport by remember { mutableStateOf<ReviewReportEntity?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            if (MainTab.entries.any { it.route == currentRoute }) {
                NavigationBar {
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(MainTab.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = MainTab.Dashboard.route, modifier = Modifier.padding(padding)) {
            composable(MainTab.Dashboard.route) {
                DashboardScreen(positionId = plan.positionId) // Task 4 expands this call
            }

            composable(MainTab.Learning.route) {
                LearningTabScreen(
                    planId = plan.id,
                    positionId = plan.positionId,
                    onTaskSelected = { task ->
                        val routeName = if (task.taskType == "card") "card" else "daily_task"
                        navController.navigate("$routeName/${task.id}/${task.knowledgePointId}")
                    },
                )
            }

            composable(MainTab.Profile.route) { ProfileScreen() } // Task 5 expands this call

            composable("mock_interview") {
                MockInterviewTabScreen(
                    planId = plan.id,
                    isUnlocked = plan.status == "unlocked_mock_interview",
                    onBack = { navController.popBackStack() },
                    onStartNewSession = {
                        scope.launch {
                            val sessionId = shellViewModel.startMockInterviewSession(plan.id, plan.positionId)
                            navController.navigate("mock_interview_chat/$sessionId")
                        }
                    },
                    onGoLearn = {
                        navController.navigate(MainTab.Learning.route) { popUpTo(MainTab.Dashboard.route) { saveState = true } }
                    },
                    onOpenReport = { report ->
                        selectedReport = report
                        navController.navigate("review_report")
                    },
                )
            }

            composable("daily_task/{taskId}/{kpId}") { entry ->
                val taskId = entry.arguments?.getString("taskId") ?: return@composable
                val kpId = entry.arguments?.getString("kpId") ?: return@composable
                ResolvedKnowledgePoint(shellViewModel, plan.positionId, kpId) { kp ->
                    DailyTaskScreen(
                        knowledgePointId = kp.id,
                        knowledgePointName = kp.name,
                        isCore = kp.isCore,
                        onTaskCompleted = {
                            scope.launch {
                                val wasUnlocked = plan.status == "unlocked_mock_interview"
                                val nowUnlocked = shellViewModel.completeTask(taskId, plan.id, plan.positionId)
                                if (!wasUnlocked && nowUnlocked) onPlanChanged()
                                navController.popBackStack()
                            }
                        },
                    )
                }
            }

            composable("card/{taskId}/{kpId}") { entry ->
                val taskId = entry.arguments?.getString("taskId") ?: return@composable
                val kpId = entry.arguments?.getString("kpId") ?: return@composable
                ResolvedKnowledgePoint(shellViewModel, plan.positionId, kpId) { kp ->
                    CardScreen(
                        knowledgePointName = kp.name,
                        onDone = {
                            scope.launch {
                                val wasUnlocked = plan.status == "unlocked_mock_interview"
                                val nowUnlocked = shellViewModel.completeTask(taskId, plan.id, plan.positionId)
                                if (!wasUnlocked && nowUnlocked) onPlanChanged()
                                navController.popBackStack()
                            }
                        },
                    )
                }
            }

            composable("mock_interview_chat/{sessionId}") { entry ->
                val sessionId = entry.arguments?.getString("sessionId") ?: return@composable
                var weakIds by remember(sessionId) { mutableStateOf<List<String>?>(null) }
                LaunchedEffect(sessionId) { weakIds = shellViewModel.weakKnowledgePointIds() }
                weakIds?.let { ids ->
                    MockInterviewChatScreen(
                        sessionId = sessionId,
                        positionId = plan.positionId,
                        weakKnowledgePointIds = ids,
                        onSessionEnded = { report ->
                            selectedReport = report
                            navController.navigate("review_report") { popUpTo("mock_interview") }
                        },
                    )
                } ?: Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            }

            composable("review_report") {
                selectedReport?.let { report ->
                    ReviewReportScreen(
                        report = report,
                        previousScore = null,
                        onReinforceWeakPoints = {
                            navController.navigate(MainTab.Learning.route) { popUpTo(MainTab.Dashboard.route) { saveState = true } }
                        },
                    )
                }
            }
        }
    }
}
