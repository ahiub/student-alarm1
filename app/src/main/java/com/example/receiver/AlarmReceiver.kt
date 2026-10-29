package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.StudentAlarmApplication
import com.example.alarm.AlarmScheduler
import com.example.data.local.RepeatType
import com.example.service.AlarmRingingService
import com.example.ui.ringing.AlarmRingingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)

        Log.d("AlarmReceiver", "Received action $action for alarmId $alarmId, isSnooze=$isSnooze")

        val app = context.applicationContext as StudentAlarmApplication
        val scheduler = AlarmScheduler(context)

        when (action) {
            ACTION_TRIGGER_ALARM -> {
                if (alarmId != -1L) {
                    val isSmartReminder = intent.getBooleanExtra(EXTRA_IS_SMART_REMINDER, false)
                    val smartLabel = intent.getStringExtra(EXTRA_SMART_REMINDER_LABEL) ?: ""

                    // Start foreground ringing service
                    AlarmRingingService.start(context, alarmId, isSnooze, isSmartReminder, smartLabel)

                    // Also launch full screen activity
                    val activityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                        putExtra(EXTRA_ALARM_ID, alarmId)
                        putExtra(EXTRA_IS_SMART_REMINDER, isSmartReminder)
                        putExtra(EXTRA_SMART_REMINDER_LABEL, smartLabel)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    try {
                        context.startActivity(activityIntent)
                    } catch (e: Exception) {
                        Log.e("AlarmReceiver", "Failed to launch AlarmRingingActivity", e)
                    }

                    // Handle repeat logic in background (only for main alarm trigger, not smart reminder)
                    if (!isSmartReminder) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val alarm = app.database.alarmDao().getAlarmById(alarmId)
                            if (alarm != null && !isSnooze) {
                                if (alarm.repeatType == RepeatType.ONCE.name) {
                                    app.database.alarmDao().setAlarmEnabled(alarmId, false)
                                } else {
                                    scheduler.scheduleAlarm(alarm)
                                }
                            }
                        }
                    }
                }
            }

            ACTION_STOP_ALARM -> {
                AlarmRingingService.stop(context)
                CoroutineScope(Dispatchers.IO).launch {
                    app.preferencesRepository.recordAlarmStoppedOnTime()
                }
            }

            ACTION_SNOOZE_ALARM -> {
                AlarmRingingService.stop(context)
                if (alarmId != -1L) {
                    scheduler.scheduleSnooze(alarmId, snoozeMinutes)
                }
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.aistudio.studentalarm.ACTION_TRIGGER_ALARM"
        const val ACTION_STOP_ALARM = "com.aistudio.studentalarm.ACTION_STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.aistudio.studentalarm.ACTION_SNOOZE_ALARM"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_IS_SMART_REMINDER = "extra_is_smart_reminder"
        const val EXTRA_SMART_REMINDER_LABEL = "extra_smart_reminder_label"
    }
}
