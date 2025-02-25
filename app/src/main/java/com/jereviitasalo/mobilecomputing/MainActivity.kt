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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil3.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.CompositionLocalProvider
import coil3.compose.AsyncImagePreviewHandler
import coil3.request.ImageRequest

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.*
import coil3.compose.AsyncImage
import com.jereviitasalo.mobilecomputing.ui.theme.MobileComputingTheme
import java.io.File
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import com.jereviitasalo.mobilecomputing.ui.theme.MobileComputingTheme

// Entity - tietokantataulu käyttäjäprofiilille
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "username")
    val username: String,
    @ColumnInfo(name = "image_uri")
    val imageUri: String?
)

// DAO - tietokantaoperaatiot
@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProfile(profile: UserProfile)

    @Update
    fun updateProfile(profile: UserProfile)
}

// Database - tietokanta
@Database(entities = [UserProfile::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).allowMainThreadQueries() // Vain demotarkoitukseen
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobileComputingTheme {

                // ----------------------------------------------------------
                // Tietokantayhteys
                val context = LocalContext.current
                val db = remember { AppDatabase.getDatabase(context) }

                // Hae profiilitiedot
                var storedProfile by remember { mutableStateOf<UserProfile?>(null) }

                // Käytä jaettua toimintoa, joka päivittää profiilin aina kun se tarvitaan
                val refreshProfile = {
                    storedProfile = db.userProfileDao().getProfile()
                }

                // Lataa profiili aluksi
                LaunchedEffect(Unit) {
                    refreshProfile()
                }

                /*LaunchedEffect(Unit) {
                    storedProfile = db.userProfileDao().getProfile()
                }*/
                // ---------------------------------------------------------


                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Route.conversation
                ) {
                    composable(route = Route.conversation) {
                        // --------------------------------
                        // Varmista, että profiili on ajan tasalla aina kun tähän näkymään saavutaan
                        LaunchedEffect(Unit) {
                            refreshProfile()
                        }
                        // --------------------------------
                        Conversation(

                            messages = SampleData.conversationSample,
                            navigateToProfile = {
                                navController.navigate(Route.profile) {
                                    popUpTo(Route.conversation) {inclusive = false}
                                }
                            },
                            // -------------------------------
                            userProfile = storedProfile
                            // -------------------------------
                        )
                    }
                    composable(route = Route.profile) {
                        Profile(
                            navigateToConversations = {
                                // --------------------------------
                                // Päivitä profiili ennen navigointia takaisin
                                refreshProfile()
                                // ---------------------------------
                                navController.navigate(route = Route.conversation) {
                                    popUpTo(Route.conversation) {inclusive = true}
                                }
                            },
                            // ---------------------------------
                            db = db,
                            initialProfile = storedProfile,
                            onProfileUpdated = refreshProfile
                            // ---------------------------------
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
fun MessageCard(
    msg: Message,
    // ---------------------------------------
    userProfile: UserProfile? = null,
    context: Context = LocalContext.current
    // ---------------------------------------
) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        // -----------------------------------------------------------------
        // Käytä käyttäjän valitsemaa kuvaa, jos sellainen on tallennettu
        if (userProfile?.imageUri != null) {
            AsyncImage(
                model = Uri.parse(userProfile.imageUri),
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        } else {
        // -------------------------------------------------------------------
            Image(
                painter = painterResource(R.drawable.profile_picture),
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        }

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
                //text = msg.author,
                // ---------------------------------------
                text = userProfile?.username ?: msg.author,
                // ---------------------------------------
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
fun Conversation(
    messages: List<Message>,
    navigateToProfile: () -> Unit,
    // -------------------------------
    userProfile: UserProfile? = null
    // -------------------------------
) {
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
                    message -> MessageCard(message, userProfile)
            }
        }
    }
}

@Composable
fun Profile(
    navigateToConversations: () -> Unit,
    db: AppDatabase,
    initialProfile: UserProfile? = null,
    onProfileUpdated: () -> Unit = {}
) {
    val context = LocalContext.current

    // Käytetään useEffect-tyyppistä ratkaisua, jotta komponentti päivittyy kun initialProfile muuttuu
    var username by remember(initialProfile) { mutableStateOf(initialProfile?.username ?: "") }
    var imageUri by remember(initialProfile) { mutableStateOf<Uri?>(initialProfile?.imageUri?.let { Uri.parse(it) }) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // Kopioi kuva sovelluksen sisäiseen tallennustilaan
            val inputStream = context.contentResolver.openInputStream(selectedUri)
            val file = File(context.filesDir, "profile_image.jpg")
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            imageUri = Uri.fromFile(file)
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Otsikko (Conversation) näkyy ylhäällä sinisessä napissa
        Button(
            onClick = navigateToConversations,
            modifier = Modifier
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF80D8FF)
            )
        ) {
            Text(
                text = "Conversation",
                color = Color.Black,
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Profiilikuva (pyöreä)
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("?")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Username input kenttä
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            placeholder = { Text("someone") },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pick photo -nappi
        Button(
            onClick = {
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                text = "Pick photo",
                color = Color.Black,
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Automaattinen tallennus kun poistutaan näkymästä
        DisposableEffect(username, imageUri) {
            onDispose {
                if (username.isNotBlank() || imageUri != null) {
                    val profile = UserProfile(
                        id = initialProfile?.id ?: 0,
                        username = username,
                        imageUri = imageUri?.toString()
                    )
                    db.userProfileDao().insertProfile(profile)
                    onProfileUpdated()
                }
            }
        }
    }

    /*Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "User Profile Page")
        Button(onClick = navigateToConversations) {
            Text(text = "Conversation")
        }
    }*/
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