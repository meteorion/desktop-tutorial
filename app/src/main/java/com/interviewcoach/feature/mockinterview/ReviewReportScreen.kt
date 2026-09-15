package com.interviewcoach.feature.mockinterview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interviewcoach.core.storage.entity.ReviewReportEntity

@Composable
fun ReviewReportScreen(report: ReviewReportEntity, previousScore: Double?, onReinforceWeakPoints: () -> Unit) {
    val delta = previousScore?.let { report.overallScore - it }

    Scaffold(topBar = { TopAppBar(title = { Text("本次模拟面试报告") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text(report.overallScore.toInt().toString(), fontSize = 40.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            delta?.let { Text("比上次 ${if (it >= 0) "+" else ""}${it.toInt()}", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            Spacer(Modifier.height(16.dp))
            Text("专业知识/技能准确度:${report.knowledgeScore.toInt()}")
            Text("表达与逻辑结构:${report.expressionScore.toInt()}")
            Text("非语言信号(语调/自信度):敬请期待")
            Spacer(Modifier.height(16.dp))
            Text("✅ 亮点\n${report.highlights}")
            Spacer(Modifier.height(8.dp))
            Text("⚠️ 不足\n${report.weaknesses}")
            Spacer(Modifier.height(8.dp))
            Text("💡 建议\n${report.suggestions}")
            Spacer(Modifier.weight(1f))
            Button(onClick = onReinforceWeakPoints, modifier = Modifier.fillMaxWidth()) { Text("去强化薄弱知识点 →") }
        }
    }
}
