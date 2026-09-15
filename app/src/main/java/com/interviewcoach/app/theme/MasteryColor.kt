package com.interviewcoach.app.theme

import androidx.compose.ui.graphics.Color

/**
 * Maps a 0-100 mastery/interview-dimension score to the low/mid/high accent
 * from the UI design doc v2 §5.1.
 */
fun masteryColor(score: Double): Color = when {
    score < 60.0 -> AppColors.MasteryLow
    score < 80.0 -> AppColors.MasteryMid
    else -> AppColors.MasteryHigh
}
