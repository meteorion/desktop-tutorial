package com.interviewcoach.feature.planconfirm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.core.storage.entity.PlanEntity

@Composable
fun PlanConfirmScreen(plan: PlanEntity, onConfirmed: () -> Unit, viewModel: PlanConfirmViewModel = hiltViewModel()) {
    LaunchedEffect(plan.id) { viewModel.load(plan.id) }
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("确认学习计划") }) }) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding)) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            return@Scaffold
        }
        val byKnowledgePoint = state.tasks.groupingBy { it.knowledgePointId }.eachCount()
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text("共 ${plan.periodDays} 天,${state.tasks.size} 个任务", modifier = Modifier.padding(16.dp))
            LazyColumn(Modifier.weight(1f, fill = true)) {
                items(byKnowledgePoint.entries.toList()) { (kpId, count) ->
                    ListItem(headlineContent = { Text(kpId) }, trailingContent = { Text("$count 次") })
                }
            }
            Button(onClick = { viewModel.confirm(plan.id, onConfirmed) }, modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("确认计划")
            }
        }
    }
}
