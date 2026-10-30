package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.StudentAlarmApplication
import com.example.data.local.AlarmEntity
import com.example.receiver.AlarmReceiver
import com.example.ui.ringing.AlarmRingingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AlarmRingingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var alarmMediaPlayer: MediaPlayer? = null
    private var voiceNotePlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var ringtonePlaybackJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private var isSmartReminder = false
    private var smartLabel = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L
        val isSnooze = intent?.getBooleanExtra(EXTRA_IS_SNOOZE, false) ?: false
        isSmartReminder = intent?.getBooleanExtra(EXTRA_IS_SMART_REMINDER, false) ?: false
        smartLabel = intent?.getStringExtra(EXTRA_SMART_REMINDER_LABEL) ?: ""

        if (alarmId != -1L) {
            serviceScope.launch {
                val db = (applicationContext as StudentAlarmApplication).database
                val alarm = db.alarmDao().getAlarmById(alarmId)
                if (alarm != null) {
                    currentRingingAlarm.value = alarm
                    startRingingForeground(alarm, isSnooze)
                } else {
                    stopSelf()
                }
            }
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startRingingForeground(alarm: AlarmEntity, isSnooze: Boolean) {
        val notification = buildForegroundNotification(alarm, isSnooze)
        startForeground(NOTIFICATION_ID, notification)

        // Start vibration if enabled
        if (alarm.vibrate) {
            startVibrating()
        }

        // Start Audio Playback: Alarm Tone -> Voice Note
        startAlarmAudioSequence(alarm)
    }

    private fun startAlarmAudioSequence(alarm: AlarmEntity) {
        // If alarm explicitly requested vibrate only, do not play sound
        if (alarm.alarmTuneUri == "VIBRATE_ONLY") {
            startVibrating()
            if (!alarm.voiceNotePath.isNullOrBlank()) {
                playVoiceNote(alarm.voiceNotePath)
            }
            return
        }

        try {
            val audioUri: android.net.Uri = when {
                !alarm.alarmTuneUri.isNullOrBlank() -> android.net.Uri.parse(alarm.alarmTuneUri)
                else -> {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
            }

            alarmMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, audioUri)
                isLooping = alarm.voiceNotePath.isNullOrBlank() // Loop if no voice note
                prepare()
                start()
            }

            // If a voice note exists, schedule transition to play voice note after initial alarm alert
            if (!alarm.voiceNotePath.isNullOrBlank() && File(alarm.voiceNotePath).exists()) {
                ringtonePlaybackJob = serviceScope.launch {
                    delay(3500) // Ring tone for 3.5 seconds to wake the user
                    playVoiceNote(alarm.voiceNotePath)
                }
            }
        } catch (e: Exception) {
            // User requirement: If custom audio tune is moved or deleted, fallback to Vibrate Only
            Log.w("AlarmRingingService", "Custom or default audio tune inaccessible, falling back to Vibrate only", e)
            startVibrating()
            // If alarm tone failed, try playing voice note if present
            if (!alarm.voiceNotePath.isNullOrBlank()) {
                playVoiceNote(alarm.voiceNotePath)
            }
        }
    }

    private fun playVoiceNote(filePath: String) {
        try {
            alarmMediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            alarmMediaPlayer = null

            val vFile = File(filePath)
            if (!vFile.exists()) return

            voiceNotePlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(filePath)
                isLooping = true // Loop until user stops or snoozes
                prepare()
                start()
            }
            isVoiceNotePlaying.value = true
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "Error playing voice note", e)
        }
    }

    private fun startVibrating() {
        val pattern = longArrayOf(0, 800, 400, 800, 400, 1000)
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(pattern, 0)
            }
        }
    }

    private fun stopVibrating() {
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    private fun buildForegroundNotification(alarm: AlarmEntity, isSnooze: Boolean): Notification {
        // Full screen intent to show AlarmRingingActivity over the lock screen
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarm.id.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop Action
        val stopIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_STOP_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            (alarm.id + 1000).toInt(),
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Action
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            (alarm.id + 2000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            isSnooze -> "Snoozed Alarm: ${alarm.subject}"
            isSmartReminder -> "Smart Reminder ($smartLabel): ${alarm.subject}"
            else -> "Alarm: ${alarm.subject}"
        }
        val content = if (alarm.title.isNotBlank()) alarm.title else "Time to focus!"

        return NotificationCompat.Builder(this, StudentAlarmApplication.CHANNEL_ID_ALARM_RINGING)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Snooze (${alarm.snoozeMinutes}m)", snoozePendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtonePlaybackJob?.cancel()
        stopVibrating()

        try {
            alarmMediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        alarmMediaPlayer = null

        try {
            voiceNotePlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        voiceNotePlayer = null

        currentRingingAlarm.value = null
        isVoiceNotePlaying.value = false
    }

    companion object {
        const val NOTIFICATION_ID = 4001
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
        const val EXTRA_IS_SMART_REMINDER = "extra_is_smart_reminder"
        const val EXTRA_SMART_REMINDER_LABEL = "extra_smart_reminder_label"

        val currentRingingAlarm = MutableStateFlow<AlarmEntity?>(null)
        val isVoiceNotePlaying = MutableStateFlow(false)

        fun start(
            context: Context,
            alarmId: Long,
            isSnooze: Boolean = false,
            isSmartReminder: Boolean = false,
            smartLabel: String = ""
        ) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_IS_SNOOZE, isSnooze)
                putExtra(EXTRA_IS_SMART_REMINDER, isSmartReminder)
                putExtra(EXTRA_SMART_REMINDER_LABEL, smartLabel)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AlarmRingingService::class.java)
            context.stopService(intent)
        }
    }
}
