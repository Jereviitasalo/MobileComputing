package com.jereviitasalo.mobilecomputingviitasalojere

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class Components : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_components)
    }
}

@Composable
fun Greeting(name: String, isDarkTheme: MutableState<Boolean>) {
    LazyColumn(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.background(if (isDarkTheme.value) Color.Black else Color.White)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Hello $name",
                    color = if (isDarkTheme.value) Color.White else Color.Black,
                    modifier = Modifier
                        .background(if (isDarkTheme.value) Color.Black else Color.White),
                    fontSize = 50.sp,
                    textAlign = TextAlign.Center
                )
                Button(onClick = { isDarkTheme.value = !isDarkTheme.value }) {
                    Text("Dark Theme")
                }
            }
            Text(
                text = "Here are pictures of cats.",
                color = if (isDarkTheme.value) Color.White else Color.Black,
                fontSize = 35.sp,
            )
        }
    }
}

@Composable
fun DisplayImage(isDarkTheme: MutableState<Boolean>) {

    val catImages = listOf(
        R.drawable.cat1,
        R.drawable.cat2,
        R.drawable.cat3,
        R.drawable.cat4
    )

    LazyColumn (horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.background(if (isDarkTheme.value) Color.Black else Color.White)) {
        items(catImages) { catImage ->
            Image(
                painter = painterResource(id = catImage),
                contentDescription = null,
                modifier = Modifier
                    .height(300.dp)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}