package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate

enum class RepeatType {
    ONCE,
    DAILY,
    WEEKDAYS,
    CUSTOM
}

enum class AlarmType(val label: String) {
    NORMAL("Normal"),
    ASSIGNMENT("Assignment"),
    EXAM("Exam"),
    CLASS("Class")
}

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,                  // Note or description, e.g. "Math assignment due"
    val subject: String = "Math",       // Legacy subject name or fallback
    val subjectId: Long? = null,        // Foreign key reference to SubjectEntity
    val alarmType: String = AlarmType.NORMAL.name, // NORMAL, ASSIGNMENT, EXAM, CLASS
    val smartReminders: String = "",    // Comma-separated: "1d", "3h", "30m"
    val hour: Int,                      // 0 - 23
    val minute: Int,                    // 0 - 59
    val targetDateEpochDays: Long? = null, // Specific date selected (epoch days) for ONCE repeat
    val repeatType: String = RepeatType.ONCE.name,
    val repeatDays: String = "",        // Comma-separated 1..7 (1=Monday, 7=Sunday)
    val isEnabled: Boolean = true,
    val voiceNotePath: String? = null,  // Absolute path to audio file if recorded or attached
    val voiceNoteDurationMs: Long = 0L,
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 10,
    val alarmTuneUri: String? = null,   // Custom tune URI (content:// or system ringtone)
    val alarmTuneName: String? = null,  // Human readable title of the tune
    val createdAt: Long = System.currentTimeMillis()
) {
    fun repeatsOnDay(dayOfWeek: DayOfWeek): Boolean {
        return when (repeatType) {
            RepeatType.DAILY.name -> true
            RepeatType.WEEKDAYS.name -> dayOfWeek.value in 1..5
            RepeatType.CUSTOM.name -> {
                val days = repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
                dayOfWeek.value in days
            }
            RepeatType.ONCE.name -> {
                if (targetDateEpochDays != null) {
                    LocalDate.ofEpochDay(targetDateEpochDays).dayOfWeek == dayOfWeek
                } else {
                    true
                }
            }
            else -> false
        }
    }

    fun hasSmartReminder(interval: String): Boolean {
        return smartReminders.split(",").map { it.trim() }.contains(interval)
    }

    val repeatSummary: String
        get() = when (repeatType) {
            RepeatType.DAILY.name -> "Every day"
            RepeatType.WEEKDAYS.name -> "Mon - Fri"
            RepeatType.CUSTOM.name -> {
                val dayNames = listOf("M", "T", "W", "Th", "F", "Sa", "Su")
                val activeDays = repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
                if (activeDays.isEmpty()) "Once"
                else activeDays.sorted().mapNotNull { if (it in 1..7) dayNames[it - 1] else null }.joinToString(" ")
            }
            else -> {
                if (targetDateEpochDays != null) {
                    val date = LocalDate.ofEpochDay(targetDateEpochDays)
                    "${date.dayOfWeek.name.take(3)}, ${date.month.name.take(3)} ${date.dayOfMonth}"
                } else {
                    "Ring once"
                }
            }
        }
}
