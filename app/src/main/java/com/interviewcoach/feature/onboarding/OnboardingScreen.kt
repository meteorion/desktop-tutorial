package com.interviewcoach.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.theme.AppColors

@Composable
fun OnboardingScreen(
    onConfirmed: (positionId: String, salaryRange: String) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("设定目标") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                TextField(
                    value = state.positions.firstOrNull { it.id == state.selectedPositionId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("目标岗位") },
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    state.positions.forEach { position ->
                        DropdownMenuItem(
                            text = { Text(position.name) },
                            onClick = { viewModel.onPositionSelected(position.id); expanded = false },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            var resumeText by remember { mutableStateOf("") }
            TextField(
                value = resumeText,
                onValueChange = { resumeText = it; viewModel.onResumeTextChanged(it) },
                label = { Text("粘贴简历文本(可选)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            if (state.parsing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (state.parseOutcome?.needsUserConfirmation == true) {
                Card(colors = CardDefaults.cardColors(containerColor = AppColors.WarningBackground)) {
                    Text("以下为推测填写,请确认", color = AppColors.WarningText, modifier = Modifier.padding(12.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { state.selectedPositionId?.let { onConfirmed(it, state.salaryRange) } },
                enabled = state.selectedPositionId != null,
            ) { Text("生成面试计划") }
        }
    }
}
