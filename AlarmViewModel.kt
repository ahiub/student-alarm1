package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StudentAlarmApplication
import com.example.alarm.AlarmScheduler
import com.example.audio.AudioFileUtils
import com.example.data.local.AlarmEntity
import com.example.data.local.CustomMode
import com.example.data.local.PomodoroPreset
import com.example.data.local.RepeatType
import com.example.data.local.SubjectEntity
import com.example.data.local.TimetableEntry
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.AppMode
import com.example.data.preferences.AppThemeEnum
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class GroupedAlarms(
    val today: List<AlarmEntity> = emptyList(),
    val tomorrow: List<AlarmEntity> = emptyList(),
    val later: List<AlarmEntity> = emptyList()
)

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StudentAlarmApplication
    private val alarmDao = app.database.alarmDao()
    private val subjectDao = app.database.subjectDao()
    private val timetableDao = app.database.timetableDao()
    private val customModeDao = app.database.customModeDao()
    private val pomodoroPresetDao = app.database.pomodoroPresetDao()
    private val prefs = app.preferencesRepository
    private val scheduler = AlarmScheduler(application)

    // ALARMS
    val alarms: StateFlow<List<AlarmEntity>> = alarmDao.getAllAlarmsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // SUBJECTS
    val subjects: StateFlow<List<SubjectEntity>> = subjectDao.getAllSubjectsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // TIMETABLE
    val timetableEntries: StateFlow<List<TimetableEntry>> = timetableDao.getAllEntriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // CUSTOM MODES (Problem 3)
    val customModes: StateFlow<List<CustomMode>> = customModeDao.getAllCustomModesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCustomModeId: StateFlow<Long> = prefs.activeCustomModeIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1L)

    val activeCustomModeName: StateFlow<String> = prefs.activeCustomModeNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Exam Mode")

    // POMODORO PRESETS (Problem 2)
    val pomodoroPresets: StateFlow<List<PomodoroPreset>> = pomodoroPresetDao.getAllPresetsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePomodoroPresetName: StateFlow<String> = prefs.activePomodoroPresetNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Standard Focus (25/5)")

    // APP MODE & THEMES
    // Initial values come from the snapshot MainActivity read while the splash was showing,
    // so the first frame already has the saved theme / mode (no flash of the default theme,
    // no accidental jump to the mode-selection screen).
    private val startup = app.startupSnapshot

    val appMode: StateFlow<AppMode> = prefs.appModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), startup?.appMode ?: AppMode.STUDENT)

    val hasChosenMode: StateFlow<Boolean> = prefs.hasChosenModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), startup?.hasChosenMode ?: true)

    val appTheme: StateFlow<AppThemeEnum> = prefs.appThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), startup?.theme ?: AppThemeEnum.NEON_GLOW)

    val appLanguage: StateFlow<AppLanguage> = prefs.appLanguageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.EN)

    val alarmTone: StateFlow<String> = prefs.alarmToneFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DEFAULT")

    val alarmToneUri: StateFlow<String?> = prefs.alarmToneUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val alarmToneName: StateFlow<String> = prefs.alarmToneNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System Default")

    // Custom Mode Feature Toggles
    val featSubjects: StateFlow<Boolean> = prefs.featSubjectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val featAlarmTypes: StateFlow<Boolean> = prefs.featAlarmTypesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val featSmartReminders: StateFlow<Boolean> = prefs.featSmartRemindersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val featTimetable: StateFlow<Boolean> = prefs.featTimetableFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val featStudyTimer: StateFlow<Boolean> = prefs.featStudyTimerFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val featStreak: StateFlow<Boolean> = prefs.featStreakFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // POMODORO TIMER (Problem 2)
    val pomodoroStudyMins: StateFlow<Int> = prefs.pomodoroStudyMinsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)
    val pomodoroBreakMins: StateFlow<Int> = prefs.pomodoroBreakMinsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)
    val pomodoroLongBreakMins: StateFlow<Int> = prefs.pomodoroLongBreakMinsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)
    val pomodoroSessionsBeforeLong: StateFlow<Int> = prefs.pomodoroSessionsBeforeLongFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)
    val pomodoroCompletedSessions: StateFlow<Int> = prefs.pomodoroSessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val streakCount: StateFlow<Int> = prefs.streakCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val defaultSnoozeMinutes: StateFlow<Int> = prefs.defaultSnoozeMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    // Recently deleted alarm for Undo functionality
    private val _recentlyDeletedAlarm = MutableStateFlow<AlarmEntity?>(null)
    val recentlyDeletedAlarm: StateFlow<AlarmEntity?> = _recentlyDeletedAlarm.asStateFlow()

    // Shared audio imported from WhatsApp or other apps
    private val _pendingSharedAudioPath = MutableStateFlow<String?>(null)
    val pendingSharedAudioPath: StateFlow<String?> = _pendingSharedAudioPath.asStateFlow()

    // Next alarm and countdown
    private val _nextAlarm = MutableStateFlow<AlarmEntity?>(null)
    val nextAlarm: StateFlow<AlarmEntity?> = _nextAlarm.asStateFlow()

    private val _countdownText = MutableStateFlow<String>("")
    val countdownText: StateFlow<String> = _countdownText.asStateFlow()

    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            alarms.collect { list ->
                updateNextAlarmAndCountdown(list)
            }
        }
        startCountdownTicker()
    }

    private fun startCountdownTicker() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                updateNextAlarmAndCountdown(alarms.value)
                delay(10000)
            }
        }
    }

    private fun updateNextAlarmAndCountdown(list: List<AlarmEntity>) {
        val enabledAlarms = list.filter { it.isEnabled }
        if (enabledAlarms.isEmpty()) {
            _nextAlarm.value = null
            _countdownText.value = ""
            return
        }

        val now = LocalDateTime.now()
        val sortedUpcoming = enabledAlarms.map { alarm ->
            val triggerMillis = AlarmScheduler.calculateNextTriggerMillis(alarm, now)
            Pair(alarm, triggerMillis)
        }.sortedBy { it.second }

        val earliest = sortedUpcoming.firstOrNull()
        if (earliest != null) {
            _nextAlarm.value = earliest.first
            val nowMillis = System.currentTimeMillis()
            val diffMs = (earliest.second - nowMillis).coerceAtLeast(0)
            val duration = Duration.ofMillis(diffMs)
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60

            _countdownText.value = when {
                hours > 0 -> "rings in ${hours}h ${minutes}m"
                minutes > 0 -> "rings in ${minutes}m"
                else -> "rings in less than a minute"
            }
        } else {
            _nextAlarm.value = null
            _countdownText.value = ""
        }
    }

    val groupedAlarms: StateFlow<GroupedAlarms> = alarms.combine(_nextAlarm) { list, _ ->
        val todayList = mutableListOf<AlarmEntity>()
        val tomorrowList = mutableListOf<AlarmEntity>()
        val laterList = mutableListOf<AlarmEntity>()

        val todayDate = LocalDate.now()
        val tomorrowDate = todayDate.plusDays(1)

        for (alarm in list) {
            val triggerMillis = AlarmScheduler.calculateNextTriggerMillis(alarm)
            val triggerDate = Instant.ofEpochMilli(triggerMillis).atZone(ZoneId.systemDefault()).toLocalDate()

            when (triggerDate) {
                todayDate -> todayList.add(alarm)
                tomorrowDate -> tomorrowList.add(alarm)
                else -> laterList.add(alarm)
            }
        }

        GroupedAlarms(
            today = todayList.sortedWith(compareBy({ it.hour }, { it.minute })),
            tomorrow = tomorrowList.sortedWith(compareBy({ it.hour }, { it.minute })),
            later = laterList.sortedWith(compareBy({ it.hour }, { it.minute }))
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupedAlarms())

    // --- ALARM ACTIONS ---
    fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = enabled)
            alarmDao.updateAlarm(updated)
            if (enabled) {
                scheduler.scheduleAlarm(updated)
            } else {
                scheduler.cancelAlarm(alarm.id)
            }
        }
    }

    fun saveAlarm(alarm: AlarmEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = if (alarm.id == 0L) {
                alarmDao.insertAlarm(alarm)
            } else {
                alarmDao.updateAlarm(alarm)
                alarm.id
            }
            val scheduledAlarm = alarm.copy(id = id)
            if (scheduledAlarm.isEnabled) {
                scheduler.scheduleAlarm(scheduledAlarm)
            } else {
                scheduler.cancelAlarm(id)
            }
            onSaved(id)
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            scheduler.cancelAlarm(alarm.id)
            alarmDao.deleteAlarm(alarm)
            _recentlyDeletedAlarm.value = alarm
        }
    }

    fun undoDelete() {
        val toRestore = _recentlyDeletedAlarm.value ?: return
        viewModelScope.launch {
            alarmDao.insertAlarm(toRestore)
            if (toRestore.isEnabled) {
                scheduler.scheduleAlarm(toRestore)
            }
            _recentlyDeletedAlarm.value = null
        }
    }

    fun clearRecentlyDeleted() {
        _recentlyDeletedAlarm.value = null
    }

    // --- SUBJECT ACTIONS ---
    fun addSubject(name: String, colorHex: String) {
        viewModelScope.launch {
            subjectDao.insertSubject(SubjectEntity(name = name.trim(), colorHex = colorHex))
        }
    }

    fun updateSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            subjectDao.updateSubject(subject)
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            subjectDao.deleteSubject(subject)
        }
    }

    // --- TIMETABLE ACTIONS ---
    fun saveTimetableEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            if (entry.id == 0L) {
                timetableDao.insertEntry(entry)
            } else {
                timetableDao.updateEntry(entry)
            }
        }
    }

    fun deleteTimetableEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            timetableDao.deleteEntry(entry)
        }
    }

    // --- CUSTOM MODES (Problem 3) ---
    fun saveCustomMode(
        name: String,
        enabledFeatures: String,
        theme: String? = null,
        alarmToneUri: String? = null,
        alarmToneName: String? = null,
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val mode = CustomMode(
                name = name.trim(),
                enabledFeatures = enabledFeatures,
                theme = theme,
                alarmToneUri = alarmToneUri,
                alarmToneName = alarmToneName,
                isSystemPreset = false
            )
            val newId = customModeDao.insertCustomMode(mode)
            applyCustomMode(mode.copy(id = newId))
            onComplete(newId)
        }
    }

    fun updateCustomMode(mode: CustomMode) {
        viewModelScope.launch {
            customModeDao.updateCustomMode(mode)
            if (activeCustomModeId.value == mode.id) {
                applyCustomMode(mode)
            }
        }
    }

    fun duplicateCustomMode(mode: CustomMode) {
        viewModelScope.launch {
            val duplicate = mode.copy(
                id = 0,
                name = "${mode.name} (Copy)",
                isSystemPreset = false
            )
            val newId = customModeDao.insertCustomMode(duplicate)
            applyCustomMode(duplicate.copy(id = newId))
        }
    }

    fun deleteCustomMode(mode: CustomMode) {
        viewModelScope.launch {
            customModeDao.deleteCustomMode(mode)
            // If the deleted mode was active, switch to first remaining mode or default
            if (activeCustomModeId.value == mode.id) {
                val remaining = customModeDao.getAllCustomModesList()
                val next = remaining.firstOrNull()
                if (next != null) {
                    applyCustomMode(next)
                } else {
                    prefs.setActiveCustomMode(1L, "Default Custom")
                }
            }
        }
    }

    fun applyCustomMode(mode: CustomMode) {
        viewModelScope.launch {
            prefs.setAppMode(AppMode.CUSTOM)
            prefs.setActiveCustomMode(mode.id, mode.name)
            prefs.setCustomFeatures(
                subjects = mode.hasFeature("subjects"),
                alarmTypes = mode.hasFeature("alarm_types"),
                smartReminders = mode.hasFeature("smart_reminders"),
                timetable = mode.hasFeature("timetable"),
                studyTimer = mode.hasFeature("study_timer"),
                streak = mode.hasFeature("streak")
            )
            // Apply mode's theme if specified
            mode.theme?.let { themeStr ->
                try {
                    val themeEnum = AppThemeEnum.valueOf(themeStr)
                    prefs.setAppTheme(themeEnum)
                } catch (_: Exception) {}
            }
            // Apply mode's alarm tone if specified
            if (mode.alarmToneUri != null || mode.alarmToneName != null) {
                prefs.setAlarmTone("CUSTOM", mode.alarmToneUri, mode.alarmToneName ?: "Custom Tone")
            }
        }
    }

    // --- POMODORO PRESETS (Problem 2) ---
    fun savePomodoroPreset(
        name: String,
        focusMins: Int,
        shortBreakMins: Int,
        longBreakMins: Int,
        sessionsBeforeLong: Int,
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val preset = PomodoroPreset(
                name = name.trim(),
                focusMinutes = focusMins,
                shortBreakMinutes = shortBreakMins,
                longBreakMinutes = longBreakMins,
                sessionsBeforeLongBreak = sessionsBeforeLong
            )
            val id = pomodoroPresetDao.insertPreset(preset)
            applyPomodoroPreset(preset.copy(id = id))
            onComplete(id)
        }
    }

    fun updatePomodoroPreset(preset: PomodoroPreset) {
        viewModelScope.launch {
            pomodoroPresetDao.updatePreset(preset)
            if (activePomodoroPresetName.value == preset.name) {
                applyPomodoroPreset(preset)
            }
        }
    }

    fun deletePomodoroPreset(preset: PomodoroPreset) {
        viewModelScope.launch {
            pomodoroPresetDao.deletePreset(preset)
            if (activePomodoroPresetName.value == preset.name) {
                resetPomodoroToDefaults()
            }
        }
    }

    fun applyPomodoroPreset(preset: PomodoroPreset) {
        viewModelScope.launch {
            prefs.setPomodoroDurations(
                studyMins = preset.focusMinutes,
                breakMins = preset.shortBreakMinutes,
                longBreakMins = preset.longBreakMinutes,
                sessionsBeforeLong = preset.sessionsBeforeLongBreak,
                presetName = preset.name
            )
        }
    }

    fun resetPomodoroToDefaults() {
        viewModelScope.launch {
            prefs.setPomodoroDurations(
                studyMins = 25,
                breakMins = 5,
                longBreakMins = 15,
                sessionsBeforeLong = 4,
                presetName = "Standard Focus (25/5)"
            )
        }
    }

    // --- MODE & THEME & SETTINGS ACTIONS ---
    fun setAppMode(mode: AppMode) {
        viewModelScope.launch {
            prefs.setAppMode(mode)
        }
    }

    fun setAppTheme(theme: AppThemeEnum) {
        viewModelScope.launch {
            prefs.setAppTheme(theme)
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            prefs.setAppLanguage(language)
        }
    }

    fun setAlarmTone(tone: String, uri: String? = null, name: String = "Default") {
        viewModelScope.launch {
            prefs.setAlarmTone(tone, uri, name)
        }
    }

    fun updateCustomFeatures(
        subjects: Boolean,
        alarmTypes: Boolean,
        smartReminders: Boolean,
        timetable: Boolean,
        studyTimer: Boolean,
        streak: Boolean
    ) {
        viewModelScope.launch {
            prefs.setCustomFeatures(subjects, alarmTypes, smartReminders, timetable, studyTimer, streak)
        }
    }

    fun setPomodoroDurations(
        studyMins: Int,
        breakMins: Int,
        longBreakMins: Int = 15,
        sessionsBeforeLong: Int = 4,
        presetName: String? = null
    ) {
        viewModelScope.launch {
            prefs.setPomodoroDurations(studyMins, breakMins, longBreakMins, sessionsBeforeLong, presetName)
        }
    }

    fun incrementPomodoroSession() {
        viewModelScope.launch {
            prefs.incrementPomodoroSession()
        }
    }

    // --- SHARED AUDIO ---
    fun handleSharedAudioUri(uri: Uri) {
        viewModelScope.launch {
            val savedPath = AudioFileUtils.copyAudioUriToInternalStorage(app, uri)
            _pendingSharedAudioPath.value = savedPath
        }
    }

    fun clearPendingSharedAudio() {
        _pendingSharedAudioPath.value = null
    }

    fun canScheduleExactAlarms(): Boolean {
        return scheduler.canScheduleExactAlarms()
    }

    /**
     * Checks if a given feature should be visible based on the active mode
     */
    fun isFeatureEnabled(featureName: String): Boolean {
        return when (appMode.value) {
            AppMode.STUDENT -> true
            AppMode.COMMON -> {
                when (featureName) {
                    "subjects", "alarm_types", "smart_reminders", "timetable", "study_timer" -> false
                    "streak" -> false
                    else -> true
                }
            }
            AppMode.CUSTOM -> {
                when (featureName) {
                    "subjects" -> featSubjects.value
                    "alarm_types" -> featAlarmTypes.value
                    "smart_reminders" -> featSmartReminders.value
                    "timetable" -> featTimetable.value
                    "study_timer" -> featStudyTimer.value
                    "streak" -> featStreak.value
                    else -> true
                }
            }
        }
    }
}
