package com.example.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received: $token")
        FcmTokenManager.registerDeviceFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Push notification received: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Medication Reminder"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Time for your scheduled dose."

        NotificationScheduler.createNotificationChannel(applicationContext)
    }

    companion object {
        private const val TAG = "AppFirebaseMessaging"
    }
}
