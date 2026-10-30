package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "student_alarm_prefs")

enum class AppMode {
    STUDENT,
    COMMON,
    CUSTOM
}

enum class AppThemeEnum(val title: String) {
    NEON_GLOW("Neon Glow"),
    GLASS("Glass"),
    MIDNIGHT_DARK("Midnight Dark"),
    STUDY_DESK_LIGHT("Study Desk Light"),
    SUNRISE("Sunrise"),
    OCEAN("Ocean"),
    FOREST("Forest"),
    ROSE_SUNSET("Rose Sunset"),
    MINIMAL_WHITE("Minimal White"),
    AUTO("Auto")
}

enum class AppLanguage(val label: String) {
    EN("English"),
    URDU_ROMAN("Roman Urdu")
}

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val STREAK_COUNT = intPreferencesKey("streak_count")
        val LAST_STOPPED_EPOCH_DAY = longPreferencesKey("last_stopped_epoch_day")
        val DEFAULT_SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
        val AUTO_PLAY_VOICE_NOTE = booleanPreferencesKey("auto_play_voice_note")
        val HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
        val HAS_CHOSEN_MODE = booleanPreferencesKey("has_chosen_mode")
        val APP_MODE = stringPreferencesKey("app_mode")
        val ACTIVE_CUSTOM_MODE_ID = longPreferencesKey("active_custom_mode_id")
        val ACTIVE_CUSTOM_MODE_NAME = stringPreferencesKey("active_custom_mode_name")
        val APP_THEME = stringPreferencesKey("app_theme")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val ALARM_TONE = stringPreferencesKey("alarm_tone")
        val ALARM_TONE_URI = stringPreferencesKey("alarm_tone_uri")
        val ALARM_TONE_NAME = stringPreferencesKey("alarm_tone_name")
        val DARK_MODE_OVERRIDE = booleanPreferencesKey("dark_mode_override")

        // Custom Mode Feature Toggles
        val FEAT_SUBJECTS = booleanPreferencesKey("feat_subjects")
        val FEAT_ALARM_TYPES = booleanPreferencesKey("feat_alarm_types")
        val FEAT_SMART_REMINDERS = booleanPreferencesKey("feat_smart_reminders")
        val FEAT_TIMETABLE = booleanPreferencesKey("feat_timetable")
        val FEAT_STUDY_TIMER = booleanPreferencesKey("feat_study_timer")
        val FEAT_STREAK = booleanPreferencesKey("feat_streak")

        // Study Timer (Pomodoro) Settings
        val POMODORO_STUDY_MINS = intPreferencesKey("pomodoro_study_mins")
        val POMODORO_BREAK_MINS = intPreferencesKey("pomodoro_break_mins")
        val POMODORO_LONG_BREAK_MINS = intPreferencesKey("pomodoro_long_break_mins")
        val POMODORO_SESSIONS_BEFORE_LONG = intPreferencesKey("pomodoro_sessions_before_long")
        val POMODORO_COMPLETED_SESSIONS = intPreferencesKey("pomodoro_completed_sessions")
        val ACTIVE_POMODORO_PRESET_NAME = stringPreferencesKey("active_pomodoro_preset_name")
    }

    /**
     * Values needed to draw the very first frame correctly (theme, mode chosen or not).
     * Read once, off the main thread, while the splash screen is still showing, so the
     * UI never flashes the default theme or the mode-selection screen before the real values arrive.
     */
    data class StartupSnapshot(
        val theme: AppThemeEnum,
        val hasChosenMode: Boolean,
        val appMode: AppMode
    )

    suspend fun readStartupSnapshot(): StartupSnapshot {
        val prefs = context.dataStore.data.first()
        val theme = try {
            AppThemeEnum.valueOf(prefs[PreferencesKeys.APP_THEME] ?: AppThemeEnum.NEON_GLOW.name)
        } catch (_: Exception) { AppThemeEnum.NEON_GLOW }
        val mode = try {
            AppMode.valueOf(prefs[PreferencesKeys.APP_MODE] ?: AppMode.STUDENT.name)
        } catch (_: Exception) { AppMode.STUDENT }
        return StartupSnapshot(
            theme = theme,
            hasChosenMode = prefs[PreferencesKeys.HAS_CHOSEN_MODE] ?: false,
            appMode = mode
        )
    }

    val appModeFlow: Flow<AppMode> = context.dataStore.data.map { prefs ->
        val modeStr = prefs[PreferencesKeys.APP_MODE] ?: AppMode.STUDENT.name
        try { AppMode.valueOf(modeStr) } catch (_: Exception) { AppMode.STUDENT }
    }

    val activeCustomModeIdFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ACTIVE_CUSTOM_MODE_ID] ?: 1L
    }

    val activeCustomModeNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ACTIVE_CUSTOM_MODE_NAME] ?: "Exam Mode"
    }

    val hasChosenModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.HAS_CHOSEN_MODE] ?: false
    }

    val appThemeFlow: Flow<AppThemeEnum> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[PreferencesKeys.APP_THEME] ?: AppThemeEnum.NEON_GLOW.name
        try { AppThemeEnum.valueOf(themeStr) } catch (_: Exception) { AppThemeEnum.NEON_GLOW }
    }

    val appLanguageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        val langStr = prefs[PreferencesKeys.APP_LANGUAGE] ?: AppLanguage.EN.name
        try { AppLanguage.valueOf(langStr) } catch (_: Exception) { AppLanguage.EN }
    }

    val alarmToneFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ALARM_TONE] ?: "DEFAULT"
    }

    val alarmToneUriFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ALARM_TONE_URI]
    }

    val alarmToneNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ALARM_TONE_NAME] ?: "System Default"
    }

    val darkModeOverrideFlow: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DARK_MODE_OVERRIDE]
    }

    // Custom Mode toggles
    val featSubjectsFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FEAT_SUBJECTS] ?: true
    }
    val featAlarmTypesFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FEAT_ALARM_TYPES] ?: true
    }
    val featSmartRemindersFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FEAT_SMART_REMINDERS] ?: true
    }
    val featTimetableFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FEAT_TIMETABLE] ?: true
    }
    val featStudyTimerFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FEAT_STUDY_TIMER] ?: true
    }
    val featStreakFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FEAT_STREAK] ?: true
    }

    // Pomodoro settings
    val pomodoroStudyMinsFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.POMODORO_STUDY_MINS] ?: 25
    }
    val pomodoroBreakMinsFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.POMODORO_BREAK_MINS] ?: 5
    }
    val pomodoroLongBreakMinsFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.POMODORO_LONG_BREAK_MINS] ?: 15
    }
    val pomodoroSessionsBeforeLongFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.POMODORO_SESSIONS_BEFORE_LONG] ?: 4
    }
    val pomodoroSessionsFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.POMODORO_COMPLETED_SESSIONS] ?: 0
    }
    val activePomodoroPresetNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ACTIVE_POMODORO_PRESET_NAME] ?: "Standard Focus (25/5)"
    }

    val streakCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.STREAK_COUNT] ?: 3
    }

    val defaultSnoozeMinutesFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DEFAULT_SNOOZE_MINUTES] ?: 10
    }

    val autoPlayVoiceNoteFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.AUTO_PLAY_VOICE_NOTE] ?: true
    }

    val hasSeenWelcomeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.HAS_SEEN_WELCOME] ?: false
    }

    suspend fun recordAlarmStoppedOnTime() {
        val todayEpochDay = LocalDate.now().toEpochDay()
        context.dataStore.edit { prefs ->
            val lastDay = prefs[PreferencesKeys.LAST_STOPPED_EPOCH_DAY] ?: -1L
            val currentStreak = prefs[PreferencesKeys.STREAK_COUNT] ?: 0

            when {
                lastDay == todayEpochDay -> {}
                lastDay == todayEpochDay - 1 -> {
                    prefs[PreferencesKeys.STREAK_COUNT] = currentStreak + 1
                    prefs[PreferencesKeys.LAST_STOPPED_EPOCH_DAY] = todayEpochDay
                }
                else -> {
                    prefs[PreferencesKeys.STREAK_COUNT] = 1
                    prefs[PreferencesKeys.LAST_STOPPED_EPOCH_DAY] = todayEpochDay
                }
            }
        }
    }

    suspend fun setAppMode(mode: AppMode) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_MODE] = mode.name
            prefs[PreferencesKeys.HAS_CHOSEN_MODE] = true
        }
    }

    suspend fun setActiveCustomMode(id: Long, name: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ACTIVE_CUSTOM_MODE_ID] = id
            prefs[PreferencesKeys.ACTIVE_CUSTOM_MODE_NAME] = name
        }
    }

    suspend fun setAppTheme(theme: AppThemeEnum) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_THEME] = theme.name
        }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_LANGUAGE] = language.name
        }
    }

    suspend fun setAlarmTone(tone: String, uri: String? = null, name: String = "Default") {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ALARM_TONE] = tone
            if (uri != null) {
                prefs[PreferencesKeys.ALARM_TONE_URI] = uri
            } else {
                prefs.remove(PreferencesKeys.ALARM_TONE_URI)
            }
            prefs[PreferencesKeys.ALARM_TONE_NAME] = name
        }
    }

    suspend fun setCustomFeatures(
        subjects: Boolean,
        alarmTypes: Boolean,
        smartReminders: Boolean,
        timetable: Boolean,
        studyTimer: Boolean,
        streak: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FEAT_SUBJECTS] = subjects
            prefs[PreferencesKeys.FEAT_ALARM_TYPES] = alarmTypes
            prefs[PreferencesKeys.FEAT_SMART_REMINDERS] = smartReminders
            prefs[PreferencesKeys.FEAT_TIMETABLE] = timetable
            prefs[PreferencesKeys.FEAT_STUDY_TIMER] = studyTimer
            prefs[PreferencesKeys.FEAT_STREAK] = streak
        }
    }

    suspend fun setPomodoroDurations(
        studyMins: Int,
        breakMins: Int,
        longBreakMins: Int = 15,
        sessionsBeforeLong: Int = 4,
        presetName: String? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.POMODORO_STUDY_MINS] = studyMins
            prefs[PreferencesKeys.POMODORO_BREAK_MINS] = breakMins
            prefs[PreferencesKeys.POMODORO_LONG_BREAK_MINS] = longBreakMins
            prefs[PreferencesKeys.POMODORO_SESSIONS_BEFORE_LONG] = sessionsBeforeLong
            if (presetName != null) {
                prefs[PreferencesKeys.ACTIVE_POMODORO_PRESET_NAME] = presetName
            }
        }
    }

    suspend fun incrementPomodoroSession() {
        context.dataStore.edit { prefs ->
            val curr = prefs[PreferencesKeys.POMODORO_COMPLETED_SESSIONS] ?: 0
            prefs[PreferencesKeys.POMODORO_COMPLETED_SESSIONS] = curr + 1
        }
    }

    suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEFAULT_SNOOZE_MINUTES] = minutes
        }
    }

    suspend fun setAutoPlayVoiceNote(autoPlay: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTO_PLAY_VOICE_NOTE] = autoPlay
        }
    }

    suspend fun setHasSeenWelcome(seen: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HAS_SEEN_WELCOME] = seen
        }
    }
}
