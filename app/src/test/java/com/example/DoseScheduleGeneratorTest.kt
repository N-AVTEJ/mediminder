package com.example

import com.example.data.repository.DoseScheduleGenerator
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DoseScheduleGeneratorTest {

    @Test
    fun `test generating dose schedule with frequency twice daily and custom startDate`() {
        val startDate = "2026-08-01"
        val schedule = DoseScheduleGenerator.generateSchedule(
            userId = "user_test_99",
            medicineName = "Metformin",
            dose = "500 mg",
            frequency = "twice daily",
            durationDays = 5,
            startDate = startDate,
            instructions = "Take with food"
        )

        // Check medicine model
        assertEquals("Metformin", schedule.firebaseMedicine.name)
        assertEquals("user_test_99", schedule.firebaseMedicine.userId)
        assertEquals("500 mg", schedule.firebaseMedicine.dose)
        assertEquals("twice daily", schedule.firebaseMedicine.frequency)
        assertEquals(5, schedule.firebaseMedicine.durationDays)
        assertEquals(startDate, schedule.firebaseMedicine.startDate)

        // For twice daily x 5 days = 10 dose documents
        assertEquals(10, schedule.firebaseDoses.size)
        assertEquals(10, schedule.firebaseReminders.size)

        // Verify dose scheduledTime slots start with startDate
        val firstDoseTime = schedule.firebaseDoses.first().scheduledTime
        assertTrue(firstDoseTime.startsWith("2026-08-01"))

        val lastDoseTime = schedule.firebaseDoses.last().scheduledTime
        assertTrue(lastDoseTime.startsWith("2026-08-05"))

        // Check slots per day (08:00 AM and 08:00 PM)
        val day1Doses = schedule.firebaseDoses.filter { it.scheduledTime.startsWith("2026-08-01") }
        assertEquals(2, day1Doses.size)
        assertEquals("pending", day1Doses[0].status)
    }

    @Test
    fun `test frequency parsing thrice daily produces 3 slots per day`() {
        val schedule = DoseScheduleGenerator.generateSchedule(
            medicineName = "Amoxicillin",
            dose = "250 mg",
            frequency = "three times daily",
            durationDays = 3,
            startDate = "2026-09-10"
        )

        assertEquals(9, schedule.firebaseDoses.size)
        val day1 = schedule.firebaseDoses.filter { it.scheduledTime.startsWith("2026-09-10") }
        assertEquals(3, day1.size)
    }
}
