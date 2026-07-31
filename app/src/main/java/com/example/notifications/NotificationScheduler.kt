package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object NotificationScheduler {

    const val CHANNEL_ID = "medicine_reminder_channel"
    const val CHANNEL_NAME = "Medication Dose Reminders"
    const val EXTRA_DOSE_ID = "extra_dose_id"
    const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
    const val EXTRA_DOSE = "extra_dose"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timely notification reminders for taking prescribed medication doses"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleNotification(
        context: Context,
        doseId: String,
        medicineName: String,
        dose: String,
        scheduledTimeStr: String,
        triggerTimeMillis: Long
    ): String {
        val notificationId = "notif_$doseId"
        try {
            createNotificationChannel(context)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DoseNotificationReceiver::class.java).apply {
                action = DoseNotificationReceiver.ACTION_SHOW_NOTIFICATION
                putExtra(EXTRA_DOSE_ID, doseId)
                putExtra(EXTRA_MEDICINE_NAME, medicineName)
                putExtra(EXTRA_DOSE, dose)
                putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeStr)
            }

            val requestCode = doseId.hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val now = System.currentTimeMillis()
            val effectiveTrigger = if (triggerTimeMillis <= now) now + 5000L else triggerTimeMillis

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    effectiveTrigger,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    effectiveTrigger,
                    pendingIntent
                )
            }
            Log.d("NotificationScheduler", "Scheduled notification $notificationId for $medicineName at $effectiveTrigger")

            // Schedule missed dose check (e.g., 2 hours after scheduled time)
            val gracePeriodMillis = 2 * 60 * 60 * 1000L // 2 hours
            val missedIntent = Intent(context, DoseNotificationReceiver::class.java).apply {
                action = DoseNotificationReceiver.ACTION_CHECK_MISSED
                putExtra(EXTRA_DOSE_ID, doseId)
                putExtra(EXTRA_MEDICINE_NAME, medicineName)
                putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeStr)
            }
            val missedPendingIntent = PendingIntent.getBroadcast(
                context,
                doseId.hashCode() + 2,
                missedIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val missedTrigger = effectiveTrigger + gracePeriodMillis
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    missedTrigger,
                    missedPendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    missedTrigger,
                    missedPendingIntent
                )
            }
            Log.d("NotificationScheduler", "Scheduled missed check for $notificationId at $missedTrigger")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return notificationId
    }

    fun cancelNotification(context: Context, doseId: String) {
        try {
            val intent = Intent(context, DoseNotificationReceiver::class.java)
            val requestCode = doseId.hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
            
            // Also cancel the missed check alarm
            val missedIntent = Intent(context, DoseNotificationReceiver::class.java).apply {
                action = DoseNotificationReceiver.ACTION_CHECK_MISSED
            }
            val missedPendingIntent = PendingIntent.getBroadcast(
                context,
                doseId.hashCode() + 2,
                missedIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (missedPendingIntent != null) {
                alarmManager.cancel(missedPendingIntent)
                missedPendingIntent.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
