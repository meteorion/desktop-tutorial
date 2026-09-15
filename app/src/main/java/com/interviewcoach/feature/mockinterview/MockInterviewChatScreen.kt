package com.interviewcoach.feature.mockinterview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.core.storage.entity.ReviewReportEntity
import kotlinx.coroutines.launch

@Composable
fun MockInterviewChatScreen(
    sessionId: String,
    positionId: String,
    weakKnowledgePointIds: List<String>,
    onSessionEnded: (ReviewReportEntity) -> Unit,
    viewModel: MockInterviewChatViewModel = hiltViewModel(),
) {
    LaunchedEffect(sessionId) { viewModel.start(sessionId, positionId, weakKnowledgePointIds) }
    val state by viewModel.state.collectAsState()
    var answerText by remember { mutableStateOf("") }
    var showEndDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showEndDialog) {
        val userTurnCount = state.turns.count { it.role == "user" }
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("确定要结束吗?") },
            text = { Text("已进行 $userTurnCount 轮,结束后将生成审核报告。") },
            confirmButton = {
                TextButton(onClick = {
                    showEndDialog = false
                    scope.launch { onSessionEnded(viewModel.endSession(sessionId)) }
                }) { Text("结束") }
            },
            dismissButton = { TextButton(onClick = { showEndDialog = false }) { Text("取消") } },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("模拟面试进行中") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(Modifier.weight(1f).padding(14.dp)) {
                items(state.turns) { turn ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (turn.role == "ai") Arrangement.Start else Arrangement.End) {
                        Card {
                            Text(turn.text, modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp))
                        }
                    }
                }
            }
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(value = answerText, onValueChange = { answerText = it }, modifier = Modifier.weight(1f), placeholder = { Text("输入你的回答…") })
                IconButton(onClick = {
                    viewModel.sendAnswer(sessionId, positionId, weakKnowledgePointIds, answerText)
                    answerText = ""
                }) { Icon(Icons.Filled.Send, contentDescription = "发送") }
            }
            TextButton(onClick = { showEndDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("结束面试并生成审核报告") }
        }
    }
}
