package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AlarmEntity
import com.example.data.local.AlarmType
import com.example.data.local.RepeatType
import com.example.ui.AlarmViewModel
import com.example.ui.components.AlarmTunePickerDialog
import com.example.ui.components.SubjectManagerDialog
import com.example.ui.components.VoiceNoteRecorderPlayer
import com.example.ui.components.parseHexColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditAlarmScreen(
    viewModel: AlarmViewModel,
    initialAlarm: AlarmEntity?,
    initialVoiceNotePath: String? = null,
    onBack: () -> Unit,
    onSave: (AlarmEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEditMode = initialAlarm != null

    val subjects by viewModel.subjects.collectAsState()
    val isSubjectsEnabled = viewModel.isFeatureEnabled("subjects")
    val isAlarmTypesEnabled = viewModel.isFeatureEnabled("alarm_types")
    val isSmartRemindersEnabled = viewModel.isFeatureEnabled("smart_reminders")
    val defaultTuneName by viewModel.alarmToneName.collectAsState()
    val defaultTuneUri by viewModel.alarmToneUri.collectAsState()

    var title by remember { mutableStateOf(initialAlarm?.title ?: "") }
    var selectedSubjectId by remember { mutableStateOf(initialAlarm?.subjectId ?: subjects.firstOrNull()?.id) }
    var selectedAlarmType by remember {
        mutableStateOf(initialAlarm?.alarmType ?: AlarmType.NORMAL.name)
    }

    // Smart reminders set ("1d", "3h", "30m")
    var smartRemindersSet by remember {
        mutableStateOf(
            initialAlarm?.smartReminders?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
                ?: setOf()
        )
    }

    var hour by remember { mutableIntStateOf(initialAlarm?.hour ?: LocalTime.now().plusHours(1).hour) }
    var minute by remember { mutableIntStateOf(initialAlarm?.minute ?: 0) }
    var selectedDateEpochDays by remember {
        mutableStateOf(initialAlarm?.targetDateEpochDays ?: LocalDate.now().toEpochDay())
    }
    var repeatType by remember {
        mutableStateOf(initialAlarm?.repeatType ?: RepeatType.ONCE.name)
    }
    var customDays by remember {
        mutableStateOf(
            initialAlarm?.repeatDays?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet()
                ?: setOf(1, 2, 3, 4, 5)
        )
    }
    var voiceNotePath by remember {
        mutableStateOf(initialAlarm?.voiceNotePath ?: initialVoiceNotePath)
    }
    var voiceNoteDurationMs by remember {
        mutableStateOf(initialAlarm?.voiceNoteDurationMs ?: 0L)
    }
    var snoozeMinutes by remember { mutableIntStateOf(initialAlarm?.snoozeMinutes ?: 10) }
    var vibrate by remember { mutableStateOf(initialAlarm?.vibrate ?: true) }

    // Alarm tune state (Problem 4)
    var alarmTuneUri by remember { mutableStateOf(initialAlarm?.alarmTuneUri ?: defaultTuneUri) }
    var alarmTuneName by remember { mutableStateOf(initialAlarm?.alarmTuneName ?: defaultTuneName) }
    var showTunePickerDialog by remember { mutableStateOf(false) }

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showSubjectManagerDialog by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateEpochDays * 86400000L
    )

    val timeFormatted = remember(hour, minute) {
        val amPm = if (hour >= 12) "PM" else "AM"
        val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        Pair("%d:%02d".format(hour12, minute), amPm)
    }

    val selectedDate = remember(selectedDateEpochDays) {
        LocalDate.ofEpochDay(selectedDateEpochDays)
    }
    val dateFormatted = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Alarm" else "New Alarm",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val savedDays = if (repeatType == RepeatType.CUSTOM.name) {
                                customDays.sorted().joinToString(",")
                            } else ""

                            val currentSubjectObj = subjects.find { it.id == selectedSubjectId }
                            val subjectName = currentSubjectObj?.name ?: "General"

                            val alarmToSave = (initialAlarm ?: AlarmEntity(
                                title = title,
                                hour = hour,
                                minute = minute
                            )).copy(
                                title = title.trim(),
                                subject = subjectName,
                                subjectId = if (isSubjectsEnabled) selectedSubjectId else null,
                                alarmType = if (isAlarmTypesEnabled) selectedAlarmType else AlarmType.NORMAL.name,
                                smartReminders = if (isSmartRemindersEnabled) smartRemindersSet.joinToString(",") else "",
                                hour = hour,
                                minute = minute,
                                targetDateEpochDays = if (repeatType == RepeatType.ONCE.name) selectedDateEpochDays else null,
                                repeatType = repeatType,
                                repeatDays = savedDays,
                                isEnabled = true,
                                voiceNotePath = voiceNotePath,
                                voiceNoteDurationMs = voiceNoteDurationMs,
                                vibrate = vibrate,
                                snoozeMinutes = snoozeMinutes,
                                alarmTuneUri = alarmTuneUri,
                                alarmTuneName = alarmTuneName
                            )
                            onSave(alarmToSave)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("save_alarm_button")
                    ) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // 1. TIME DISPLAY & PICKER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        TimePickerDialog(
                            context,
                            { _, sh, sm ->
                                hour = sh
                                minute = sm
                            },
                            hour,
                            minute,
                            false
                        ).show()
                    }
                    .testTag("time_picker_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Alarm Time",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = timeFormatted.first,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 54.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeFormatted.second,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    Text(
                        text = "Tap to change time",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. ALARM TYPE SELECTOR (Normal, Assignment, Exam, Class) - if enabled in mode
            if (isAlarmTypesEnabled) {
                Text(
                    text = "Alarm Type",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                val alarmTypes = listOf(
                    Triple(AlarmType.NORMAL.name, "Normal", Icons.Default.Alarm),
                    Triple(AlarmType.ASSIGNMENT.name, "Assignment", Icons.Default.Assignment),
                    Triple(AlarmType.EXAM.name, "Exam", Icons.Default.WarningAmber),
                    Triple(AlarmType.CLASS.name, "Class", Icons.Default.Class)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    alarmTypes.forEach { (typeKey, label, icon) ->
                        val isSelected = selectedAlarmType == typeKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAlarmType = typeKey },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.weight(1f).testTag("alarm_type_$typeKey")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. SUBJECT SELECTION & MANAGEMENT - if enabled in mode
            if (isSubjectsEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subject",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    OutlinedButton(
                        onClick = { showSubjectManagerDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(32.dp).testTag("manage_subjects_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manage", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjects.forEach { subject ->
                        val isSelected = selectedSubjectId == subject.id
                        val subjectColor = parseHexColor(subject.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSubjectId = subject.id },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(subjectColor)
                                )
                            },
                            label = {
                                Text(
                                    text = subject.name,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                selectedContainerColor = subjectColor.copy(alpha = 0.25f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) subjectColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("subject_chip_${subject.id}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. SMART REMINDERS (1 Day, 3 Hours, 30 Mins Before)
            if (isSmartRemindersEnabled && (selectedAlarmType == AlarmType.ASSIGNMENT.name || selectedAlarmType == AlarmType.EXAM.name)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Smart Extra Reminders",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            )
                        }

                        Text(
                            text = "Automatic alerts to prepare in advance for your ${selectedAlarmType.lowercase()}:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )

                        listOf(
                            "1d" to "1 day before",
                            "3h" to "3 hours before",
                            "30m" to "30 minutes before"
                        ).forEach { (code, label) ->
                            val isChecked = smartRemindersSet.contains(code)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        smartRemindersSet = if (isChecked) smartRemindersSet - code else smartRemindersSet + code
                                    }
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        smartRemindersSet = if (checked) smartRemindersSet + code else smartRemindersSet - code
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.tertiary
                                    ),
                                    modifier = Modifier.size(28.dp).testTag("smart_reminder_$code")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 5. TEXT NOTE INPUT
            Text(
                text = "Reminder Note",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text(text = "e.g. Math assignment due, Physics quiz prep")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alarm_note_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = false,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 6. REPEAT OPTIONS (Once, Daily, Weekdays, Custom)
            Text(
                text = "Repeat",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    RepeatType.ONCE.name to "Once",
                    RepeatType.DAILY.name to "Daily",
                    RepeatType.WEEKDAYS.name to "Mon-Fri",
                    RepeatType.CUSTOM.name to "Custom"
                ).forEach { (type, label) ->
                    val isSelected = repeatType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { repeatType = type },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.secondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.weight(1f).testTag("repeat_chip_$label")
                    )
                }
            }

            AnimatedVisibility(visible = repeatType == RepeatType.CUSTOM.name) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "Select Days:",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val daysLabels = listOf(
                        1 to "M", 2 to "T", 3 to "W", 4 to "Th", 5 to "F", 6 to "Sa", 7 to "Su"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysLabels.forEach { (dayInt, label) ->
                            val isSelected = customDays.contains(dayInt)
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        customDays = if (isSelected) customDays - dayInt else customDays + dayInt
                                    }
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                                    .testTag("day_picker_$label")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = repeatType == RepeatType.ONCE.name) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clickable { showDatePickerDialog = true }
                        .testTag("date_picker_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Date",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    dateFormatted,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                        Text(
                            "Change",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7. ALARM TUNE SELECTOR (Problem 4)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTunePickerDialog = true }
                    .testTag("alarm_tune_selector_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Alarm Sound",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = alarmTuneName ?: "System Default",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 8. VOICE NOTE COMPONENT (RECORD, PLAY, FILE PICKER)
            VoiceNoteRecorderPlayer(
                currentVoiceNotePath = voiceNotePath,
                onVoiceNoteUpdated = { path, duration ->
                    voiceNotePath = path
                    voiceNoteDurationMs = duration
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 9. SNOOZE AND VIBRATION SETTINGS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Snooze Duration",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 10, 15).forEach { mins ->
                                val isSelected = snoozeMinutes == mins
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { snoozeMinutes = mins }
                                        .testTag("snooze_${mins}m")
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Vibrate when ringing",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Switch(
                            checked = vibrate,
                            onCheckedChange = { vibrate = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Date Picker Dialog
        if (showDatePickerDialog) {
            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                selectedDateEpochDays = millis / 86400000L
                            }
                            showDatePickerDialog = false
                        }
                    ) {
                        Text("OK", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Subject Manager Dialog
        if (showSubjectManagerDialog) {
            SubjectManagerDialog(
                subjects = subjects,
                onAddSubject = { name, hex ->
                    viewModel.addSubject(name, hex)
                },
                onDeleteSubject = { subject ->
                    viewModel.deleteSubject(subject)
                },
                onDismiss = { showSubjectManagerDialog = false }
            )
        }

        // Alarm Tune Picker Dialog (Problem 4)
        if (showTunePickerDialog) {
            AlarmTunePickerDialog(
                currentUri = alarmTuneUri,
                currentName = alarmTuneName,
                onDismiss = { showTunePickerDialog = false },
                onTuneSelected = { uri, name ->
                    alarmTuneUri = uri
                    alarmTuneName = name
                    showTunePickerDialog = false
                }
            )
        }
    }
}
