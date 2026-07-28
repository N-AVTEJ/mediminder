package com.example

import com.example.data.firebase.FirebaseSyncRepository
import com.example.data.repository.DoseScheduleGenerator
import org.junit.Assert.*
import org.junit.Test

class NotificationSchedulerTest {

    @Test
    fun `test for each dose doc local notification id is created and stored in reminder doc`() {
        val repo = FirebaseSyncRepository.getInstance()

        val schedule = DoseScheduleGenerator.generateSchedule(
            userId = "user_notif_test",
            medicineName = "Aspirin",
            dose = "81 mg",
            frequency = "Once daily",
            durationDays = 3,
            startDate = "2026-08-10"
        )

        repo.addMedicineWithDosesAndReminders(
            medicine = schedule.firebaseMedicine,
            newDoses = schedule.firebaseDoses,
            newReminders = schedule.firebaseReminders
        )

        // Verify dose count matches reminder count
        assertEquals(3, schedule.firebaseDoses.size)
        assertEquals(3, schedule.firebaseReminders.size)

        // Verify each reminder doc contains notificationId matching the dose
        for (dose in schedule.firebaseDoses) {
            val reminder = schedule.firebaseReminders.find { it.doseId == dose.id }
            assertNotNull("Reminder doc should exist for dose ${dose.id}", reminder)
            assertNotNull("Notification ID should not be null", reminder!!.notificationId)
            assertTrue("Notification ID should contain dose ID", reminder.notificationId.contains(dose.id))
            assertEquals("notif_${dose.id}", reminder.notificationId)
            assertEquals("notif_${dose.id}", reminder.notification_id)
        }
    }
}
