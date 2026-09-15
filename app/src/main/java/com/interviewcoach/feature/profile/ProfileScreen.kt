package com.interviewcoach.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.theme.AppColors

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val isConfigured by viewModel.isConfigured.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        var apiKeyInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("配置 LLM API Key") },
            text = { TextField(value = apiKeyInput, onValueChange = { apiKeyInput = it }, visualTransformation = PasswordVisualTransformation(), placeholder = { Text("粘贴你的 API Key") }) },
            confirmButton = { TextButton(onClick = { viewModel.saveApiKey(apiKeyInput); showDialog = false }) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("取消") } },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("我的") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            if (!isConfigured) {
                Card(colors = CardDefaults.cardColors(containerColor = AppColors.WarningBackground), modifier = Modifier.padding(bottom = 14.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("还没配置 AI 服务", color = AppColors.WarningText)
                        Text("配置后才能生成计划、批改作答、进行模拟面试", color = AppColors.WarningText)
                        TextButton(onClick = { showDialog = true }) { Text("立即配置 →") }
                    }
                }
            }
            Text("以下设置项均可点击进入编辑", color = AppColors.TextMuted, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Text("AI 服务", color = AppColors.TextLabel, style = MaterialTheme.typography.titleSmall)
            Card {
                ListItem(
                    headlineContent = { Text("🔑 API Key") },
                    trailingContent = {
                        Text(
                            if (isConfigured) "已配置 ✓" else "未配置",
                            color = if (isConfigured) AppColors.MasteryHigh else AppColors.MasteryLow,
                        )
                    },
                    modifier = Modifier.clickable { showDialog = true },
                )
            }
        }
    }
}
