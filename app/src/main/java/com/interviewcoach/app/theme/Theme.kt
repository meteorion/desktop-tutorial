package com.interviewcoach.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = lightColorScheme(
    primary = AppColors.Accent,
    onPrimary = Color.White,
    primaryContainer = AppColors.AccentSoftBackground,
    onPrimaryContainer = AppColors.Accent,
    secondary = AppColors.Accent,
    onSecondary = Color.White,
    background = AppColors.PageBackground,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.CardBackground,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.CardBorder,
    outlineVariant = Color(0xFFEEEEEE),
    error = AppColors.ErrorText,
    onError = Color.White,
    errorContainer = AppColors.ErrorBackground,
    onErrorContainer = AppColors.ErrorText,
)

@Composable
fun InterviewCoachTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColorScheme, content = content)
}
