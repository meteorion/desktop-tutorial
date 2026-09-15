package com.interviewcoach.feature.learning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.theme.AppColors
import com.interviewcoach.core.storage.entity.DailyTaskEntity

@Composable
fun TaskListScreen(planId: String, onTaskSelected: (DailyTaskEntity) -> Unit, viewModel: TaskListViewModel = hiltViewModel()) {
    LaunchedEffect(planId) { viewModel.load(planId) }
    val tasks by viewModel.tasks.collectAsState()
    val done = tasks.count { it.completed }

    LinearProgressIndicator(
        progress = { if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size },
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = AppColors.Accent,
        trackColor = AppColors.CardBorder,
    )
    LazyColumn {
        items(tasks) { task ->
            ListItem(
                modifier = Modifier.clickable(enabled = !task.completed) { onTaskSelected(task) },
                leadingContent = {
                    if (task.completed) Icon(Icons.Filled.CheckCircle, null, tint = AppColors.MasteryHigh)
                    else Icon(if (task.taskType == "practice") Icons.Outlined.MenuBook else Icons.Outlined.Style, null, tint = AppColors.Accent)
                },
                headlineContent = {
                    Text(
                        task.knowledgePointId,
                        color = if (task.completed) AppColors.TextMuted else Color.Unspecified,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    )
                },
            )
        }
    }
}
