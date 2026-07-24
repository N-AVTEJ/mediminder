package com.example.data.pharmacy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Supabase Inventory Entity (Table: "inventory")
 */
data class SupabaseInventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "user_default_1",
    val medicineName: String,
    val quantity: Int = 10,
    val inStock: Boolean = true
)

/**
 * Supabase Affiliate Clicks Logging Entity (Table: "affiliate_clicks")
 */
data class SupabaseAffiliateClick(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "user_default_1",
    val medicine: String,
    val pharmacy: String,
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
)

enum class PharmacyProvider(
    val displayName: String,
    val packageName: String,
    val affiliateTag: String
) {
    ONE_MG("1mg", "com.aranoah.healthkart.plus", "medremind_aff_1mg"),
    PHARMEASY("PharmEasy", "com.phoneme.pharmeasy", "medremind_aff_pe"),
    NETMEDS("Netmeds", "com.netmeds.marketplace", "medremind_aff_nm")
}

object PharmacyAffiliateService {

    private const val TAG = "PharmacyAffiliate"

    // Mock Supabase tables in-memory state
    private val _inventoryTable = MutableStateFlow<List<SupabaseInventoryItem>>(
        listOf(
            SupabaseInventoryItem(medicineName = "Amoxicillin", quantity = 14, inStock = true),
            SupabaseInventoryItem(medicineName = "Lisinopril", quantity = 30, inStock = true)
        )
    )
    val inventoryTable: StateFlow<List<SupabaseInventoryItem>> = _inventoryTable.asStateFlow()

    private val _affiliateClicksTable = MutableStateFlow<List<SupabaseAffiliateClick>>(emptyList())
    val affiliateClicksTable: StateFlow<List<SupabaseAffiliateClick>> = _affiliateClicksTable.asStateFlow()

    fun isMedicineInInventory(medicineName: String): Boolean {
        if (medicineName.isBlank()) return true
        val cleanName = medicineName.trim().lowercase(Locale.ROOT)
        return _inventoryTable.value.any {
            it.inStock && it.medicineName.trim().lowercase(Locale.ROOT).contains(cleanName) ||
                    cleanName.contains(it.medicineName.trim().lowercase(Locale.ROOT))
        }
    }

    fun addMedicineToInventory(medicineName: String, quantity: Int = 10) {
        val current = _inventoryTable.value.toMutableList()
        current.add(
            SupabaseInventoryItem(
                medicineName = medicineName,
                quantity = quantity,
                inStock = true
            )
        )
        _inventoryTable.value = current
    }

    /**
     * Requirement:
     * Opens deep link to pharmacy apps (1mg, PharmEasy, Netmeds) prefilled with medicine name search,
     * using affiliate tracking param in URL.
     * If app not installed, fallback to web link.
     * Log click event to Supabase table "affiliate_clicks" (user_id, medicine, pharmacy, timestamp)
     */
    fun launchPharmacyPurchase(
        context: Context,
        medicineName: String,
        provider: PharmacyProvider,
        userId: String = "user_default_1"
    ) {
        // 1. Log click event to Supabase table "affiliate_clicks"
        logAffiliateClick(userId, medicineName, provider.displayName)

        // 2. Build affiliate deep link & web fallback URLs
        val encodedName = URLEncoder.encode(medicineName.trim(), StandardCharsets.UTF_8.toString())

        val webUrl = when (provider) {
            PharmacyProvider.ONE_MG ->
                "https://www.1mg.com/search/all?name=$encodedName&utm_source=affiliate&aff_id=${provider.affiliateTag}"
            PharmacyProvider.PHARMEASY ->
                "https://pharmeasy.in/search/all?name=$encodedName&ref=${provider.affiliateTag}"
            PharmacyProvider.NETMEDS ->
                "https://www.netmeds.com/catalogsearch/result/$encodedName/all?aff=${provider.affiliateTag}"
        }

        val appDeepLinkUri = when (provider) {
            PharmacyProvider.ONE_MG ->
                Uri.parse("onemg://search?q=$encodedName&aff=${provider.affiliateTag}")
            PharmacyProvider.PHARMEASY ->
                Uri.parse("pharmeasy://search?q=$encodedName&aff=${provider.affiliateTag}")
            PharmacyProvider.NETMEDS ->
                Uri.parse("netmeds://search?q=$encodedName&aff=${provider.affiliateTag}")
        }

        // Try launching native app first, fallback to browser
        val pm = context.packageManager
        val isAppInstalled = try {
            pm.getPackageInfo(provider.packageName, 0)
            true
        } catch (e: Exception) {
            false
        }

        val intentToLaunch = if (isAppInstalled) {
            Intent(Intent.ACTION_VIEW, appDeepLinkUri).apply {
                setPackage(provider.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        try {
            context.startActivity(intentToLaunch)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching pharmacy link: ${e.message}")
            // Fallback to web browser
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }

    private fun logAffiliateClick(userId: String, medicine: String, pharmacy: String) {
        val click = SupabaseAffiliateClick(
            userId = userId,
            medicine = medicine,
            pharmacy = pharmacy
        )
        val current = _affiliateClicksTable.value.toMutableList()
        current.add(click)
        _affiliateClicksTable.value = current
        Log.d(TAG, "Logged click to Supabase table 'affiliate_clicks': $click")
    }
}
