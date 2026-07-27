package com.example

import com.example.data.firebase.FirebaseSyncRepository
import com.example.data.firebase.ReminderFirebaseModel
import org.junit.Assert.*
import org.junit.Test

class FirebaseRemindersCollectionTest {

    @Test
    fun `test Firestore reminders collection model schema`() {
        val reminder = ReminderFirebaseModel(
            id = "rem_101",
            doseId = "dose_202",
            notifyTime = "2026-07-26T08:00:00Z",
            sent = false
        )

        assertEquals("rem_101", reminder.id)
        assertEquals("dose_202", reminder.doseId)
        assertEquals("2026-07-26T08:00:00Z", reminder.notifyTime)
        assertFalse(reminder.sent)
    }

    @Test
    fun `test updating dose status updates associated reminder sent status`() {
        val repo = FirebaseSyncRepository.getInstance()
        val doseId = "dose_for_reminder_test"
        val reminderId = "reminder_test_1"

        val dose = com.example.data.firebase.DoseFirebaseModel(
            id = doseId,
            medicineId = "med_rem_test",
            scheduledTime = "2026-07-26T10:00:00Z",
            status = "pending"
        )

        val reminder = ReminderFirebaseModel(
            id = reminderId,
            doseId = doseId,
            notifyTime = "2026-07-26T10:00:00Z",
            sent = false
        )

        repo.addMedicineWithDosesAndReminders(
            medicine = com.example.data.firebase.MedicineFirebaseModel(
                id = "med_rem_test",
                userId = "user_1",
                name = "Vitamin C",
                dose = "500 mg",
                frequency = "Daily",
                durationDays = 7,
                startDate = "2026-07-26"
            ),
            newDoses = listOf(dose),
            newReminders = listOf(reminder)
        )

        val initialReminder = repo.reminders.value.find { it.id == reminderId }
        assertNotNull(initialReminder)
        assertFalse(initialReminder!!.sent)

        // Mark dose taken
        repo.updateDoseStatusInFirebase(doseId, "taken")

        val updatedReminder = repo.reminders.value.find { it.id == reminderId }
        assertNotNull(updatedReminder)
        assertTrue(updatedReminder!!.sent)
    }
}
