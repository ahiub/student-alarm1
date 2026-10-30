package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.StudentAlarmApplication
import com.example.alarm.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Device reboot detected: $action. Rescheduling all enabled student alarms...")
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as StudentAlarmApplication
                    val scheduler = AlarmScheduler(context)
                    val activeAlarms = app.database.alarmDao().getActiveAlarmsList()

                    Log.d("BootReceiver", "Found ${activeAlarms.size} active alarms to reschedule.")
                    for (alarm in activeAlarms) {
                        scheduler.scheduleAlarm(alarm)
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
