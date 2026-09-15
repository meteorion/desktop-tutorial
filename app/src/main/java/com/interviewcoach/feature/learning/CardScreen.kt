package com.interviewcoach.feature.learning

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CardScreen(knowledgePointName: String, onDone: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(knowledgePointName) }) }) { padding ->
        Column(Modifier.padding(padding).padding(20.dp)) {
            Card {
                Text(
                    "「$knowledgePointName」核心要点速览:\n\n" +
                        "在正式练习前,先快速回顾这个知识点的定义、常见应用场景，" +
                        "以及面试中最容易被追问的细节，做到心里有数再作答。",
                    modifier = Modifier.padding(16.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("我已了解,继续") }
        }
    }
}
