package com.jereviitasalo.mobilecomputing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import android.provider.Telephony.Sms.Conversations
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jereviitasalo.mobilecomputing.ui.theme.MobileComputingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobileComputingTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Route.conversation
                ) {
                    composable(route = Route.conversation) {
                        Conversation(
                            messages = SampleData.conversationSample,
                            navigateToProfile = {
                                navController.navigate(Route.profile) {
                                    popUpTo(Route.conversation) {inclusive = false}
                                }
                            }
                        )
                    }
                    composable(route = Route.profile) {
                        Profile(
                            navigateToConversations = {
                                navController.navigate(route = Route.conversation) {
                                    popUpTo(Route.conversation) {inclusive = true}
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// Create message object
data class Message(val author: String, val body: String)

@Composable
fun MessageCard(msg: Message) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Image(
            painter = painterResource(R.drawable.profile_picture),
            contentDescription = "Profile picture",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )

        // Some horizontal spacing between image and text
        Spacer(modifier = Modifier.width(8.dp))

        // Variable to keep track if message is expanded or not
        var isExpanded by remember { mutableStateOf(false) }

        // surfaceColor will be updated gradually from one color to the other
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        )

        Column( modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(
                text = msg.author,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )
            // Some vertical spacing between texts
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 3.dp,
                color = surfaceColor,
                modifier = Modifier
                    .animateContentSize()
                    .padding(1.dp)
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),

                    // IF message is expanded, display all its content
                    // otherwise we only display the first line
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

object Route {
    const val conversation = "conversation"
    const val profile = "profile"
}

@Composable
fun Conversation(messages: List<Message>, navigateToProfile: () -> Unit) {
    Column {
        Button(onClick = {
            println("Navigating to Profile")
            navigateToProfile()
        },
            modifier = Modifier.padding(8.dp)
        ) {
            Text(text = "Profile")
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            items(messages) {
                    message -> MessageCard(message)
            }
        }
    }
}

@Composable
fun Profile(navigateToConversations: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "User Profile Page")
        Button(onClick = navigateToConversations) {
            Text(text = "Conversation")
        }
    }
}

//@Preview
//@Composable
//fun ConversationPreview() {
//    MobileComputingTheme {
//        Conversation(messages = SampleData.conversationSample, )
//    }
//}

//@Preview(name = "Light Mode")
//@Preview(
//    name = "Dark Mode",
//    uiMode = Configuration.UI_MODE_NIGHT_YES,
//    showBackground = true
//)
//@Composable
//fun MessageCardPreview() {
//    MobileComputingTheme {
//        Surface {
//            MessageCard(msg = Message("User 1", "This is a preview message!"))
//        }
//    }
//}