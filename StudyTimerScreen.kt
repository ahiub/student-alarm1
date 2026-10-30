package com.example.ui.screens

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PomodoroPreset
import com.example.ui.AlarmViewModel
import kotlinx.coroutines.delay

enum class TimerMode(val title: String) {
    STUDY("Focus"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

@Composable
fun StudyTimerScreen(
    viewModel: AlarmViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val studyMins by viewModel.pomodoroStudyMins.collectAsState()
    val breakMins by viewModel.pomodoroBreakMins.collectAsState()
    val longBreakMins by viewModel.pomodoroLongBreakMins.collectAsState()
    val sessionsBeforeLong by viewModel.pomodoroSessionsBeforeLong.collectAsState()
    val completedSessions by viewModel.pomodoroCompletedSessions.collectAsState()
    val presets by viewModel.pomodoroPresets.collectAsState()
    val activePresetName by viewModel.activePomodoroPresetName.collectAsState()

    var timerMode by remember { mutableStateOf(TimerMode.STUDY) }

    // Dialog states
    var showCustomValueDialogFor by remember { mutableStateOf<String?>(null) } // "focus", "short", "long", "sessions"
    var showPresetsDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetToEdit by remember { mutableStateOf<PomodoroPreset?>(null) }

    val currentTargetSeconds = when (timerMode) {
        TimerMode.STUDY -> studyMins * 60
        TimerMode.SHORT_BREAK -> breakMins * 60
        TimerMode.LONG_BREAK -> longBreakMins * 60
    }

    var totalSeconds by remember(timerMode, studyMins, breakMins, longBreakMins) {
        mutableIntStateOf(currentTargetSeconds)
    }
    var remainingSeconds by remember(totalSeconds) { mutableIntStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(false) }

    // Countdown loop
    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        } else if (isRunning && remainingSeconds == 0) {
            isRunning = false
            playCompletionSoundAndVibrate(context)

            if (timerMode == TimerMode.STUDY) {
                viewModel.incrementPomodoroSession()
                val nextCount = completedSessions + 1
                if (nextCount % sessionsBeforeLong == 0) {
                    timerMode = TimerMode.LONG_BREAK
                } else {
                    timerMode = TimerMode.SHORT_BREAK
                }
            } else {
                timerMode = TimerMode.STUDY
            }
        }
    }

    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "timer_progress")

    val activeColor by animateColorAsState(
        targetValue = when (timerMode) {
            TimerMode.STUDY -> MaterialTheme.colorScheme.primary
            TimerMode.SHORT_BREAK -> MaterialTheme.colorScheme.tertiary
            TimerMode.LONG_BREAK -> MaterialTheme.colorScheme.secondary
        },
        label = "timer_active_color"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP HEADER & ACTIVE PRESET
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Study Timer",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "Customizable Pomodoro for optimal academic focus",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Presets manager button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showPresetsDialog = true }
                    .testTag("pomodoro_presets_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Presets",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Presets",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Active preset pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Text(
                text = "Preset: $activePresetName",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3-WAY MODE SWITCHER (Study Focus, Short Break, Long Break)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                // Focus Tab
                ModeTabButton(
                    title = "Focus",
                    icon = Icons.Default.School,
                    isSelected = timerMode == TimerMode.STUDY,
                    activeColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        timerMode = TimerMode.STUDY
                        isRunning = false
                        totalSeconds = studyMins * 60
                        remainingSeconds = totalSeconds
                    },
                    testTag = "mode_study"
                )

                // Short Break Tab
                ModeTabButton(
                    title = "Short Break",
                    icon = Icons.Default.Coffee,
                    isSelected = timerMode == TimerMode.SHORT_BREAK,
                    activeColor = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        timerMode = TimerMode.SHORT_BREAK
                        isRunning = false
                        totalSeconds = breakMins * 60
                        remainingSeconds = totalSeconds
                    },
                    testTag = "mode_short_break"
                )

                // Long Break Tab
                ModeTabButton(
                    title = "Long Break",
                    icon = Icons.Default.SelfImprovement,
                    isSelected = timerMode == TimerMode.LONG_BREAK,
                    activeColor = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        timerMode = TimerMode.LONG_BREAK
                        isRunning = false
                        totalSeconds = longBreakMins * 60
                        remainingSeconds = totalSeconds
                    },
                    testTag = "mode_long_break"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CIRCULAR TIMER CANVAS
        Box(
            modifier = Modifier
                .size(250.dp)
                .testTag("circular_timer_box"),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                // Track
                drawCircle(
                    color = trackColor,
                    style = Stroke(width = strokeWidth)
                )
                // Progress Arc
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(activeColor.copy(alpha = 0.6f), activeColor, activeColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                Text(
                    text = "%02d:%02d".format(minutes, seconds),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 54.sp,
                        letterSpacing = (-1).sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (timerMode) {
                        TimerMode.STUDY -> "Study Focus"
                        TimerMode.SHORT_BREAK -> "Quick Refresh"
                        TimerMode.LONG_BREAK -> "Deep Recovery"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = activeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CONTROLS (RESET / PLAY-PAUSE)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    isRunning = false
                    remainingSeconds = totalSeconds
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("timer_reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Timer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            Button(
                onClick = { isRunning = !isRunning },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .size(72.dp)
                    .testTag("timer_play_pause_button")
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // CUSTOMIZATION SECTION (Problem 2)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize Durations",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Reset to defaults (25/5/15/4)
                    OutlinedButton(
                        onClick = {
                            viewModel.resetPomodoroToDefaults()
                            isRunning = false
                            totalSeconds = 25 * 60
                            remainingSeconds = totalSeconds
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp).testTag("pomodoro_reset_defaults_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset (25/5)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. FOCUS TIME (MINUTES)
                DurationFieldEditor(
                    label = "Focus time",
                    currentValue = studyMins,
                    unit = "min",
                    quickPresets = listOf(15, 25, 30, 45, 60),
                    onValueChange = { newVal ->
                        val valid = newVal.coerceIn(1, 180)
                        viewModel.setPomodoroDurations(valid, breakMins, longBreakMins, sessionsBeforeLong)
                        if (timerMode == TimerMode.STUDY && !isRunning) {
                            totalSeconds = valid * 60
                            remainingSeconds = totalSeconds
                        }
                    },
                    onOpenCustomDialog = { showCustomValueDialogFor = "focus" },
                    activeColor = MaterialTheme.colorScheme.primary,
                    testTagPrefix = "focus"
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 2. SHORT BREAK (MINUTES)
                DurationFieldEditor(
                    label = "Short break",
                    currentValue = breakMins,
                    unit = "min",
                    quickPresets = listOf(5, 10, 15),
                    onValueChange = { newVal ->
                        val valid = newVal.coerceIn(1, 60)
                        viewModel.setPomodoroDurations(studyMins, valid, longBreakMins, sessionsBeforeLong)
                        if (timerMode == TimerMode.SHORT_BREAK && !isRunning) {
                            totalSeconds = valid * 60
                            remainingSeconds = totalSeconds
                        }
                    },
                    onOpenCustomDialog = { showCustomValueDialogFor = "short" },
                    activeColor = MaterialTheme.colorScheme.tertiary,
                    testTagPrefix = "short_break"
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 3. LONG BREAK (MINUTES)
                DurationFieldEditor(
                    label = "Long break",
                    currentValue = longBreakMins,
                    unit = "min",
                    quickPresets = listOf(15, 20, 30),
                    onValueChange = { newVal ->
                        val valid = newVal.coerceIn(1, 90)
                        viewModel.setPomodoroDurations(studyMins, breakMins, valid, sessionsBeforeLong)
                        if (timerMode == TimerMode.LONG_BREAK && !isRunning) {
                            totalSeconds = valid * 60
                            remainingSeconds = totalSeconds
                        }
                    },
                    onOpenCustomDialog = { showCustomValueDialogFor = "long" },
                    activeColor = MaterialTheme.colorScheme.secondary,
                    testTagPrefix = "long_break"
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 4. SESSIONS BEFORE LONG BREAK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sessions before long break",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Take a long break after $sessionsBeforeLong focus rounds",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (sessionsBeforeLong > 1) {
                                    viewModel.setPomodoroDurations(studyMins, breakMins, longBreakMins, sessionsBeforeLong - 1)
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                        }

                        Text(
                            text = "$sessionsBeforeLong",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        IconButton(
                            onClick = {
                                if (sessionsBeforeLong < 12) {
                                    viewModel.setPomodoroDurations(studyMins, breakMins, longBreakMins, sessionsBeforeLong + 1)
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save as named preset button
                Button(
                    onClick = { showSavePresetDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("save_current_as_preset_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Current as Named Preset",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SESSIONS STATS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "$completedSessions Focus Sessions Completed",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    )
                    Text(
                        text = "Round ${((completedSessions % sessionsBeforeLong) + 1)} of $sessionsBeforeLong before long break",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }

    // --- DIALOG: CUSTOM VALUE NUMBER ENTRY WITH VALIDATION (1 to 180) ---
    if (showCustomValueDialogFor != null) {
        val fieldType = showCustomValueDialogFor!!
        val title = when (fieldType) {
            "focus" -> "Custom Focus Time"
            "short" -> "Custom Short Break"
            "long" -> "Custom Long Break"
            else -> "Custom Value"
        }
        val current = when (fieldType) {
            "focus" -> studyMins
            "short" -> breakMins
            "long" -> longBreakMins
            else -> sessionsBeforeLong
        }
        var textInput by remember { mutableStateOf(current.toString()) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCustomValueDialogFor = null },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a duration in minutes (1 to 180):",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { input ->
                            textInput = input
                            val parsed = input.toIntOrNull()
                            errorMessage = when {
                                input.isBlank() -> "Cannot be empty"
                                parsed == null -> "Please enter a valid number"
                                parsed <= 0 -> "Must be greater than 0"
                                parsed > 180 -> "Maximum is 180 minutes"
                                else -> null
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = errorMessage != null,
                        supportingText = {
                            if (errorMessage != null) {
                                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_duration_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = textInput.toIntOrNull()
                        if (parsed != null && parsed in 1..180) {
                            when (fieldType) {
                                "focus" -> {
                                    viewModel.setPomodoroDurations(parsed, breakMins, longBreakMins, sessionsBeforeLong)
                                    if (timerMode == TimerMode.STUDY && !isRunning) {
                                        totalSeconds = parsed * 60
                                        remainingSeconds = totalSeconds
                                    }
                                }
                                "short" -> {
                                    viewModel.setPomodoroDurations(studyMins, parsed, longBreakMins, sessionsBeforeLong)
                                    if (timerMode == TimerMode.SHORT_BREAK && !isRunning) {
                                        totalSeconds = parsed * 60
                                        remainingSeconds = totalSeconds
                                    }
                                }
                                "long" -> {
                                    viewModel.setPomodoroDurations(studyMins, breakMins, parsed, sessionsBeforeLong)
                                    if (timerMode == TimerMode.LONG_BREAK && !isRunning) {
                                        totalSeconds = parsed * 60
                                        remainingSeconds = totalSeconds
                                    }
                                }
                            }
                            showCustomValueDialogFor = null
                        } else {
                            errorMessage = "Enter a value between 1 and 180"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCustomValueDialogFor = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- DIALOG: SAVE CURRENT CONFIG AS NAMED PRESET ---
    if (showSavePresetDialog) {
        var presetNameInput by remember {
            mutableStateOf("Exam focus ${studyMins}/${breakMins}")
        }
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = {
                Text(
                    text = "Save Pomodoro Preset",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "Focus: ${studyMins}m | Short: ${breakMins}m | Long: ${longBreakMins}m",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        label = { Text("Preset Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_preset_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetNameInput.isNotBlank()) {
                            viewModel.savePomodoroPreset(
                                name = presetNameInput.trim(),
                                focusMins = studyMins,
                                shortBreakMins = breakMins,
                                longBreakMins = longBreakMins,
                                sessionsBeforeLong = sessionsBeforeLong
                            )
                            showSavePresetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Preset")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- DIALOG: PRESETS LIST & MANAGEMENT (Pick, Rename, Delete) ---
    if (showPresetsDialog) {
        AlertDialog(
            onDismissRequest = { showPresetsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pomodoro Presets",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Pick, rename or delete your saved study routines:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (presets.isEmpty()) {
                        Text(
                            text = "No saved presets yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    } else {
                        presets.forEach { preset ->
                            val isSelected = preset.name == activePresetName
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.applyPomodoroPreset(preset)
                                        isRunning = false
                                        totalSeconds = preset.focusMinutes * 60
                                        remainingSeconds = totalSeconds
                                        showPresetsDialog = false
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = "${preset.focusMinutes}m focus / ${preset.shortBreakMinutes}m short / ${preset.longBreakMinutes}m long",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = { presetToEdit = preset },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deletePomodoroPreset(preset) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPresetsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // --- DIALOG: RENAME PRESET ---
    presetToEdit?.let { preset ->
        var renameInput by remember(preset) { mutableStateOf(preset.name) }
        AlertDialog(
            onDismissRequest = { presetToEdit = null },
            title = {
                Text("Rename Preset")
            },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Preset Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.updatePomodoroPreset(preset.copy(name = renameInput.trim()))
                            presetToEdit = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { presetToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ModeTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) activeColor else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun DurationFieldEditor(
    label: String,
    currentValue: Int,
    unit: String,
    quickPresets: List<Int>,
    onValueChange: (Int) -> Unit,
    onOpenCustomDialog: () -> Unit,
    activeColor: Color,
    testTagPrefix: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            // Step controls (- and +)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onValueChange(currentValue - 1) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("${testTagPrefix}_decrease")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease $label",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "$currentValue $unit",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = activeColor
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                IconButton(
                    onClick = { onValueChange(currentValue + 1) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("${testTagPrefix}_increase")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase $label",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick presets row + Custom option button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickPresets.forEach { presetVal ->
                val isSelected = currentValue == presetVal
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onValueChange(presetVal) }
                        .testTag("${testTagPrefix}_preset_${presetVal}")
                ) {
                    Text(
                        text = "$presetVal",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // "Custom" button
            val isCustomActive = !quickPresets.contains(currentValue)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isCustomActive) activeColor else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1.3f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenCustomDialog)
                    .testTag("${testTagPrefix}_custom_button")
            ) {
                Text(
                    text = "Custom",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCustomActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

private fun playCompletionSoundAndVibrate(context: Context) {
    try {
        val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val player = MediaPlayer.create(context, alertUri)
        player?.start()
    } catch (_: Exception) {}

    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(800)
        }
    } catch (_: Exception) {}
}
