package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.pharmacy.PharmacyAffiliateService
import com.example.data.pharmacy.PharmacyProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PharmacyAffiliateServiceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `test inventory check logic`() {
        // Amoxicillin is seeded in default inventory
        assertTrue(PharmacyAffiliateService.isMedicineInInventory("Amoxicillin"))
        
        // Non-existent medicine
        assertFalse(PharmacyAffiliateService.isMedicineInInventory("UnknownDrugX99"))
    }

    @Test
    fun `test add to inventory updates check status`() {
        val newDrug = "Metformin 500mg"
        assertFalse(PharmacyAffiliateService.isMedicineInInventory(newDrug))

        PharmacyAffiliateService.addMedicineToInventory(newDrug, quantity = 30)
        assertTrue(PharmacyAffiliateService.isMedicineInInventory(newDrug))
    }

    @Test
    fun `test launchPharmacyPurchase logs click event to affiliate_clicks table`() {
        val initialClicksCount = PharmacyAffiliateService.affiliateClicksTable.value.size

        PharmacyAffiliateService.launchPharmacyPurchase(
            context = context,
            medicineName = "Atorvastatin",
            provider = PharmacyProvider.ONE_MG,
            userId = "user_test_42"
        )

        val updatedClicks = PharmacyAffiliateService.affiliateClicksTable.value
        assertEquals(initialClicksCount + 1, updatedClicks.size)

        val lastClick = updatedClicks.last()
        assertEquals("user_test_42", lastClick.userId)
        assertEquals("Atorvastatin", lastClick.medicine)
        assertEquals("1mg", lastClick.pharmacy)
        assertTrue(lastClick.timestamp.isNotBlank())
    }
}
