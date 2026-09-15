package com.interviewcoach.app.widgets

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.interviewcoach.app.theme.AppColors

enum class AiActionState { IDLE, LOADING, ERROR }

@Composable
fun AiActionButton(
    state: AiActionState,
    idleLabel: String,
    loadingLabel: String,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    if (state == AiActionState.ERROR) {
        Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = AppColors.ErrorBackground)) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(errorMessage ?: "请求失败,网络似乎有点问题", color = AppColors.ErrorText, modifier = Modifier.weight(1f))
                TextButton(onClick = onRetry) { Text("重试") }
            }
        }
        return
    }

    Button(onClick = onClick, enabled = state != AiActionState.LOADING, modifier = modifier) {
        if (state == AiActionState.LOADING) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(loadingLabel)
        } else {
            Text(idleLabel)
        }
    }
}
