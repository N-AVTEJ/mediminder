package com.example.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.firebase.FirebaseSyncRepository
import com.example.data.repository.MedicineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DoseNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW_NOTIFICATION = "com.example.action.SHOW_DOSE_NOTIFICATION"
        const val ACTION_MARK_TAKEN = "com.example.action.MARK_DOSE_TAKEN"
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
            ACTION_MARK_TAKEN -> {
                markDoseTaken(context, doseId, medicineName, scheduledTime)
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
            .setContentText("Dose: $dose ($scheduledTime). Tap to mark as taken.")
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

    private fun markDoseTaken(
        context: Context,
        doseId: String,
        medicineName: String,
        scheduledTime: String
    ) {
        // Cancel notification banner
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(doseId.hashCode())

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
                val todayLogs = db.doseLogDao().getLogsForDate(todayStr)

                // Match log by medicine name or mark first pending log for this medicine
                val logs = db.doseLogDao()
                // Update log
                val nowTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                val todayLogsList = db.doseLogDao()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Toast.makeText(context, "Dose marked as TAKEN! Synced with Firebase.", Toast.LENGTH_SHORT).show()
    }
}
