package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.example.data.local.AlarmEntity
import com.example.data.preferences.AppMode
import com.example.ui.AlarmViewModel
import com.example.ui.screens.AddEditAlarmScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ModeSelectionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyTimerScreen
import com.example.ui.screens.TimetableScreen
import com.example.ui.theme.AppTheme

sealed class Screen {
    data object Home : Screen()
    data object Calendar : Screen()
    data object Timetable : Screen()
    data object StudyTimer : Screen()
    data object Settings : Screen()
    data object ModeSelection : Screen()
    data class AddEditAlarm(val alarm: AlarmEntity? = null, val sharedVoiceNotePath: String? = null) : Screen()
}

class MainActivity : ComponentActivity() {

    private var sharedAudioUriHandler: ((Uri) -> Unit)? = null

    private companion object {
        const val STARTUP_MAX_WAIT_MS = 500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1) Splash screen: shows the AH logo from the very first frame (no white/black flash).
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as StudentAlarmApplication
        var ready = false
        // Keep the splash only until the saved theme/mode is known (hard limit ~500 ms).
        splashScreen.setKeepOnScreenCondition { !ready }

        lifecycleScope.launch {
            // 2) Read the saved theme + mode OFF the main thread (DataStore), max 500 ms.
            if (app.startupSnapshot == null) {
                withTimeoutOrNull(STARTUP_MAX_WAIT_MS) {
                    app.startupSnapshot = withContext(Dispatchers.IO) {
                        app.preferencesRepository.readStartupSnapshot()
                    }
                }
            }

            // 3) Now draw the UI with the right theme from frame one.
            setContent {
                val viewModel: AlarmViewModel = viewModel()
                val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()

                AppTheme(theme = currentTheme) {
                    // Register handler for shared audio from WhatsApp / external apps
                    sharedAudioUriHandler = { uri ->
                        viewModel.handleSharedAudioUri(uri)
                    }

                    // Check intent on launch
                    LaunchedEffect(Unit) {
                        handleIncomingIntent(intent)
                    }

                    MainScreenHost(viewModel = viewModel)
                }
            }
            ready = true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("audio/") == true) {
            val audioUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (audioUri != null) {
                sharedAudioUriHandler?.invoke(audioUri)
            }
        }
    }
}

@Composable
fun MainScreenHost(
    viewModel: AlarmViewModel,
    modifier: Modifier = Modifier
) {
    val hasChosenMode by viewModel.hasChosenMode.collectAsStateWithLifecycle()
    val appMode by viewModel.appMode.collectAsStateWithLifecycle()
    val featTimetable by viewModel.featTimetable.collectAsStateWithLifecycle()
    val featStudyTimer by viewModel.featStudyTimer.collectAsStateWithLifecycle()
    val pendingSharedAudio by viewModel.pendingSharedAudioPath.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    // If the user has not chosen a mode on first launch, show Mode Selection screen
    LaunchedEffect(hasChosenMode) {
        if (!hasChosenMode) {
            currentScreen = Screen.ModeSelection
        }
    }

    // Automatically navigate to Add Alarm when a voice note is shared to the app
    LaunchedEffect(pendingSharedAudio) {
        pendingSharedAudio?.let { audioPath ->
            currentScreen = Screen.AddEditAlarm(alarm = null, sharedVoiceNotePath = audioPath)
            viewModel.clearPendingSharedAudio()
        }
    }

    // Handle Android system Back button
    BackHandler(enabled = currentScreen != Screen.Home && currentScreen != Screen.ModeSelection) {
        currentScreen = Screen.Home
    }

    val isTopLevelScreen = currentScreen in listOf(
        Screen.Home,
        Screen.Calendar,
        Screen.Timetable,
        Screen.StudyTimer,
        Screen.Settings
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isTopLevelScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                ) {
                    // TAB 1: Alarms (Always)
                    NavigationBarItem(
                        selected = currentScreen is Screen.Home,
                        onClick = { currentScreen = Screen.Home },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "Alarms",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Alarms", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_alarms")
                    )

                    // TAB 2: Calendar (Always)
                    NavigationBarItem(
                        selected = currentScreen is Screen.Calendar,
                        onClick = { currentScreen = Screen.Calendar },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Calendar",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text("Calendar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_calendar")
                    )

                    // TAB 3: Timetable (Student Mode or Custom Mode with Timetable enabled)
                    val showTimetableTab = when (appMode) {
                        AppMode.STUDENT -> true
                        AppMode.COMMON -> false
                        AppMode.CUSTOM -> featTimetable
                    }
                    if (showTimetableTab) {
                        NavigationBarItem(
                            selected = currentScreen is Screen.Timetable,
                            onClick = { currentScreen = Screen.Timetable },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "Timetable",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = { Text("Timetable", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.tertiary,
                                selectedTextColor = MaterialTheme.colorScheme.tertiary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_timetable")
                        )
                    }

                    // TAB 4: Study Timer (Student Mode or Custom Mode with Study Timer enabled)
                    val showTimerTab = when (appMode) {
                        AppMode.STUDENT -> true
                        AppMode.COMMON -> false
                        AppMode.CUSTOM -> featStudyTimer
                    }
                    if (showTimerTab) {
                        NavigationBarItem(
                            selected = currentScreen is Screen.StudyTimer,
                            onClick = { currentScreen = Screen.StudyTimer },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Timer",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = { Text("Timer", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_timer")
                        )
                    }

                    // TAB 5: Settings (In Common Mode, or when extra student tabs aren't shown, or as direct tab)
                    if (appMode == AppMode.COMMON || (!showTimetableTab && !showTimerTab)) {
                        NavigationBarItem(
                            selected = currentScreen is Screen.Settings,
                            onClick = { currentScreen = Screen.Settings },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.secondary,
                                selectedTextColor = MaterialTheme.colorScheme.secondary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.ModeSelection -> {
                    ModeSelectionScreen(
                        viewModel = viewModel,
                        onModeConfirmed = { currentScreen = Screen.Home }
                    )
                }

                is Screen.Home -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onAddAlarmClick = {
                            currentScreen = Screen.AddEditAlarm(null, null)
                        },
                        onEditAlarmClick = { alarm ->
                            currentScreen = Screen.AddEditAlarm(alarm, null)
                        },
                        onStreakClick = {
                            currentScreen = Screen.Settings
                        }
                    )
                }

                is Screen.Calendar -> {
                    CalendarScreen(
                        viewModel = viewModel,
                        onAlarmClick = { alarm ->
                            currentScreen = Screen.AddEditAlarm(alarm, null)
                        }
                    )
                }

                is Screen.Timetable -> {
                    TimetableScreen(
                        viewModel = viewModel
                    )
                }

                is Screen.StudyTimer -> {
                    StudyTimerScreen(
                        viewModel = viewModel
                    )
                }

                is Screen.Settings -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onChangeModeClick = { currentScreen = Screen.ModeSelection }
                    )
                }

                is Screen.AddEditAlarm -> {
                    AddEditAlarmScreen(
                        viewModel = viewModel,
                        initialAlarm = screen.alarm,
                        initialVoiceNotePath = screen.sharedVoiceNotePath,
                        onBack = { currentScreen = Screen.Home },
                        onSave = { savedAlarm ->
                            viewModel.saveAlarm(savedAlarm) {
                                currentScreen = Screen.Home
                            }
                        }
                    )
                }
            }
        }
    }
}
