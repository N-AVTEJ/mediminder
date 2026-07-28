package com.example.notifications

import android.util.Log
import com.example.data.firebase.FirebaseSyncRepository
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID

object FcmTokenManager {

    private const val TAG = "FcmTokenManager"

    /**
     * Registers device FCM token on login/signup or app startup, saving it to user doc in Firestore repository.
     */
    fun registerDeviceFcmToken(
        customToken: String? = null,
        onTokenRetrieved: ((String) -> Unit)? = null
    ): String {
        val repo = FirebaseSyncRepository.getInstance()

        if (!customToken.isNullOrBlank()) {
            repo.registerFcmToken(customToken)
            Log.d(TAG, "Registered custom FCM token: $customToken")
            onTokenRetrieved?.invoke(customToken)
            return customToken
        }

        val fallbackToken = "fcm_device_token_" + UUID.randomUUID().toString().take(8)

        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful && !task.result.isNullOrBlank()) {
                    val token = task.result
                    repo.registerFcmToken(token)
                    Log.d(TAG, "FCM token retrieved and saved to user doc: $token")
                    onTokenRetrieved?.invoke(token)
                } else {
                    Log.w(TAG, "Fetching FCM token failed, using device fallback token", task.exception)
                    repo.registerFcmToken(fallbackToken)
                    onTokenRetrieved?.invoke(fallbackToken)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseMessaging error, falling back to local device token", e)
            repo.registerFcmToken(fallbackToken)
            onTokenRetrieved?.invoke(fallbackToken)
        }

        return fallbackToken
    }
}
