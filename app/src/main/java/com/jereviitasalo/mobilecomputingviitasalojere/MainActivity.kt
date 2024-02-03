package com.jereviitasalo.mobilecomputingviitasalojere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.jereviitasalo.mobilecomputingviitasalojere.ui.theme.MobileComputingViitasaloJereTheme

class MainActivity : ComponentActivity() {
    private val isDarkTheme = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobileComputingViitasaloJereTheme(darkTheme = isDarkTheme.value) {
                AppNavigation(isDarkTheme)
            }
        }
    }
}