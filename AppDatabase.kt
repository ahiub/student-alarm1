package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(
    entities = [
        AlarmEntity::class,
        SubjectEntity::class,
        TimetableEntry::class,
        CustomMode::class,
        PomodoroPreset::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableDao(): TimetableDao
    abstract fun customModeDao(): CustomModeDao
    abstract fun pomodoroPresetDao(): PomodoroPresetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_alarm_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                val subjectDao = database.subjectDao()
                                val alarmDao = database.alarmDao()
                                val timetableDao = database.timetableDao()
                                val customModeDao = database.customModeDao()
                                val pomodoroPresetDao = database.pomodoroPresetDao()

                                // 1. Prepopulate Subjects
                                val mathId = subjectDao.insertSubject(
                                    SubjectEntity(name = "Math", colorHex = "#2EC8FF")
                                )
                                val physicsId = subjectDao.insertSubject(
                                    SubjectEntity(name = "Physics", colorHex = "#8B5CF6")
                                )
                                val englishId = subjectDao.insertSubject(
                                    SubjectEntity(name = "English", colorHex = "#EC4899")
                                )
                                val chemId = subjectDao.insertSubject(
                                    SubjectEntity(name = "Chemistry", colorHex = "#FF9F43")
                                )
                                val csId = subjectDao.insertSubject(
                                    SubjectEntity(name = "Computer Science", colorHex = "#10B981")
                                )

                                // 2. Prepopulate Starter Alarms
                                val today = LocalDate.now()
                                alarmDao.insertAlarm(
                                    AlarmEntity(
                                        title = "Morning Calculus Lecture & Homework",
                                        subject = "Math",
                                        subjectId = mathId,
                                        alarmType = AlarmType.CLASS.name,
                                        hour = 8,
                                        minute = 30,
                                        repeatType = RepeatType.WEEKDAYS.name,
                                        isEnabled = true,
                                        vibrate = true,
                                        snoozeMinutes = 10
                                    )
                                )
                                alarmDao.insertAlarm(
                                    AlarmEntity(
                                        title = "Physics Final Exam Preparation",
                                        subject = "Physics",
                                        subjectId = physicsId,
                                        alarmType = AlarmType.EXAM.name,
                                        smartReminders = "1d,3h,30m",
                                        hour = 14,
                                        minute = 0,
                                        targetDateEpochDays = today.plusDays(1).toEpochDay(),
                                        repeatType = RepeatType.ONCE.name,
                                        isEnabled = true,
                                        vibrate = true,
                                        snoozeMinutes = 5
                                    )
                                )
                                alarmDao.insertAlarm(
                                    AlarmEntity(
                                        title = "Literature Term Assignment Submission",
                                        subject = "English",
                                        subjectId = englishId,
                                        alarmType = AlarmType.ASSIGNMENT.name,
                                        smartReminders = "3h,30m",
                                        hour = 18,
                                        minute = 0,
                                        targetDateEpochDays = today.plusDays(2).toEpochDay(),
                                        repeatType = RepeatType.ONCE.name,
                                        isEnabled = true,
                                        vibrate = true,
                                        snoozeMinutes = 15
                                    )
                                )

                                // 3. Prepopulate Weekly Timetable Entries
                                timetableDao.insertEntry(
                                    TimetableEntry(
                                        dayOfWeek = 1,
                                        startTime = "08:30",
                                        endTime = "10:00",
                                        subjectId = mathId,
                                        note = "Linear Algebra & Vectors",
                                        room = "Room 302"
                                    )
                                )
                                timetableDao.insertEntry(
                                    TimetableEntry(
                                        dayOfWeek = 1,
                                        startTime = "10:30",
                                        endTime = "12:00",
                                        subjectId = physicsId,
                                        note = "Electromagnetism Lab",
                                        room = "Science Wing 105"
                                    )
                                )
                                timetableDao.insertEntry(
                                    TimetableEntry(
                                        dayOfWeek = 2,
                                        startTime = "09:00",
                                        endTime = "11:00",
                                        subjectId = csId,
                                        note = "Data Structures & Algorithms",
                                        room = "Computer Lab 4"
                                    )
                                )
                                timetableDao.insertEntry(
                                    TimetableEntry(
                                        dayOfWeek = 3,
                                        startTime = "11:00",
                                        endTime = "12:30",
                                        subjectId = chemId,
                                        note = "Organic Chemistry Reactions",
                                        room = "Hall A"
                                    )
                                )
                                timetableDao.insertEntry(
                                    TimetableEntry(
                                        dayOfWeek = 4,
                                        startTime = "14:00",
                                        endTime = "15:30",
                                        subjectId = englishId,
                                        note = "Academic Writing & Essay Workshop",
                                        room = "Library Seminar 2"
                                    )
                                )
                                timetableDao.insertEntry(
                                    TimetableEntry(
                                        dayOfWeek = 5,
                                        startTime = "09:30",
                                        endTime = "11:30",
                                        subjectId = csId,
                                        note = "Project Sprint & Code Review",
                                        room = "Computer Lab 4"
                                    )
                                )

                                // 4. Prepopulate Starter Custom Modes
                                customModeDao.insertCustomMode(
                                    CustomMode(
                                        name = "Exam Mode",
                                        enabledFeatures = "subjects,alarm_types,smart_reminders,study_timer,streak",
                                        theme = "NEON_GLOW",
                                        isSystemPreset = true
                                    )
                                )
                                customModeDao.insertCustomMode(
                                    CustomMode(
                                        name = "Lecture Mode",
                                        enabledFeatures = "subjects,alarm_types,timetable,streak",
                                        theme = "STUDY_DESK_LIGHT",
                                        isSystemPreset = true
                                    )
                                )

                                // 5. Prepopulate Starter Pomodoro Presets
                                pomodoroPresetDao.insertPreset(
                                    PomodoroPreset(
                                        name = "Standard Focus (25/5)",
                                        focusMinutes = 25,
                                        shortBreakMinutes = 5,
                                        longBreakMinutes = 15,
                                        sessionsBeforeLongBreak = 4
                                    )
                                )
                                pomodoroPresetDao.insertPreset(
                                    PomodoroPreset(
                                        name = "Exam Cram (50/10)",
                                        focusMinutes = 50,
                                        shortBreakMinutes = 10,
                                        longBreakMinutes = 20,
                                        sessionsBeforeLongBreak = 3
                                    )
                                )
                                pomodoroPresetDao.insertPreset(
                                    PomodoroPreset(
                                        name = "Short Sprint (15/3)",
                                        focusMinutes = 15,
                                        shortBreakMinutes = 3,
                                        longBreakMinutes = 10,
                                        sessionsBeforeLongBreak = 4
                                    )
                                )
                            }
                        }
                    })
                    // SAFETY: only the old development versions 1 and 2 may be wiped.
                    // From version 3 on, a schema change MUST come with a Migration, otherwise
                    // users would lose all alarms and notes on update. (See README_CHANGES.md)
                    .fallbackToDestructiveMigrationFrom(1, 2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
