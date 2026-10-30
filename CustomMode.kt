package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_modes")
data class CustomMode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val enabledFeatures: String, // Comma-separated: "subjects,alarm_types,smart_reminders,timetable,study_timer,streak"
    val theme: String? = null,
    val alarmToneUri: String? = null,
    val alarmToneName: String? = null,
    val isSystemPreset: Boolean = false
) {
    fun hasFeature(feature: String): Boolean {
        return enabledFeatures.split(",").map { it.trim().lowercase() }.contains(feature.lowercase())
    }
}
