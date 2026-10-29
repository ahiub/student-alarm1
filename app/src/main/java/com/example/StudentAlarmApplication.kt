package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository

class StudentAlarmApplication : Application() {

    // Both are created lazily, the first time somebody really needs them (never in onCreate),
    // so the process starts faster. Room itself opens the file on a background thread.
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }

    /** Filled by MainActivity while the splash screen is visible (see readStartupSnapshot). */
    @Volatile
    var startupSnapshot: UserPreferencesRepository.StartupSnapshot? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Only the cheap, mandatory work stays here. Database / DataStore are lazy.
        // Notification channels stay synchronous: a cold-started alarm must find its channel.
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Alarm Ringing Channel (High Importance, Bypasses DND if permitted)
            val alarmChannel = NotificationChannel(
                CHANNEL_ID_ALARM_RINGING,
                "Student Alarm Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming student alarms and voice note alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                setSound(
                    Settings.System.DEFAULT_ALARM_ALERT_URI ?: Uri.EMPTY,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // Reminders / System Status Channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "General Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General study reminders and streak notifications"
            }

            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    companion object {
        const val CHANNEL_ID_ALARM_RINGING = "student_alarm_ringing_channel"
        const val CHANNEL_ID_GENERAL = "student_alarm_general_channel"

        lateinit var instance: StudentAlarmApplication
            private set
    }
}
