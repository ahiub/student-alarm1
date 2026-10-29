package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.AlarmEntity
import com.example.MainActivity
import com.example.data.local.RepeatType
import com.example.receiver.AlarmReceiver
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Checks if the app is authorized to schedule exact alarms (Android 12+)
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Schedules an exact alarm with RTC_WAKEUP so it fires even if the phone is asleep or locked
     */
    fun scheduleAlarm(alarm: AlarmEntity) {
        if (!alarm.isEnabled) {
            cancelAlarm(alarm.id)
            return
        }

        val triggerTimeMillis = calculateNextTriggerMillis(alarm)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Main alarm: setAlarmClock() makes Android show its own little alarm-clock icon in the
        // status bar while an alarm is set, and it is exempt from battery/Doze delays.
        scheduleAlarmClock(triggerTimeMillis, pendingIntent)

        // Schedule Smart Reminders for Assignment / Exam alarms
        scheduleSmartReminders(alarm, triggerTimeMillis)
    }

    private fun scheduleSmartReminders(alarm: AlarmEntity, primaryTriggerMillis: Long) {
        val now = System.currentTimeMillis()
        val reminders = alarm.smartReminders.split(",").map { it.trim() }

        // 1 Day Before
        if (reminders.contains("1d")) {
            val oneDayBefore = primaryTriggerMillis - (24 * 60 * 60 * 1000L)
            if (oneDayBefore > now) {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmReceiver.ACTION_TRIGGER_ALARM
                    putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
                    putExtra(AlarmReceiver.EXTRA_IS_SMART_REMINDER, true)
                    putExtra(AlarmReceiver.EXTRA_SMART_REMINDER_LABEL, "1 Day Before")
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    (alarm.id * 100 + 1).toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                scheduleExact(oneDayBefore, pi)
            }
        }

        // 3 Hours Before
        if (reminders.contains("3h")) {
            val threeHoursBefore = primaryTriggerMillis - (3 * 60 * 60 * 1000L)
            if (threeHoursBefore > now) {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmReceiver.ACTION_TRIGGER_ALARM
                    putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
                    putExtra(AlarmReceiver.EXTRA_IS_SMART_REMINDER, true)
                    putExtra(AlarmReceiver.EXTRA_SMART_REMINDER_LABEL, "3 Hours Before")
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    (alarm.id * 100 + 2).toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                scheduleExact(threeHoursBefore, pi)
            }
        }

        // 30 Minutes Before
        if (reminders.contains("30m")) {
            val thirtyMinsBefore = primaryTriggerMillis - (30 * 60 * 1000L)
            if (thirtyMinsBefore > now) {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmReceiver.ACTION_TRIGGER_ALARM
                    putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
                    putExtra(AlarmReceiver.EXTRA_IS_SMART_REMINDER, true)
                    putExtra(AlarmReceiver.EXTRA_SMART_REMINDER_LABEL, "30 Minutes Before")
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    (alarm.id * 100 + 3).toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                scheduleExact(thirtyMinsBefore, pi)
            }
        }
    }

    /**
     * Uses AlarmManager.setAlarmClock(). Tapping the status-bar alarm icon / lock-screen
     * "next alarm" opens the app (showIntent). Works without the SCHEDULE_EXACT_ALARM permission.
     */
    private fun scheduleAlarmClock(timeMillis: Long, operation: PendingIntent) {
        try {
            val showIntent = PendingIntent.getActivity(
                context,
                SHOW_APP_REQUEST_CODE,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(timeMillis, showIntent), operation)
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException in setAlarmClock, falling back", e)
            scheduleExact(timeMillis, operation)
        }
    }

    private fun scheduleExact(timeMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException scheduling exact alarm", e)
        }
    }

    /**
     * Cancels an existing scheduled alarm and its smart reminders
     */
    fun cancelAlarm(alarmId: Long) {
        // Cancel main alarm
        cancelPendingIntent(alarmId.toInt())
        // Cancel sub smart reminders
        cancelPendingIntent((alarmId * 100 + 1).toInt())
        cancelPendingIntent((alarmId * 100 + 2).toInt())
        cancelPendingIntent((alarmId * 100 + 3).toInt())
        // Cancel snooze
        cancelPendingIntent((alarmId + 90000).toInt())
    }

    private fun cancelPendingIntent(requestCode: Int) {
        // Must use the SAME action as when scheduling, otherwise Android treats it as a
        // different PendingIntent and the old alarm (and its status-bar icon) is never removed.
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Schedules a temporary snooze alarm
     */
    fun scheduleSnooze(alarmId: Long, snoozeMinutes: Int) {
        val triggerTimeMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_IS_SNOOZE, true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId + 90000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExact(triggerTimeMillis, pendingIntent)
    }

    companion object {
        private const val SHOW_APP_REQUEST_CODE = 7001

        fun calculateNextTriggerMillis(alarm: AlarmEntity, fromTime: LocalDateTime = LocalDateTime.now()): Long {
            val zone = ZoneId.systemDefault()

            if (alarm.targetDateEpochDays != null && alarm.repeatType == RepeatType.ONCE.name) {
                val targetDate = LocalDate.ofEpochDay(alarm.targetDateEpochDays)
                val targetDateTime = targetDate.atTime(alarm.hour, alarm.minute, 0)
                if (targetDateTime.isAfter(fromTime)) {
                    return targetDateTime.atZone(zone).toInstant().toEpochMilli()
                }
            }

            val todayTargetTime = fromTime.toLocalDate().atTime(alarm.hour, alarm.minute, 0)

            when (alarm.repeatType) {
                RepeatType.ONCE.name -> {
                    val nextDateTime = if (todayTargetTime.isAfter(fromTime)) todayTargetTime else todayTargetTime.plusDays(1)
                    return nextDateTime.atZone(zone).toInstant().toEpochMilli()
                }

                RepeatType.DAILY.name -> {
                    val nextDateTime = if (todayTargetTime.isAfter(fromTime)) todayTargetTime else todayTargetTime.plusDays(1)
                    return nextDateTime.atZone(zone).toInstant().toEpochMilli()
                }

                RepeatType.WEEKDAYS.name -> {
                    var candidate = todayTargetTime
                    for (i in 0..7) {
                        val dayOfWeek = candidate.dayOfWeek
                        val isWeekday = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY
                        if (isWeekday && candidate.isAfter(fromTime)) {
                            return candidate.atZone(zone).toInstant().toEpochMilli()
                        }
                        candidate = candidate.plusDays(1)
                    }
                    return candidate.atZone(zone).toInstant().toEpochMilli()
                }

                RepeatType.CUSTOM.name -> {
                    val activeDays = alarm.repeatDays.split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .toSet()

                    if (activeDays.isEmpty()) {
                        val nextDateTime = if (todayTargetTime.isAfter(fromTime)) todayTargetTime else todayTargetTime.plusDays(1)
                        return nextDateTime.atZone(zone).toInstant().toEpochMilli()
                    }

                    var candidate = todayTargetTime
                    for (i in 0..7) {
                        val dayVal = candidate.dayOfWeek.value
                        if (dayVal in activeDays && candidate.isAfter(fromTime)) {
                            return candidate.atZone(zone).toInstant().toEpochMilli()
                        }
                        candidate = candidate.plusDays(1)
                    }
                    return candidate.atZone(zone).toInstant().toEpochMilli()
                }

                else -> {
                    val nextDateTime = if (todayTargetTime.isAfter(fromTime)) todayTargetTime else todayTargetTime.plusDays(1)
                    return nextDateTime.atZone(zone).toInstant().toEpochMilli()
                }
            }
        }
    }
}
