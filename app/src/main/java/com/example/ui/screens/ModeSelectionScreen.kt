package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.AppMode
import com.example.ui.AlarmViewModel

@Composable
fun ModeSelectionScreen(
    viewModel: AlarmViewModel,
    onModeConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMode by viewModel.appMode.collectAsState()
    var selectedMode by remember(currentMode) { mutableStateOf(currentMode) }

    val featSubjects by viewModel.featSubjects.collectAsState()
    val featAlarmTypes by viewModel.featAlarmTypes.collectAsState()
    val featSmartReminders by viewModel.featSmartReminders.collectAsState()
    val featTimetable by viewModel.featTimetable.collectAsState()
    val featStudyTimer by viewModel.featStudyTimer.collectAsState()
    val featStreak by viewModel.featStreak.collectAsState()
    val customModes by viewModel.customModes.collectAsState()
    val activeCustomModeName by viewModel.activeCustomModeName.collectAsState()

    var customSubjects by remember(featSubjects) { mutableStateOf(featSubjects) }
    var customAlarmTypes by remember(featAlarmTypes) { mutableStateOf(featAlarmTypes) }
    var customSmartReminders by remember(featSmartReminders) { mutableStateOf(featSmartReminders) }
    var customTimetable by remember(featTimetable) { mutableStateOf(featTimetable) }
    var customStudyTimer by remember(featStudyTimer) { mutableStateOf(featStudyTimer) }
    var customStreak by remember(featStreak) { mutableStateOf(featStreak) }

    var showNameCustomModeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Header
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    ),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_ah_logo),
                contentDescription = "AH Logo",
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "How do you want to use this app?",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Choose your experience. Changing the mode never deletes your alarms, notes, voice notes, or subjects.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // CARD 1: STUDENT MODE
        ModeCard(
            title = "Student Mode",
            subtitle = "Everything in Common + subjects, alarm types, smart reminders, timetable, study timer and streak.",
            icon = Icons.Default.School,
            iconTint = MaterialTheme.colorScheme.primary,
            accentColor = MaterialTheme.colorScheme.primary,
            isSelected = selectedMode == AppMode.STUDENT,
            badge = "RECOMMENDED FOR STUDENTS",
            onClick = { selectedMode = AppMode.STUDENT }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // CARD 2: COMMON MODE
        ModeCard(
            title = "Common Mode",
            subtitle = "Simple alarms with notes and voice notes. Clean layout with Home, Calendar, and Settings.",
            icon = Icons.Default.Alarm,
            iconTint = MaterialTheme.colorScheme.secondary,
            accentColor = MaterialTheme.colorScheme.secondary,
            isSelected = selectedMode == AppMode.COMMON,
            badge = null,
            onClick = { selectedMode = AppMode.COMMON }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // CARD 3: CUSTOM MODE
        ModeCard(
            title = "Custom Mode",
            subtitle = "Select exact features you want enabled with named presets. Only your chosen features will be shown.",
            icon = Icons.Default.Tune,
            iconTint = MaterialTheme.colorScheme.tertiary,
            accentColor = MaterialTheme.colorScheme.tertiary,
            isSelected = selectedMode == AppMode.CUSTOM,
            badge = null,
            onClick = { selectedMode = AppMode.CUSTOM }
        )

        // Custom Toggles Section (visible if Custom Mode selected)
        AnimatedVisibility(visible = selectedMode == AppMode.CUSTOM) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Customize Enabled Features",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    CustomFeatureToggle("Subjects (Math, Physics, etc.)", customSubjects) { customSubjects = it }
                    CustomFeatureToggle("Alarm Types (Exam, Class, Assignment)", customAlarmTypes) { customAlarmTypes = it }
                    CustomFeatureToggle("Smart Reminders (1d, 3h, 30m before)", customSmartReminders) { customSmartReminders = it }
                    CustomFeatureToggle("Timetable Weekly Schedule", customTimetable) { customTimetable = it }
                    CustomFeatureToggle("Study Timer (Pomodoro)", customStudyTimer) { customStudyTimer = it }
                    CustomFeatureToggle("On-Time Study Streak", customStreak) { customStreak = it }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Confirmation Button
        Button(
            onClick = {
                if (selectedMode == AppMode.CUSTOM) {
                    showNameCustomModeDialog = true
                } else {
                    viewModel.setAppMode(selectedMode)
                    onModeConfirmed()
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("confirm_mode_button")
        ) {
            Text(
                text = if (selectedMode == AppMode.CUSTOM) "Save Custom Mode & Continue" else "Apply & Continue",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Dialog to name the custom mode if user selects Custom
    if (showNameCustomModeDialog) {
        var modeName by remember { mutableStateOf(activeCustomModeName.ifBlank { "My Custom Mode" }) }
        AlertDialog(
            onDismissRequest = { showNameCustomModeDialog = false },
            title = {
                Text(
                    text = "Name Your Custom Mode",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "Give this custom preset a name (e.g. 'Study Mode', 'Exam Mode', 'Minimal'):",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = modeName,
                        onValueChange = { modeName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_mode_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = modeName.trim().ifBlank { "Custom Mode" }
                        val features = mutableListOf<String>()
                        if (customSubjects) features.add("subjects")
                        if (customAlarmTypes) features.add("alarm_types")
                        if (customSmartReminders) features.add("smart_reminders")
                        if (customTimetable) features.add("timetable")
                        if (customStudyTimer) features.add("study_timer")
                        if (customStreak) features.add("streak")

                        viewModel.saveCustomMode(
                            name = name,
                            enabledFeatures = features.joinToString(",")
                        )
                        showNameCustomModeDialog = false
                        onModeConfirmed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save & Apply")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNameCustomModeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    accentColor: Color,
    isSelected: Boolean,
    badge: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("mode_card_${title.replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.2f),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomFeatureToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
