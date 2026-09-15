package com.interviewcoach.feature.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DashboardScreen(positionId: String, viewModel: DashboardViewModel = hiltViewModel()) {
    LaunchedEffect(positionId) { viewModel.load(positionId) }
    val items by viewModel.items.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("首页") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            items(items) { kp ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text(if (kp.isCore) "${kp.name} ⭐核心" else kp.name) },
                        supportingContent = { LinearProgressIndicator(progress = { (kp.score / 100).toFloat() }, modifier = Modifier.fillMaxWidth()) },
                        trailingContent = { Text("${kp.score.toInt()}%") },
                    )
                }
            }
        }
    }
}
