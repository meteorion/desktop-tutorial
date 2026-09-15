package com.interviewcoach.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary = AppColors.PrimaryPurple,
    background = AppColors.PageBackground,
    surface = AppColors.CardBackground,
    onBackground = AppColors.TextPrimary,
)

@Composable
fun InterviewCoachTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColorScheme, content = content)
}
