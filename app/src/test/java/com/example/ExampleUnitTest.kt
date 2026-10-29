package com.example

import com.example.alarm.AlarmScheduler
import com.example.data.local.AlarmEntity
import com.example.data.local.RepeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class ExampleUnitTest {

    @Test
    fun repeatLogic_daily_returnsTrueForAnyDay() {
        val alarm = AlarmEntity(
            title = "Math Prep",
            hour = 8,
            minute = 30,
            repeatType = RepeatType.DAILY.name
        )
        assertTrue(alarm.repeatsOnDay(DayOfWeek.MONDAY))
        assertTrue(alarm.repeatsOnDay(DayOfWeek.SATURDAY))
        assertTrue(alarm.repeatsOnDay(DayOfWeek.SUNDAY))
    }

    @Test
    fun repeatLogic_weekdays_returnsTrueOnlyMonToFri() {
        val alarm = AlarmEntity(
            title = "Physics Lecture",
            hour = 9,
            minute = 0,
            repeatType = RepeatType.WEEKDAYS.name
        )
        assertTrue(alarm.repeatsOnDay(DayOfWeek.MONDAY))
        assertTrue(alarm.repeatsOnDay(DayOfWeek.FRIDAY))
        assertFalse(alarm.repeatsOnDay(DayOfWeek.SATURDAY))
        assertFalse(alarm.repeatsOnDay(DayOfWeek.SUNDAY))
    }

    @Test
    fun repeatLogic_customDays_returnsTrueForSelectedDays() {
        val alarm = AlarmEntity(
            title = "CS Lab",
            hour = 14,
            minute = 0,
            repeatType = RepeatType.CUSTOM.name,
            repeatDays = "1,3,5" // Mon, Wed, Fri
        )
        assertTrue(alarm.repeatsOnDay(DayOfWeek.MONDAY))
        assertFalse(alarm.repeatsOnDay(DayOfWeek.TUESDAY))
        assertTrue(alarm.repeatsOnDay(DayOfWeek.WEDNESDAY))
        assertFalse(alarm.repeatsOnDay(DayOfWeek.THURSDAY))
        assertTrue(alarm.repeatsOnDay(DayOfWeek.FRIDAY))
    }

    @Test
    fun alarmScheduler_calculatesNextTriggerMillis() {
        val testTime = LocalDateTime.of(2026, 10, 29, 7, 0, 0)
        val alarm = AlarmEntity(
            title = "Morning alarm",
            hour = 8,
            minute = 0,
            repeatType = RepeatType.DAILY.name
        )
        val triggerMillis = AlarmScheduler.calculateNextTriggerMillis(alarm, testTime)
        assertTrue(triggerMillis > 0)
    }
}
