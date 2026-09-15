package com.interviewcoach.app.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interviewcoach.app.theme.AppColors

/**
 * Solid-accent hero header (UI design doc v2 §5.1), used atop key screens
 * instead of a flat TopAppBar. Replaces the retired gradient GradientHeader.
 */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Accent)
            .padding(horizontal = 18.dp, vertical = 22.dp),
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        if (subtitle != null) {
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
