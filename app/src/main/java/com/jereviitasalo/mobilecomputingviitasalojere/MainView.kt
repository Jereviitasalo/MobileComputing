package com.jereviitasalo.mobilecomputingviitasalojere

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.navigation.NavController

class MainView : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_view)
    }
}

@Composable
fun MainView(navController: NavController, isDarkTheme: MutableState<Boolean>) {

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Greeting("User!", isDarkTheme)
        DisplayImage(isDarkTheme)
    }
    Button(onClick = { navController.navigate("second") }) {
        Text("Go to Second View")
    }
}