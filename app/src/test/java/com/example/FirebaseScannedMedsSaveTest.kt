package com.example

import com.example.data.api.ScannedMedicationItem
import com.example.data.firebase.FirebaseSyncRepository
import com.example.data.repository.DoseScheduleGenerator
import org.junit.Assert.*
import org.junit.Test

class FirebaseScannedMedsSaveTest {

    @Test
    fun `test saving scanned medicines writes to Firestore medicines collection under current user`() {
        val repo = FirebaseSyncRepository.getInstance()
        val currentUserId = repo.currentUser.value.id

        val scannedItems = listOf(
            ScannedMedicationItem(
                medicine = "Amoxicillin",
                dose = "500 mg",
                frequency = "Three times daily",
                durationDays = 7,
                instructions = "Take after meals"
            ),
            ScannedMedicationItem(
                medicine = "Lisinopril",
                dose = "10 mg",
                frequency = "Once daily",
                durationDays = 30,
                instructions = "Take in the morning"
            )
        )

        for (item in scannedItems) {
            val schedule = DoseScheduleGenerator.generateSchedule(
                userId = currentUserId,
                medicineName = item.medicine,
                dose = item.dose,
                frequency = item.frequency,
                durationDays = item.durationDays,
                instructions = item.instructions
            )

            repo.addMedicineWithDosesAndReminders(
                medicine = schedule.firebaseMedicine,
                newDoses = schedule.firebaseDoses,
                newReminders = schedule.firebaseReminders
            )
        }

        val storedMeds = repo.medicines.value
        val amox = storedMeds.find { it.name == "Amoxicillin" }
        assertNotNull(amox)
        assertEquals(currentUserId, amox!!.userId)
        assertEquals("500 mg", amox.dose)
        assertEquals("Three times daily", amox.frequency)
        assertEquals(7, amox.durationDays)

        val lisin = storedMeds.find { it.name == "Lisinopril" }
        assertNotNull(lisin)
        assertEquals(currentUserId, lisin!!.userId)
        assertEquals("10 mg", lisin.dose)
        assertEquals("Once daily", lisin.frequency)
        assertEquals(30, lisin.durationDays)
    }
}
