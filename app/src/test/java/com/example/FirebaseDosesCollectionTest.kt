package com.example

import com.example.data.firebase.DoseFirebaseModel
import com.example.data.firebase.FirebaseSyncRepository
import org.junit.Assert.*
import org.junit.Test

class FirebaseDosesCollectionTest {

    @Test
    fun `test Firestore doses collection model contains required fields`() {
        val dose = DoseFirebaseModel(
            id = "dose_101",
            medicineId = "med_202",
            scheduledTime = "2026-07-26T08:00:00Z",
            status = "pending"
        )

        assertEquals("dose_101", dose.id)
        assertEquals("med_202", dose.medicineId)
        assertEquals("2026-07-26T08:00:00Z", dose.scheduledTime)
        assertEquals("pending", dose.status)
    }

    @Test
    fun `test dose status updates and status transitions`() {
        val repo = FirebaseSyncRepository.getInstance()
        val dose = DoseFirebaseModel(
            id = "dose_status_test",
            medicineId = "med_test",
            scheduledTime = "2026-07-26T12:00:00Z",
            status = "pending"
        )

        repo.addMedicineWithDosesAndReminders(
            medicine = com.example.data.firebase.MedicineFirebaseModel(
                id = "med_test",
                userId = "user_1",
                name = "Ibuprofen",
                dose = "200 mg",
                frequency = "Once",
                durationDays = 1,
                startDate = "2026-07-26"
            ),
            newDoses = listOf(dose),
            newReminders = emptyList()
        )

        val storedInitial = repo.doses.value.find { it.id == "dose_status_test" }
        assertNotNull(storedInitial)
        assertEquals("pending", storedInitial!!.status)

        // Update status to taken
        repo.updateDoseStatusInFirebase("dose_status_test", "taken")
        val storedTaken = repo.doses.value.find { it.id == "dose_status_test" }
        assertEquals("taken", storedTaken!!.status)

        // Update status to missed
        repo.updateDoseStatusInFirebase("dose_status_test", "missed")
        val storedMissed = repo.doses.value.find { it.id == "dose_status_test" }
        assertEquals("missed", storedMissed!!.status)
    }
}
