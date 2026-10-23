package com.example.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.firebase.FirebaseSyncRepository
import com.example.data.repository.MedicineRepository
import com.example.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class DoseNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW_NOTIFICATION = "com.example.action.SHOW_DOSE_NOTIFICATION"
        const val ACTION_MARK_TAKEN = "com.example.action.MARK_DOSE_TAKEN"
        const val ACTION_ESCALATION_TIER1 = "com.example.action.ESCALATION_TIER1"
        const val ACTION_CHECK_MISSED = "com.example.action.CHECK_MISSED_DOSE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val doseId = intent.getStringExtra(NotificationScheduler.EXTRA_DOSE_ID) ?: ""
        val medicineName = intent.getStringExtra(NotificationScheduler.EXTRA_MEDICINE_NAME) ?: "Medication"
        val dose = intent.getStringExtra(NotificationScheduler.EXTRA_DOSE) ?: "1 Dose"
        val scheduledTime = intent.getStringExtra(NotificationScheduler.EXTRA_SCHEDULED_TIME) ?: ""

        when (action) {
            ACTION_SHOW_NOTIFICATION -> {
                showNotification(context, doseId, medicineName, dose, scheduledTime)
            }
            ACTION_ESCALATION_TIER1 -> {
                handleEscalationTier1(context, doseId, medicineName, dose, scheduledTime)
            }
            ACTION_MARK_TAKEN -> {
                markDoseTaken(context, doseId, medicineName, scheduledTime)
            }
            ACTION_CHECK_MISSED -> {
                checkMissedDose(context, doseId, medicineName, scheduledTime)
            }
        }
    }

    private fun showNotification(
        context: Context,
        doseId: String,
        medicineName: String,
        dose: String,
        scheduledTime: String
    ) {
        NotificationScheduler.createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent for tapping the notification itself (Opens MainActivity)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ACTION", ACTION_MARK_TAKEN)
            putExtra(NotificationScheduler.EXTRA_DOSE_ID, doseId)
            putExtra(NotificationScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(NotificationScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            doseId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for "Mark Taken" button action directly on notification
        val markTakenIntent = Intent(context, DoseNotificationReceiver::class.java).apply {
            action = ACTION_MARK_TAKEN
            putExtra(NotificationScheduler.EXTRA_DOSE_ID, doseId)
            putExtra(NotificationScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(NotificationScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            doseId.hashCode() + 1,
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Time to take $medicineName")
            .setContentText("Dose: $dose (${TimeUtils.fromUtcIsoStringToLocal12Hour(scheduledTime)}). Tap to mark as taken.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Mark Taken",
                markTakenPendingIntent
            )
            .build()

        notificationManager.notify(doseId.hashCode(), notification)
    }

    /**
     * Phase 24: Escalation tier 1
     * If dose missed 15 min, trigger second local notification louder/vibration on patient device.
     */
    private fun handleEscalationTier1(
        context: Context,
        doseId: String,
        medicineName: String,
        dose: String,
        scheduledTime: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Check if the dose has ALREADY been taken
                val db = AppDatabase.getDatabase(context)
                val repo = MedicineRepository(db)
                val todayStr = repo.getTodayDateStr()
                val todayLogs = db.doseLogDao().getLogsForDate(todayStr).firstOrNull() ?: emptyList()

                val targetLog = todayLogs.find { it.medicineName == medicineName && it.scheduledTime == scheduledTime }
                    ?: todayLogs.find { it.medicineName == medicineName && it.status == "PENDING" }

                if (targetLog != null && targetLog.status.equals("TAKEN", ignoreCase = true)) {
                    Log.d("DoseNotificationReceiver", "Escalation Tier 1 skipped: Dose $medicineName already taken.")
                    return@launch
                }

                // 2. Trigger louder notification with intense vibration on patient device
                showEscalationTier1Notification(context, doseId, medicineName, dose, scheduledTime)
            } catch (e: Exception) {
                // In case of database error, still deliver escalation notification for safety
                showEscalationTier1Notification(context, doseId, medicineName, dose, scheduledTime)
            }
        }
    }

    private fun showEscalationTier1Notification(
        context: Context,
        doseId: String,
        medicineName: String,
        dose: String,
        scheduledTime: String
    ) {
        NotificationScheduler.createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent for tapping notification
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ACTION", ACTION_MARK_TAKEN)
            putExtra(NotificationScheduler.EXTRA_DOSE_ID, doseId)
            putExtra(NotificationScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(NotificationScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(NotificationScheduler.EXTRA_IS_ESCALATION, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            doseId.hashCode() + 100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark Taken
        val markTakenIntent = Intent(context, DoseNotificationReceiver::class.java).apply {
            action = ACTION_MARK_TAKEN
            putExtra(NotificationScheduler.EXTRA_DOSE_ID, doseId)
            putExtra(NotificationScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(NotificationScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            doseId.hashCode() + 101,
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        // High-intensity repeating pattern: pause, vibrate 800ms, pause 250ms, vibrate 800ms, pause 250ms, vibrate 1200ms
        val strongVibrationPattern = longArrayOf(0, 800, 250, 800, 250, 1200)

        val formattedTime = if (scheduledTime.isNotBlank()) {
            TimeUtils.fromUtcIsoStringToLocal12Hour(scheduledTime)
        } else "15 minutes ago"

        val escalationNotification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ESCALATION_TIER1_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ URGENT: 15-Min Missed Dose Alert!")
            .setContentText("Missed dose: $medicineName ($dose). Scheduled at $formattedTime. Tap to mark as taken now!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("⚠️ MISSED DOSE ALERT (15 MINUTES OVERDUE)\n\nYou missed your scheduled dose of $medicineName ($dose) from $formattedTime.\n\nPlease take your medication immediately to stay adherent, or tap 'TAKE NOW' below.")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(0xFFD32F2F.toInt()) // Urgent medical red
            .setSound(alarmSound)
            .setVibrate(strongVibrationPattern)
            .setLights(android.graphics.Color.RED, 1000, 500)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "✓ TAKE NOW",
                markTakenPendingIntent
            )
            .setFullScreenIntent(contentPendingIntent, false)
            .build()

        val notifId = doseId.hashCode() + 100
        notificationManager.notify(notifId, escalationNotification)

        // Directly vibrate device to guarantee loud physical vibration
        triggerDeviceVibration(context, strongVibrationPattern)
    }

    private fun triggerDeviceVibration(context: Context, pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                if (vibrator?.hasVibrator() == true) {
                    val effect = VibrationEffect.createWaveform(pattern, -1)
                    vibrator.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createWaveform(pattern, -1)
                        vibrator.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(pattern, -1)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DoseNotificationReceiver", "Failed to trigger device vibration", e)
        }
    }

    private fun markDoseTaken(
        context: Context,
        doseId: String,
        medicineName: String,
        scheduledTime: String
    ) {
        // Cancel both primary and escalation notifications
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(doseId.hashCode())
        notificationManager.cancel(doseId.hashCode() + 100)
        NotificationScheduler.cancelNotification(context, doseId) // Cancel any pending escalation

        // 1. Update Firebase
        val firebaseRepo = FirebaseSyncRepository.getInstance()
        if (doseId.isNotBlank()) {
            firebaseRepo.updateDoseStatusInFirebase(doseId, "taken")
        }
        firebaseRepo.markDoseTakenByMedicineAndSchedule(medicineName, scheduledTime)

        // 2. Update local Room DB
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repo = MedicineRepository(db)
                val todayStr = repo.getTodayDateStr()
                val todayLogs = db.doseLogDao().getLogsForDate(todayStr).firstOrNull() ?: emptyList()

                // Match log by medicine name and time, or mark first pending log for this medicine
                val targetLog = todayLogs.find { it.medicineName == medicineName && it.scheduledTime == scheduledTime }
                    ?: todayLogs.find { it.medicineName == medicineName && it.status == "PENDING" }

                if (targetLog != null) {
                    repo.updateDoseStatus(targetLog.id, "TAKEN")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Toast.makeText(context, "Dose marked as TAKEN! Synced with Firebase.", Toast.LENGTH_SHORT).show()
    }

    private fun checkMissedDose(
        context: Context,
        doseId: String,
        medicineName: String,
        scheduledTime: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repo = MedicineRepository(db)
                val todayStr = repo.getTodayDateStr()
                val todayLogs = db.doseLogDao().getLogsForDate(todayStr).firstOrNull() ?: emptyList()

                // Find log by doseId (if it's not a generic ID) or by medicine/time
                val targetLog = todayLogs.find { it.medicineName == medicineName && it.scheduledTime == scheduledTime }
                    ?: todayLogs.find { it.medicineName == medicineName && it.status == "PENDING" }

                if (targetLog != null && targetLog.status == "PENDING") {
                    // It's still pending, mark as missed!
                    repo.updateDoseStatus(targetLog.id, "MISSED")

                    // Update firebase
                    val firebaseRepo = FirebaseSyncRepository.getInstance()
                    if (doseId.isNotBlank()) {
                        firebaseRepo.updateDoseStatusInFirebase(doseId, "missed")
                    }
                    // Generic update for Firebase doses
                    firebaseRepo.markDoseStatusByMedicineAndSchedule(medicineName, scheduledTime, "missed")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
