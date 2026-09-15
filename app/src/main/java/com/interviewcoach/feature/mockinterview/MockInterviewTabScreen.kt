package com.interviewcoach.feature.mockinterview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.theme.AppColors
import com.interviewcoach.core.storage.entity.ReviewReportEntity
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MockInterviewTabScreen(
    planId: String,
    isUnlocked: Boolean, // derived from PlanEntity.status by the caller, never recomputed live here
    onStartNewSession: () -> Unit,
    onGoLearn: () -> Unit,
    onOpenReport: (ReviewReportEntity) -> Unit,
    viewModel: MockInterviewTabViewModel = hiltViewModel(),
) {
    LaunchedEffect(planId, isUnlocked) { if (isUnlocked) viewModel.load(planId) }
    val reports by viewModel.reports.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Scaffold(topBar = { TopAppBar(title = { Text("模拟面试") }) }) { padding ->
        if (!isUnlocked) {
            Column(Modifier.padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Card(border = BorderStroke(1.dp, AppColors.CardBorder)) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Lock, contentDescription = null)
                        Text("模拟面试待解锁")
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onGoLearn) { Text("去学习 →") }
                    }
                }
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            item {
                Button(onClick = onStartNewSession, modifier = Modifier.fillMaxWidth()) { Text("🎤 开始新的模拟面试") }
                Spacer(Modifier.height(16.dp))
                Text("历次报告", style = MaterialTheme.typography.titleMedium)
            }
            items(reports) { report ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text(dateFormat.format(report.createdAt)) },
                        trailingContent = { Text(report.overallScore.toInt().toString()) },
                        modifier = Modifier.clickable { onOpenReport(report) },
                    )
                }
            }
        }
    }
}
