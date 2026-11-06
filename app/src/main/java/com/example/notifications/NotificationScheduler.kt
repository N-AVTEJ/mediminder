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

    // Phase 25: Escalation Tier 2 Channel (Guardian Alert Overdue)
    const val CHANNEL_ESCALATION_TIER2_ID = "medicine_reminder_escalation_tier2"
    const val CHANNEL_ESCALATION_TIER2_NAME = "Guardian Alert Overdue (Tier 2 Escalation)"

    // Phase 26: Escalation Tier 3 Channel (Guardian FCM Push & Check-in Alert)
    const val CHANNEL_GUARDIAN_CHECKIN_ID = "guardian_checkin_channel"
    const val CHANNEL_GUARDIAN_CHECKIN_NAME = "Caregiver Check-in Alerts (Tier 3 Escalation)"

    const val EXTRA_DOSE_ID = "extra_dose_id"
    const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
    const val EXTRA_DOSE = "extra_dose"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    const val EXTRA_IS_ESCALATION = "extra_is_escalation"
    const val EXTRA_ESCALATION_STAGE = "extra_escalation_stage"

    // Phase 24 requirement: 15 minutes overdue trigger
    const val ESCALATION_TIER1_DELAY_MILLIS = 15 * 60 * 1000L

    // Phase 25 requirement: 30 minutes overdue trigger (Stage 2 Guardian Alert)
    const val ESCALATION_TIER2_DELAY_MILLIS = 30 * 60 * 1000L

    // Phase 26 requirement: 45 minutes overdue trigger (Stage 3 Guardian FCM Push Dispatch)
    const val ESCALATION_TIER3_DELAY_MILLIS = 45 * 60 * 1000L

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

            val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            // 2. Phase 24: Escalation Tier 1 Channel (Urgent, louder sound & intense vibration pattern)
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

            // 3. Phase 25: Escalation Tier 2 Channel (Stage 2 Guardian Alert notification)
            val escalationTier2Channel = NotificationChannel(
                CHANNEL_ESCALATION_TIER2_ID,
                CHANNEL_ESCALATION_TIER2_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alert notifying patient that dose is 30 min overdue and guardian notify payload has been prepared"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 300, 1000, 300, 1500)
                setSound(alarmSound, audioAttributes)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(escalationTier2Channel)

            // 4. Phase 26: Escalation Tier 3 Channel (Caregiver FCM Push Check-in notification)
            val guardianCheckinChannel = NotificationChannel(
                CHANNEL_GUARDIAN_CHECKIN_ID,
                CHANNEL_GUARDIAN_CHECKIN_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Direct check-in notification for linked caregivers when patient has missed medication dose"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 250, 600, 250, 900)
                setSound(alarmSound, audioAttributes)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(guardianCheckinChannel)
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

            // 3. Phase 25: Escalation Tier 2 (30 minutes after scheduled dose time: mark Stage 2 & prep guardian payload)
            val escalationTier2Intent = Intent(context, DoseNotificationReceiver::class.java).apply {
                action = DoseNotificationReceiver.ACTION_ESCALATION_TIER2
                putExtra(EXTRA_DOSE_ID, doseId)
                putExtra(EXTRA_MEDICINE_NAME, medicineName)
                putExtra(EXTRA_DOSE, dose)
                putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeStr)
                putExtra(EXTRA_ESCALATION_STAGE, 2)
            }
            val escalationTier2PendingIntent = PendingIntent.getBroadcast(
                context,
                doseId.hashCode() + 20,
                escalationTier2Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val escalationTier2Trigger = effectiveTrigger + ESCALATION_TIER2_DELAY_MILLIS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    escalationTier2Trigger,
                    escalationTier2PendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    escalationTier2Trigger,
                    escalationTier2PendingIntent
                )
            }
            Log.d("NotificationScheduler", "Scheduled Escalation Tier 2 for $doseId at $escalationTier2Trigger (+30 min)")

            // 4. Phase 26: Escalation Tier 3 (45 minutes after scheduled dose time: send FCM push to linked guardians)
            val escalationTier3Intent = Intent(context, DoseNotificationReceiver::class.java).apply {
                action = DoseNotificationReceiver.ACTION_ESCALATION_TIER3
                putExtra(EXTRA_DOSE_ID, doseId)
                putExtra(EXTRA_MEDICINE_NAME, medicineName)
                putExtra(EXTRA_DOSE, dose)
                putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeStr)
                putExtra(EXTRA_ESCALATION_STAGE, 3)
            }
            val escalationTier3PendingIntent = PendingIntent.getBroadcast(
                context,
                doseId.hashCode() + 30,
                escalationTier3Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val escalationTier3Trigger = effectiveTrigger + ESCALATION_TIER3_DELAY_MILLIS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    escalationTier3Trigger,
                    escalationTier3PendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    escalationTier3Trigger,
                    escalationTier3PendingIntent
                )
            }
            Log.d("NotificationScheduler", "Scheduled Escalation Tier 3 for $doseId at $escalationTier3Trigger (+45 min)")

            // 5. Schedule 2-hour missed dose check
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

            // 2. Cancel Escalation Tier 1 alarm (+15 min)
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

            // 3. Cancel Escalation Tier 2 alarm (+30 min)
            val escalationTier2Intent = Intent(context, DoseNotificationReceiver::class.java).apply {
                action = DoseNotificationReceiver.ACTION_ESCALATION_TIER2
            }
            val escalationTier2PendingIntent = PendingIntent.getBroadcast(
                context,
                doseId.hashCode() + 20,
                escalationTier2Intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (escalationTier2PendingIntent != null) {
                alarmManager.cancel(escalationTier2PendingIntent)
                escalationTier2PendingIntent.cancel()
            }

            // 4. Cancel Escalation Tier 3 alarm (+45 min)
            val escalationTier3Intent = Intent(context, DoseNotificationReceiver::class.java).apply {
                action = DoseNotificationReceiver.ACTION_ESCALATION_TIER3
            }
            val escalationTier3PendingIntent = PendingIntent.getBroadcast(
                context,
                doseId.hashCode() + 30,
                escalationTier3Intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (escalationTier3PendingIntent != null) {
                alarmManager.cancel(escalationTier3PendingIntent)
                escalationTier3PendingIntent.cancel()
            }

            // 5. Cancel the 2-hour missed check alarm
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

            // 6. Cancel active notification banners from NotificationManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(doseId.hashCode()) // Primary notification
            notificationManager.cancel(doseId.hashCode() + 100) // Escalation Tier 1 notification
            notificationManager.cancel(doseId.hashCode() + 200) // Escalation Tier 2 notification
            notificationManager.cancel(doseId.hashCode() + 300) // Escalation Tier 3 notification
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

    /**
     * Phase 25: Helper to test or simulate Escalation Tier 2 immediately (30-min missed dose & guardian notify payload prep).
     */
    fun triggerEscalationTier2Now(
        context: Context,
        doseId: String,
        medicineName: String,
        dose: String,
        scheduledTimeStr: String
    ) {
        val intent = Intent(context, DoseNotificationReceiver::class.java).apply {
            action = DoseNotificationReceiver.ACTION_ESCALATION_TIER2
            putExtra(EXTRA_DOSE_ID, doseId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSE, dose)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeStr)
            putExtra(EXTRA_ESCALATION_STAGE, 2)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Phase 26: Helper to test or simulate Escalation Tier 3 immediately (send FCM push to all linked guardians).
     */
    fun triggerEscalationTier3Now(
        context: Context,
        doseId: String,
        medicineName: String,
        dose: String,
        scheduledTimeStr: String
    ) {
        val intent = Intent(context, DoseNotificationReceiver::class.java).apply {
            action = DoseNotificationReceiver.ACTION_ESCALATION_TIER3
            putExtra(EXTRA_DOSE_ID, doseId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSE, dose)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeStr)
            putExtra(EXTRA_ESCALATION_STAGE, 3)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Phase 26: Post local notification simulating the caregiver's receipt of the FCM push,
     * with deep link directly to the Check-in Screen.
     */
    fun postGuardianFcmPushNotification(
        context: Context,
        record: com.example.data.firebase.GuardianFcmPushRecord?,
        messageBody: String
    ) {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val deepLinkUri = record?.deepLinkUri ?: "medremind://checkin"
        val checkInIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(deepLinkUri)).apply {
            setClass(context, com.example.MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ACTION", "CHECK_IN")
            putExtra("DEEP_LINK_URI", deepLinkUri)
            putExtra(EXTRA_MEDICINE_NAME, record?.medicineName ?: "Medication")
            putExtra(EXTRA_DOSE, record?.dose ?: "Dose")
            putExtra(EXTRA_SCHEDULED_TIME, record?.formattedTime ?: "")
            putExtra(EXTRA_DOSE_ID, record?.doseId ?: "")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (record?.doseId ?: "checkin").hashCode() + 300,
            checkInIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notif = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_GUARDIAN_CHECKIN_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Caregiver Alert: Missed Dose")
            .setContentText(messageBody)
            .setStyle(
                androidx.core.app.NotificationCompat.BigTextStyle()
                    .bigText("🚨 CAREGIVER ALERT (Tier 3 Escalation)\n\n$messageBody\n\nTap to open the Check-in screen, contact the patient, or verify dose intake.")
            )
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_call,
                "Check-In Screen",
                pendingIntent
            )
            .build()

        val notifId = (record?.doseId ?: "checkin").hashCode() + 300
        notificationManager.notify(notifId, notif)
    }
}
