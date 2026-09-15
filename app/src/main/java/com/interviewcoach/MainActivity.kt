package com.interviewcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.interviewcoach.app.navigation.RootShell
import com.interviewcoach.app.theme.InterviewCoachTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { InterviewCoachTheme { RootShell() } }
    }
}
