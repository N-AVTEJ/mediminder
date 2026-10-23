package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log

object NotificationScheduler {

    // Standard Dose Reminder Channel
    const val CHANNEL_ID = "medicine_reminder_channel"
    const val CHANNEL_NAME = "Medication Dose Reminders"

    // Phase 24: Escalation Tier 1 Channel (Louder & Stronger Vibration)
    const val CHANNEL_ESCALATION_TIER1_ID = "medicine_reminder_escalation_tier1"
    const val CHANNEL_ESCALATION_TIER1_NAME = "Urgent Dose Reminders (Tier 1 Escalation)"

    const val EXTRA_DOSE_ID = "extra_dose_id"
    const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
    const val EXTRA_DOSE = "extra_dose"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    const val EXTRA_IS_ESCALATION = "extra_is_escalation"

    // Phase 24 requirement: 15 minutes overdue trigger
    const val ESCALATION_TIER1_DELAY_MILLIS = 15 * 60 * 1000L

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Standard Dose Reminder Channel
            val standardChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timely notification reminders for taking prescribed medication doses"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
            }
            manager.createNotificationChannel(standardChannel)

            // 2. Phase 24: Escalation Tier 1 Channel (Urgent, louder sound & intense vibration pattern)
            val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val escalationChannel = NotificationChannel(
                CHANNEL_ESCALATION_TIER1_ID,
                CHANNEL_ESCALATION_TIER1_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent louder notification with intense vibration triggered 15 minutes after missed dose"
                enableVibration(true)
                // Heavy, attention-grabbing vibration pattern: pause, vibrate 800ms, pause 250ms, vibrate 800ms, pause 250ms, vibrate 1200ms
                vibrationPattern = longArrayOf(0, 800, 250, 800, 250, 1200)
                setSound(alarmSound, audioAttributes)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(escalationChannel)
        }
    }

    // Backward compatibility helper
    fun createNotificationChannel(context: Context) {
        createNotificationChannels(context)
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
            createNotificationChannels(context)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // 1. Primary Dose Reminder (at scheduled time)
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

            // 2. Phase 24: Escalation Tier 1 (15 minutes after scheduled dose time)
            val escalationIntent = Intent(context, DoseNotificationReceiver::class.java).apply {
                action = DoseNotificationReceiver.ACTION_ESCALATION_TIER1
                putExtra(EXTRA_DOSE_ID, doseId)
                putExtra(EXTRA_MEDICINE_NAME, medicineName)
                putExtra(EXTRA_DOSE, dose)
                putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeStr)
            }
            val escalationPendingIntent = PendingIntent.getBroadcast(
                context,
                doseId.hashCode() + 10,
                escalationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val escalationTrigger = effectiveTrigger + ESCALATION_TIER1_DELAY_MILLIS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    escalationTrigger,
                    escalationPendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    escalationTrigger,
                    escalationPendingIntent
                )
            }
            Log.d("NotificationScheduler", "Scheduled Escalation Tier 1 for $doseId at $escalationTrigger (+15 min)")

            // 3. Schedule 2-hour missed dose check
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
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // 1. Cancel primary reminder alarm
            val intent = Intent(context, DoseNotificationReceiver::class.java)
            val requestCode = doseId.hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }

            // 2. Cancel Escalation Tier 1 alarm
            val escalationIntent = Intent(context, DoseNotificationReceiver::class.java).apply {
                action = DoseNotificationReceiver.ACTION_ESCALATION_TIER1
            }
            val escalationPendingIntent = PendingIntent.getBroadcast(
                context,
                doseId.hashCode() + 10,
                escalationIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (escalationPendingIntent != null) {
                alarmManager.cancel(escalationPendingIntent)
                escalationPendingIntent.cancel()
            }

            // 3. Cancel the 2-hour missed check alarm
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

            // 4. Cancel active notification banners from NotificationManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(doseId.hashCode()) // Primary notification
            notificationManager.cancel(doseId.hashCode() + 100) // Escalation Tier 1 notification
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Helper to test or simulate Escalation Tier 1 immediately (e.g. for testing or UI preview).
     */
    fun triggerEscalationTier1Now(
        context: Context,
        doseId: String,
        medicineName: String,
        dose: String,
        scheduledTimeStr: String
    ) {
        val intent = Intent(context, DoseNotificationReceiver::class.java).apply {
            action = DoseNotificationReceiver.ACTION_ESCALATION_TIER1
            putExtra(EXTRA_DOSE_ID, doseId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSE, dose)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeStr)
        }
        context.sendBroadcast(intent)
    }
}
