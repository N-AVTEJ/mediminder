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
     * Requirement Phase 23: Extract invite token from deep link URI, web URL, or text.
     */
    fun extractToken(input: String?): String? {
        if (input.isNullOrBlank()) return null
        val trimmed = input.trim()

        // 1. If input is already directly a token
        if (trimmed.startsWith("tok_") && !trimmed.contains("/") && !trimmed.contains("?")) {
            return trimmed
        }

        // 2. Extract token query parameter via regex (?token=... or &token=...)
        val tokenQueryRegex = Regex("""[?&]token=([^&#\s]+)""")
        val tokenMatch = tokenQueryRegex.find(trimmed)
        if (tokenMatch != null) {
            return tokenMatch.groupValues[1]
        }

        // 3. Match any tok_ token identifier pattern
        val tokSubstrRegex = Regex("""(tok_[a-zA-Z0-9_-]+)""")
        val tokSubstrMatch = tokSubstrRegex.find(trimmed)
        if (tokSubstrMatch != null) {
            return tokSubstrMatch.groupValues[1]
        }

        return if (trimmed.startsWith("tok_")) trimmed else null
    }

    /**
     * Requirement Phase 23: Generate responsive, interactive HTML Caregiver Web View.
     * Rendered in Android WebView for guardians accepting via the web link portal.
     */
    fun generateCaregiverWebHtml(
        token: String,
        patientName: String = "Eleanor Vance",
        guardianPhone: String = "+1 (555) 345-6789",
        guardianUid: String = "guardian_user_345",
        isLinked: Boolean = false
    ): String {
        val statusText = if (isLinked) "LINKED" else "PENDING INVITATION"
        val statusBg = if (isLinked) "#E8F5E9" else "#FFF3E0"
        val statusColor = if (isLinked) "#2E7D32" else "#E65100"
        val buttonDisplay = if (isLinked) "none" else "block"
        val linkedBannerDisplay = if (isLinked) "block" else "none"

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <title>MedRemind Guardian Portal</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
                    body { background-color: #F8FBFB; color: #1E293B; padding: 20px 16px; min-height: 100vh; }
                    .container { max-width: 480px; margin: 0 auto; }
                    .header { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; padding-bottom: 12px; border-bottom: 1px solid #E2E8F0; }
                    .logo-icon { width: 42px; height: 42px; border-radius: 12px; background: #00796B; display: flex; align-items: center; justify-content: center; color: white; font-size: 22px; font-weight: bold; }
                    .title-box h1 { font-size: 20px; font-weight: 700; color: #004D40; }
                    .title-box p { font-size: 13px; color: #64748B; }
                    
                    .card { background: #FFFFFF; border-radius: 16px; padding: 20px; box-shadow: 0 4px 16px rgba(0, 77, 64, 0.06); border: 1px solid #E0F2F1; margin-bottom: 18px; }
                    .patient-invite { background: #E0F2F1; border-radius: 12px; padding: 14px; margin-bottom: 16px; border-left: 4px solid #00796B; }
                    .patient-invite h2 { font-size: 16px; color: #004D40; margin-bottom: 4px; }
                    .patient-invite p { font-size: 13px; color: #334155; line-height: 1.4; }
                    
                    .badge { display: inline-block; padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 700; background: $statusBg; color: $statusColor; margin-top: 6px; }
                    
                    .info-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px dashed #CBD5E1; font-size: 14px; }
                    .info-label { color: #64748B; font-weight: 500; }
                    .info-val { color: #0F172A; font-weight: 600; text-align: right; }
                    
                    .privileges { margin: 16px 0; }
                    .priv-item { display: flex; align-items: flex-start; gap: 8px; margin-bottom: 8px; font-size: 13px; color: #475569; }
                    .priv-icon { color: #00796B; font-weight: bold; }
                    
                    .confirm-btn { width: 100%; padding: 14px; background: #00796B; color: white; border: none; border-radius: 12px; font-size: 16px; font-weight: 700; cursor: pointer; display: $buttonDisplay; transition: background 0.2s; box-shadow: 0 4px 12px rgba(0, 121, 107, 0.25); }
                    .confirm-btn:active { background: #004D40; }
                    
                    .success-banner { display: $linkedBannerDisplay; background: #E8F5E9; border-radius: 12px; padding: 16px; text-align: center; border: 1.5.dp solid #A5D6A7; }
                    .success-banner h3 { color: #2E7D32; font-size: 17px; margin-bottom: 6px; }
                    .success-banner p { color: #1B5E20; font-size: 13px; line-height: 1.4; }
                    
                    .footer-note { font-size: 12px; color: #94A3B8; text-align: center; margin-top: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo-icon">Rx</div>
                        <div class="title-box">
                            <h1>MedRemind Web Portal</h1>
                            <p>Official Caregiver & Guardian Acceptance</p>
                        </div>
                    </div>
                    
                    <div class="card">
                        <div class="patient-invite">
                            <h2>Caregiver Invitation</h2>
                            <p><strong>$patientName</strong> has invited you to join MedRemind as their designated caregiver/guardian.</p>
                            <span id="status-badge" class="badge">$statusText</span>
                        </div>
                        
                        <div class="info-row">
                            <span class="info-label">Patient</span>
                            <span class="info-val">$patientName</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Guardian Phone</span>
                            <span class="info-val">$guardianPhone</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Guardian UID</span>
                            <span class="info-val" style="font-family: monospace; font-size: 12px;">$guardianUid</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Invite Token</span>
                            <span class="info-val" style="font-family: monospace; font-size: 12px; color: #00796B;">$token</span>
                        </div>
                        
                        <div class="privileges">
                            <div class="priv-item"><span class="priv-icon">✓</span> <span>Automatic SMS & Push alerts if doses are missed</span></div>
                            <div class="priv-item"><span class="priv-icon">✓</span> <span>Real-time access to daily medication adherence schedule</span></div>
                            <div class="priv-item"><span class="priv-icon">✓</span> <span>Emergency contact access & direct calling</span></div>
                        </div>
                        
                        <div id="success-box" class="success-banner">
                            <h3>✓ Guardian Linked Successfully!</h3>
                            <p>Your account is now linked to $patientName in Firestore. You will receive immediate alerts for any missed doses.</p>
                        </div>
                        
                        <button id="btn-confirm" class="confirm-btn" onclick="onConfirmClick()">
                            Confirm & Link as Guardian
                        </button>
                    </div>
                    
                    <p class="footer-note">MedRemind Cloud • Powered by Firebase Firestore Sync</p>
                </div>
                
                <script>
                    function onConfirmClick() {
                        var token = "$token";
                        var uid = "$guardianUid";
                        
                        // 1. Call native Android WebView JavaScript interface if available
                        if (window.AndroidBridge && window.AndroidBridge.confirmAccept) {
                            window.AndroidBridge.confirmAccept(token, uid);
                        } else {
                            // Fallback custom protocol navigation
                            window.location.href = "medremind://confirm-invite?token=" + encodeURIComponent(token) + "&uid=" + encodeURIComponent(uid);
                        }
                        
                        // Update in-page UI immediately
                        document.getElementById('status-badge').innerText = 'LINKED';
                        document.getElementById('status-badge').style.background = '#E8F5E9';
                        document.getElementById('status-badge').style.color = '#2E7D32';
                        document.getElementById('btn-confirm').style.display = 'none';
                        document.getElementById('success-box').style.display = 'block';
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Javascript Interface for Android WebView communication.
     */
    class CaregiverWebBridge(
        private val onConfirm: (token: String, guardianUid: String) -> Unit
    ) {
        @android.webkit.JavascriptInterface
        fun confirmAccept(token: String, guardianUid: String) {
            onConfirm(token, guardianUid)
        }
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
