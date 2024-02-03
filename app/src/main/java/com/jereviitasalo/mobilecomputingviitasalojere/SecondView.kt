package com.jereviitasalo.mobilecomputingviitasalojere

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

class SecondView : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second_view)
    }
}

@Composable
fun SecondView(navController: NavController) {
    Button(onClick = { navController.popBackStack() }) {
        Text("Go Back")
    }
}