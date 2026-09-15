package com.interviewcoach.feature.learning

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interviewcoach.app.theme.AppColors
import com.interviewcoach.core.storage.entity.DailyTaskEntity
import com.interviewcoach.feature.freelearning.FreeLearningScreen

private enum class LearningMode(val label: String) { DailyTasks("每日任务"), FreeLearning("自由学习") }

/**
 * Hosts the two independent learning entry points (product doc §2.4 daily
 * tasks and §2.5 free learning) behind one segmented control, per UI design
 * doc v2 §2: they merge into a single bottom-nav tab, but stay separate
 * screens/ViewModels underneath — free learning must not touch daily-task
 * progress, and vice versa.
 */
@Composable
fun LearningTabScreen(planId: String, positionId: String, onTaskSelected: (DailyTaskEntity) -> Unit) {
    var mode by rememberSaveable { mutableStateOf(LearningMode.DailyTasks) }

    Scaffold(containerColor = AppColors.PageBackground) { padding ->
        Column(Modifier.padding(padding)) {
            Text(
                "学习",
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(horizontal = 16.dp)) {
                LearningMode.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = mode == entry,
                        onClick = { mode = entry },
                        shape = SegmentedButtonDefaults.itemShape(index, LearningMode.entries.size),
                    ) { Text(entry.label) }
                }
            }
            when (mode) {
                LearningMode.DailyTasks -> TaskListScreen(planId = planId, onTaskSelected = onTaskSelected)
                LearningMode.FreeLearning -> FreeLearningScreen(positionId = positionId)
            }
        }
    }
}
