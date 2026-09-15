package com.interviewcoach.feature.learning

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.widgets.AiActionButton

@Composable
fun DailyTaskScreen(
    knowledgePointId: String,
    knowledgePointName: String,
    isCore: Boolean,
    onTaskCompleted: () -> Unit,
    viewModel: DailyTaskViewModel = hiltViewModel(),
) {
    LaunchedEffect(knowledgePointId) { viewModel.load(knowledgePointId, knowledgePointName, isCore) }
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text(knowledgePointName) }) }) { padding ->
        val question = state.question
        if (question == null) {
            Box(Modifier.fillMaxSize().padding(padding)) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            return@Scaffold
        }
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text(question.content)
            Spacer(Modifier.height(12.dp))
            var answerText by remember { mutableStateOf("") }
            val feedback = state.feedback
            if (feedback == null) {
                TextField(value = answerText, onValueChange = { answerText = it }, minLines = 6, modifier = Modifier.fillMaxWidth(), placeholder = { Text("在这里输入你的回答…") })
                Spacer(Modifier.height(12.dp))
                AiActionButton(
                    state = state.actionState, idleLabel = "提交作答", loadingLabel = "AI 正在批改…",
                    onClick = { viewModel.submit(knowledgePointId, answerText) },
                    onRetry = { viewModel.submit(knowledgePointId, answerText) },
                )
            } else {
                Text("得分:${feedback.score.toInt()}")
                Text(feedback.feedback)
                Text("参考答案:${question.referenceAnswer}")
                TextButton(onClick = { viewModel.flagQuestion() }) { Text("🚩 这道题/答案有问题?") }
                Button(onClick = onTaskCompleted) { Text("下一题 →") }
            }
        }
    }
}
