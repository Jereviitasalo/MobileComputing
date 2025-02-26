package com.jereviitasalo.mobilecomputing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil3.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.room.*
import java.io.File
import androidx.compose.foundation.background
import com.jereviitasalo.mobilecomputing.ui.theme.MobileComputingTheme
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs

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

// Tietokantaoperaatiot
@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProfile(profile: UserProfile)

    @Update
    fun updateProfile(profile: UserProfile)
}

// Tietokanta
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
                ).allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class MainActivity : ComponentActivity(), SensorEventListener {
    // sensori- ja ilmoitusmuuttujat
    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    private var isMonitoring = false

    // Ilmoituskanavan tunniste
    companion object {
        const val CHANNEL_ID = "gyroscope_channel"
        // Permission request code
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }

    // Määritellään ilmoituksen permission launcher
    private lateinit var requestPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()

        // Sensorin alustus - käytetään gyroskooppia
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            // Käsitellään vastaus lupapyyntöön
            val permissionState = if (isGranted) {
                // Lupa myönnetty, käynnistetään monitorointi
                startMonitoring()
                true
            } else {
                // Lupaa ei myönnetty
                false
            }

            // Päivitetään UI state
            isMonitoring = permissionState
        }

        setContent {
            MobileComputingTheme {

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

                var monitoringState by remember { mutableStateOf(isMonitoring) }
                // State notifikaatioluvalle
                var permissionRequested by remember { mutableStateOf(hasNotificationPermission()) }

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Route.conversation
                ) {
                    composable(route = Route.conversation) {
                        // Varmista, että profiili on ajan tasalla aina kun tähän näkymään saavutaan
                        LaunchedEffect(Unit) {
                            refreshProfile()
                        }
                        Conversation(
                            messages = SampleData.conversationSample,
                            navigateToProfile = {
                                navController.navigate(Route.profile) {
                                    popUpTo(Route.conversation) {inclusive = false}
                                }
                            },
                            userProfile = storedProfile,
                        )
                    }
                    composable(route = Route.profile) {
                        Profile(
                            navigateToConversations = {
                                // Päivitä profiili ennen navigointia takaisin
                                refreshProfile()
                                navController.navigate(route = Route.conversation) {
                                    popUpTo(Route.conversation) {inclusive = true}
                                }
                            },
                            db = db,
                            initialProfile = storedProfile,
                            onProfileUpdated = refreshProfile,
                            onToggleMonitoring = {
                                if (!permissionRequested) {
                                    // Käynnistä lupapyyntö
                                    askNotificationPermission()
                                    permissionRequested = hasNotificationPermission()
                                } else if (monitoringState == false) {
                                    startMonitoring()
                                    monitoringState = true
                                } else {
                                    stopMonitoring()
                                    monitoringState = false
                                }
                            },
                            isMonitoring = monitoringState
                        )
                    }
                }
            }
        }
    }

    // Metodi notifikaatioluvan pyytämiseen
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startMonitoring()
                isMonitoring = true
            }
        }
    }

    // LISÄTTY: Metodi joka tarkistaa onko notifikaatiolupa jo olemassa
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Vanhemmilla Android-versioilla oletetaan, että lupa on
            true
        }
    }

    private fun startMonitoring() {
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            isMonitoring = true
        }
    }

    private fun stopMonitoring() {
        sensorManager.unregisterListener(this)
        isMonitoring = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Jos tapahtuu pyörähdys millä tahansa akselilla, lähetetään ilmoitus
            if (abs(x) > 0.01 || abs(y) > 0.01 || abs(z) > 0.01) {
                sendNotification("$y")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Ilmoitusten käsittely
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Gyroscope Detection"
            val descriptionText = "Notifications for gyroscope motion"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(message: String) {
        // Intent, joka avaa sovelluksen, kun ilmoitusta painetaan
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Liikettä havaittu")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(1, builder.build())
            }
        }
    }
}

// Luodaan message objekti
data class Message(val author: String, val body: String)

@Composable
fun MessageCard(
    msg: Message,
    userProfile: UserProfile? = null
) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
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
            Image(
                painter = painterResource(R.drawable.profile_picture),
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        var isExpanded by remember { mutableStateOf(false) }

        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        )

        Column( modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(
                text = userProfile?.username ?: msg.author,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )
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
    userProfile: UserProfile? = null,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = {
                println("Navigating to Profile")
                navigateToProfile()
            }) {
                Text(text = "Profile")
            }
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
    onProfileUpdated: () -> Unit = {},
    onToggleMonitoring: () -> Unit = {},
    isMonitoring: Boolean = false
) {
    val context = LocalContext.current

    // Tarkista lupatila
    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

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

        // Profiilikuva
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

        Spacer(modifier = Modifier.height(16.dp))

        if (!hasPermission || isMonitoring) {
            Button(
                onClick = onToggleMonitoring,
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    text = if (isMonitoring) "Notifications active" else "Enable notifications",
                    color = Color.Black,
                    style = MaterialTheme.typography.titleMedium
                )
            }
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
}