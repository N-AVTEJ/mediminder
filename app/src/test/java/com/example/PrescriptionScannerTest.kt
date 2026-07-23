package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.PrescriptionScanner
import com.example.data.api.ScannedMedicationItem
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrescriptionScannerTest {

    private lateinit var context: Context
    private lateinit var scanner: PrescriptionScanner

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        scanner = PrescriptionScanner(context)
        PrescriptionScanner.clearCache()
    }

    @Test
    fun `test cache map is initially empty`() {
        assertEquals(0, PrescriptionScanner.getCacheSize())
    }

    @Test
    fun `test demo fallback items generated based on hash`() {
        val hash = "test_hash_123"
        val items = scanner.generateDemoFallbackItems(hash)
        assertNotNull(items)
        assertTrue(items.isNotEmpty())
        val firstItem = items.first()
        assertTrue(firstItem.medicine.isNotBlank())
        assertTrue(firstItem.dose.isNotBlank())
        assertTrue(firstItem.durationDays > 0)
    }

    @Test
    fun `test ScannedMedicationItem data structure mutation`() {
        val item = ScannedMedicationItem(
            medicine = "Amoxicillin",
            dose = "500 mg",
            frequency = "Twice Daily",
            durationDays = 7
        )

        assertEquals("Amoxicillin", item.medicine)
        assertEquals("500 mg", item.dose)
        assertEquals("Twice Daily", item.frequency)
        assertEquals(7, item.durationDays)

        // Mutate user editable fields
        item.medicine = "Amoxicillin Clavulanate"
        item.dose = "875 mg"
        assertEquals("Amoxicillin Clavulanate", item.medicine)
        assertEquals("875 mg", item.dose)
    }
}
