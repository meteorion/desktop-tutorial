package com.interviewcoach.feature.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.theme.AppColors
import com.interviewcoach.app.widgets.PrimaryButton
import com.interviewcoach.app.widgets.ScreenHeader

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
}

@Composable
fun OnboardingScreen(
    onConfirmed: (positionId: String, salaryRange: String) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(containerColor = AppColors.PageBackground) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader(title = "设定目标", subtitle = "告诉我们你的目标岗位,AI 来定制专属学习计划")

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                var expanded by remember { mutableStateOf(false) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("目标岗位")
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            val selectedName = state.positions.firstOrNull { it.id == state.selectedPositionId }?.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .clickable { expanded = !expanded }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = selectedName ?: "选择你想应聘的岗位",
                                    color = if (selectedName != null) AppColors.TextPrimary else AppColors.TextMuted,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = AppColors.TextMuted)
                            }
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                state.positions.forEach { position ->
                                    DropdownMenuItem(
                                        text = { Text(position.name) },
                                        onClick = { viewModel.onPositionSelected(position.id); expanded = false },
                                    )
                                }
                            }
                        }
                    }
                }

                PrimaryButton(
                    text = "生成面试计划",
                    enabled = state.selectedPositionId != null,
                    onClick = { state.selectedPositionId?.let { onConfirmed(it, state.salaryRange) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
