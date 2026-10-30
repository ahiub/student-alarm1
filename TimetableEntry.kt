package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetable")
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayOfWeek: Int,             // 1=Monday, 2=Tuesday, ..., 7=Sunday
    val startTime: String,          // "09:00"
    val endTime: String = "",       // "10:30"
    val subjectId: Long? = null,
    val note: String = "",          // e.g. "Room 304 - Bring Textbook"
    val room: String = ""           // e.g. "Hall B"
)
