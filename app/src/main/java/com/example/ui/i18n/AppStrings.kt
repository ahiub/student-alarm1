package com.example.ui.i18n

import com.example.data.preferences.AppLanguage

object AppStrings {

    fun get(key: String, language: AppLanguage): String {
        val isUrdu = language == AppLanguage.URDU_ROMAN
        return when (key) {
            "app_title" -> if (isUrdu) "Student Alarm" else "Student Alarm"
            "alarms_tab" -> if (isUrdu) "Alarms" else "Alarms"
            "calendar_tab" -> if (isUrdu) "Calendar" else "Calendar"
            "timetable_tab" -> if (isUrdu) "Time Table" else "Timetable"
            "timer_tab" -> if (isUrdu) "Study Timer" else "Study Timer"
            "settings_tab" -> if (isUrdu) "Settings" else "Settings"

            "good_morning" -> if (isUrdu) "Subah Bakhair" else "Good morning"
            "good_afternoon" -> if (isUrdu) "Dopehr Bakhair" else "Good afternoon"
            "good_evening" -> if (isUrdu) "Shaam Bakhair" else "Good evening"
            "stay_on_track" -> if (isUrdu) "Apna schedule yaad rakhein" else "Stay on track today"

            "rings_in" -> if (isUrdu) "Baje ga" else "rings in"
            "no_alarms" -> if (isUrdu) "Abhi koi alarm nahi hai" else "No alarms yet"
            "tap_plus" -> if (isUrdu) "Naya alarm shamil karne ke liye + dabayein" else "Tap + to add your study schedule & reminders"
            "days_on_time" -> if (isUrdu) "din waqt par" else "days on time"

            "today" -> if (isUrdu) "Aaj" else "Today"
            "tomorrow" -> if (isUrdu) "Kal" else "Tomorrow"
            "later" -> if (isUrdu) "Baad mein" else "Later"

            "new_alarm" -> if (isUrdu) "Naya Alarm" else "New Alarm"
            "edit_alarm" -> if (isUrdu) "Alarm Tabdeel Karein" else "Edit Alarm"
            "save" -> if (isUrdu) "Mehfooz Karein" else "Save"
            "cancel" -> if (isUrdu) "Khatam Karein" else "Cancel"
            "delete" -> if (isUrdu) "Delete Karein" else "Delete"
            "undo" -> if (isUrdu) "WAPIS" else "UNDO"

            "time" -> if (isUrdu) "Waqt" else "Time"
            "date" -> if (isUrdu) "Tareekh" else "Date"
            "subject" -> if (isUrdu) "Mazmoon (Subject)" else "Subject"
            "alarm_type" -> if (isUrdu) "Alarm Ki Qisam" else "Alarm Type"
            "smart_reminders" -> if (isUrdu) "Pehle Se Yaad Dehani (Smart Reminders)" else "Smart Reminders"
            "reminder_note" -> if (isUrdu) "Yaad Dehani Note" else "Reminder Note"
            "voice_note" -> if (isUrdu) "Voice Note (Ikhtiyari)" else "Voice Note (Optional)"
            "repeat" -> if (isUrdu) "Dohrayein" else "Repeat"
            "snooze_duration" -> if (isUrdu) "Snooze Ka Waqt" else "Snooze Duration"
            "vibrate" -> if (isUrdu) "Vibrate Karein" else "Vibrate when ringing"

            "study_mode" -> if (isUrdu) "Parhai Ka Waqt" else "Study Time"
            "break_mode" -> if (isUrdu) "Waqfa (Break)" else "Break Time"
            "start_focus" -> if (isUrdu) "Focus Shuru Karein" else "Start Focus"
            "pause" -> if (isUrdu) "Rokein" else "Pause"
            "reset" -> if (isUrdu) "Dobara Shuru Karein" else "Reset"

            "stop_alarm" -> if (isUrdu) "ALARM BAND KAREIN" else "STOP ALARM"
            "snooze" -> if (isUrdu) "Snooze Karein" else "Snooze Alarm For"

            else -> key
        }
    }
}
