package com.example

import com.example.data.firebase.FirebaseSyncRepository
import com.example.data.firebase.MedicineFirebaseModel
import org.junit.Assert.*
import org.junit.Test

class FirebaseMedicinesCollectionTest {

    @Test
    fun `test Firestore medicines collection schema contains required fields`() {
        val med = MedicineFirebaseModel(
            id = "med_123_abc",
            userId = "usr_test_456",
            name = "Aspirin",
            dose = "81 mg",
            frequency = "Once Daily",
            durationDays = 30,
            startDate = "2026-07-26"
        )

        assertEquals("med_123_abc", med.id)
        assertEquals("usr_test_456", med.userId)
        assertEquals("Aspirin", med.name)
        assertEquals("81 mg", med.dose)
        assertEquals("Once Daily", med.frequency)
        assertEquals(30, med.durationDays)
        assertEquals("2026-07-26", med.startDate)
    }

    @Test
    fun `test repository sync stores medicines with required fields`() {
        val repo = FirebaseSyncRepository.getInstance()
        val med = MedicineFirebaseModel(
            id = "med_789_xyz",
            userId = "usr_test_789",
            name = "Metformin",
            dose = "500 mg",
            frequency = "Twice Daily",
            durationDays = 14,
            startDate = "2026-07-26"
        )

        repo.addMedicineWithDosesAndReminders(med, emptyList(), emptyList())

        val stored = repo.medicines.value.find { it.id == "med_789_xyz" }
        assertNotNull(stored)
        assertEquals("usr_test_789", stored!!.userId)
        assertEquals("Metformin", stored.name)
        assertEquals("500 mg", stored.dose)
        assertEquals("Twice Daily", stored.frequency)
        assertEquals(14, stored.durationDays)
        assertEquals("2026-07-26", stored.startDate)
    }
}
