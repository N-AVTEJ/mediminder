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
        assertTrue(firstItem.instructions.isNotBlank())
    }

    @Test
    fun `test ScannedMedicationItem data structure mutation`() {
        val item = ScannedMedicationItem(
            medicine = "Amoxicillin",
            dose = "500 mg",
            frequency = "Twice Daily",
            durationDays = 7,
            instructions = "Take after food with water."
        )

        assertEquals("Amoxicillin", item.medicine)
        assertEquals("500 mg", item.dose)
        assertEquals("Twice Daily", item.frequency)
        assertEquals(7, item.durationDays)
        assertEquals("Take after food with water.", item.instructions)

        // Mutate user editable fields
        item.medicine = "Amoxicillin Clavulanate"
        item.dose = "875 mg"
        item.instructions = "Take every 12 hours."
        assertEquals("Amoxicillin Clavulanate", item.medicine)
        assertEquals("875 mg", item.dose)
        assertEquals("Take every 12 hours.", item.instructions)
    }

    @Test
    fun `test parsing Gemini structured JSON response with single medication`() {
        val geminiRawResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "[{\"medicine\":\"Amoxicillin\",\"dose\":\"500 mg\",\"frequency\":\"Twice Daily\",\"duration_days\":10,\"instructions\":\"Take 1 capsule by mouth every 12 hours with meals. Finish entire course.\"}]"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = scanner.parseGeminiJsonResponse(geminiRawResponse)
        assertEquals(1, parsed.size)

        val item = parsed.first()
        assertEquals("Amoxicillin", item.medicine)
        assertEquals("500 mg", item.dose)
        assertEquals("Twice Daily", item.frequency)
        assertEquals(10, item.durationDays)
        assertEquals("Take 1 capsule by mouth every 12 hours with meals. Finish entire course.", item.instructions)
        assertEquals("Morning", item.timeCategory)
    }

    @Test
    fun `test parsing Gemini structured JSON with multiple medications and night timing`() {
        val geminiRawResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "[{\"medicine\":\"Lisinopril\",\"dose\":\"10 mg\",\"frequency\":\"Once Daily in Morning\",\"duration_days\":30,\"instructions\":\"Take 1 tablet every morning with water.\"},{\"medicine\":\"Atorvastatin\",\"dose\":\"20 mg\",\"frequency\":\"Once Daily at Bedtime\",\"duration_days\":30,\"instructions\":\"Take 1 tablet at bedtime. Avoid grapefruit.\"}]"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = scanner.parseGeminiJsonResponse(geminiRawResponse)
        assertEquals(2, parsed.size)

        val lisinopril = parsed[0]
        assertEquals("Lisinopril", lisinopril.medicine)
        assertEquals("10 mg", lisinopril.dose)
        assertEquals(30, lisinopril.durationDays)
        assertEquals("Morning", lisinopril.timeCategory)

        val atorvastatin = parsed[1]
        assertEquals("Atorvastatin", atorvastatin.medicine)
        assertEquals("20 mg", atorvastatin.dose)
        assertEquals("Night", atorvastatin.timeCategory)
        assertEquals("09:00 PM", atorvastatin.time)
        assertTrue(atorvastatin.instructions.contains("bedtime"))
    }

    @Test
    fun `test parsing Gemini JSON wrapped in markdown codeblock`() {
        val geminiRawResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\n[{\"medicine\":\"Metformin HCl\",\"dose\":\"500 mg\",\"frequency\":\"Twice Daily with Dinner\",\"duration_days\":14,\"instructions\":\"Take with evening meal to prevent stomach upset.\"}]\n```"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = scanner.parseGeminiJsonResponse(geminiRawResponse)
        assertEquals(1, parsed.size)
        assertEquals("Metformin HCl", parsed[0].medicine)
        assertEquals("500 mg", parsed[0].dose)
        assertEquals("Evening", parsed[0].timeCategory)
    }

    @Test
    fun `test parsing empty or invalid candidate response returns empty list`() {
        val emptyResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "[]"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = scanner.parseGeminiJsonResponse(emptyResponse)
        assertTrue(parsed.isEmpty())

        val malformedResponse = """{"error": "something went wrong"}"""
        val parsedMalformed = scanner.parseGeminiJsonResponse(malformedResponse)
        assertTrue(parsedMalformed.isEmpty())
    }
}
