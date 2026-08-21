package com.example.data.firebase

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

object GuardianInviteService {
    private const val TAG = "GuardianInviteService"

    fun generateInviteToken(): String {
        return "tok_" + UUID.randomUUID().toString().replace("-", "").take(10)
    }

    fun buildInviteLink(token: String): String {
        return "https://medremind.app/invite?token=$token"
    }

    fun buildInviteMessage(patientName: String, inviteLink: String): String {
        return "Hi! $patientName invited you as a guardian/caregiver on MedRemind. Click to link account: $inviteLink"
    }

    /**
     * Requirement Phase 22: Send SMS via Twilio API simulation & launch Android SMS Intent with token link.
     */
    fun sendSmsInvite(context: Context, guardianPhone: String, patientName: String, inviteLink: String) {
        val message = buildInviteMessage(patientName, inviteLink)
        
        // 1. Simulated Twilio Cloud SMS dispatch
        Log.d(TAG, "Twilio SMS API Request -> To: $guardianPhone | Body: $message")
        Toast.makeText(context, "Twilio SMS dispatched & opening SMS client...", Toast.LENGTH_SHORT).show()

        // 2. Launch Android System SMS Intent
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$guardianPhone")).apply {
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch SMS intent: ${e.message}")
        }
    }

    /**
     * Requirement Phase 22: Send WhatsApp link prefilled with invite token URL.
     */
    fun sendWhatsAppInvite(context: Context, guardianPhone: String, patientName: String, inviteLink: String) {
        val message = buildInviteMessage(patientName, inviteLink)
        val cleanPhone = guardianPhone.replace(Regex("[^0-9]"), "")
        val encodedMsg = try {
            URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            message
        }

        Toast.makeText(context, "Opening WhatsApp invite link...", Toast.LENGTH_SHORT).show()

        try {
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch WhatsApp intent: ${e.message}")
        }
    }
}
