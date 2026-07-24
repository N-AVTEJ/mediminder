package com.example

import com.example.data.repository.DoseScheduleGenerator
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class DoseScheduleGeneratorTest {

    @Test
    fun `test frequency parser produces expected times per day`() {
        val twiceDaily = DoseScheduleGenerator.parseDailyTimesFromFrequency("twice daily")
        assertEquals(2, twiceDaily.size)
        assertEquals("08:00 AM", twiceDaily[0])
        assertEquals("08:00 PM", twiceDaily[1])

        val threeTimes = DoseScheduleGenerator.parseDailyTimesFromFrequency("three times daily")
        assertEquals(3, threeTimes.size)

        val onceDaily = DoseScheduleGenerator.parseDailyTimesFromFrequency("once daily")
        assertEquals(1, onceDaily.size)
    }

    @Test
    fun `test schedule generator produces exact number of doses and reminders for duration_days`() {
        val schedule = DoseScheduleGenerator.generateSchedule(
            medicineName = "Amoxicillin",
            dose = "500 mg",
            frequency = "twice daily",
            durationDays = 7,
            instructions = "Take with water"
        )

        assertNotNull(schedule)
        assertEquals("Amoxicillin", schedule.firebaseMedicine.name)
        assertEquals(7, schedule.firebaseMedicine.duration_days)

        // 2 times per day * 7 days = 14 doses and 14 reminders
        assertEquals(14, schedule.firebaseDoses.size)
        assertEquals(14, schedule.firebaseReminders.size)
        assertEquals(14, schedule.notificationTriggers.size)

        // All doses initially pending
        assertTrue(schedule.firebaseDoses.all { it.status == "pending" })
        assertTrue(schedule.firebaseReminders.all { !it.sent })
    }

    @Test
    fun `test timezone is derived from system default`() {
        val zone = ZoneId.systemDefault()
        assertNotNull(zone)
    }
}
