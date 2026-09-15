package com.interviewcoach.feature.freelearning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.widgets.AiActionButton

@Composable
fun FreeLearningScreen(positionId: String, viewModel: FreeLearningViewModel = hiltViewModel()) {
    LaunchedEffect(positionId) { viewModel.pick(positionId) }
    val state by viewModel.state.collectAsState()

    Box(Modifier.fillMaxSize()) {
        when {
            state.loadingPick -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.knowledgePoint == null -> Text(
                "还没有可以回顾的内容,先完成今天的学习任务吧",
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
            else -> {
                val kp = state.knowledgePoint!!
                val question = state.question!!
                Column(Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("🎯 AI 为你选了「${kp.name}」做回顾")
                        TextButton(onClick = { viewModel.pick(positionId, excludeId = kp.id) }) { Text("换一个 ↻") }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(question.content)
                    Spacer(Modifier.height(12.dp))
                    var answerText by remember { mutableStateOf("") }
                    val feedback = state.feedback
                    if (feedback == null) {
                        TextField(value = answerText, onValueChange = { answerText = it }, minLines = 5, modifier = Modifier.fillMaxWidth(), placeholder = { Text("在这里输入你的回答…") })
                        Spacer(Modifier.height(12.dp))
                        AiActionButton(state = state.actionState, idleLabel = "提交回答", loadingLabel = "AI 正在批改…", onClick = { viewModel.submit(answerText) }, onRetry = { viewModel.submit(answerText) })
                    } else {
                        Text("得分:${feedback.score.toInt()}")
                        Text(feedback.feedback)
                    }
                }
            }
        }
    }
}
