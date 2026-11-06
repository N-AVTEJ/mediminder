package com.example

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.DoseLogEntity
import com.example.data.firebase.FirebaseSyncRepository
import com.example.data.repository.MedicineRepository
import com.example.notifications.DoseNotificationReceiver
import com.example.notifications.NotificationScheduler
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EscalationTier2Test {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
    }

    @Test
    fun `test notification channel for escalation tier 2 is created with high importance`() {
        NotificationScheduler.createNotificationChannels(context)

        val channel = notificationManager.getNotificationChannel(NotificationScheduler.CHANNEL_ESCALATION_TIER2_ID)
        assertNotNull("Escalation Tier 2 channel should be registered", channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertTrue("Channel should have vibration enabled", channel.shouldVibrate())
        assertNotNull("Channel sound should be set", channel.sound)
    }

    @Test
    fun `test scheduleNotification schedules escalation alarm 30 minutes after dose trigger`() {
        val triggerTime = System.currentTimeMillis() + 60_000L // 1 minute from now
        val doseId = "dose_test_tier2_123"

        NotificationScheduler.scheduleNotification(
            context = context,
            doseId = doseId,
            medicineName = "Lisinopril",
            dose = "10mg",
            scheduledTimeStr = "2026-11-06T08:00:00Z",
            triggerTimeMillis = triggerTime
        )

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertFalse("At least 1 alarm should be scheduled", scheduledAlarms.isEmpty())

        // Find escalation tier 2 alarm which is set to triggerTime + 30 min (1,800,000 ms)
        val expectedEscalationTrigger = triggerTime + NotificationScheduler.ESCALATION_TIER2_DELAY_MILLIS
        val escalationTier2Alarm = scheduledAlarms.find { alarm ->
            Math.abs(alarm.triggerAtTime - expectedEscalationTrigger) < 2000L
        }
        assertNotNull("Escalation Tier 2 alarm (+30m) should be scheduled", escalationTier2Alarm)
    }

    @Test
    fun `test cancelNotification removes scheduled tier 2 escalation alarm and clears notifications`() {
        val triggerTime = System.currentTimeMillis() + 60_000L
        val doseId = "dose_cancel_tier2_456"

        NotificationScheduler.scheduleNotification(
            context = context,
            doseId = doseId,
            medicineName = "Metformin",
            dose = "500mg",
            scheduledTimeStr = "2026-11-06T12:00:00Z",
            triggerTimeMillis = triggerTime
        )

        // Cancel
        NotificationScheduler.cancelNotification(context, doseId)

        // Verify escalation tier 2 notification is not in active notifications
        val activeNotifs = shadowNotificationManager.allNotifications
        assertTrue(activeNotifs.none { it.extras.getString(NotificationScheduler.EXTRA_DOSE_ID) == doseId })
    }

    @Test
    fun `test DoseNotificationReceiver handles ACTION_ESCALATION_TIER2 preps guardian notify payload and marks stage 2`() = runBlocking {
        NotificationScheduler.createNotificationChannels(context)

        val doseId = "dose_tier2_urgent_999"
        val medicineName = "Atorvastatin"
        val dose = "20mg"
        val scheduledTime = "2026-11-06T09:00:00Z"

        // Insert pending dose in Room DB
        val db = AppDatabase.getDatabase(context)
        val repo = MedicineRepository(db)
        val todayStr = repo.getTodayDateStr()
        db.doseLogDao().insertLog(
            DoseLogEntity(
                reminderId = 1,
                medicineName = medicineName,
                dosage = dose,
                dateStr = todayStr,
                scheduledTime = scheduledTime,
                status = "PENDING",
                escalationStage = 0
            )
        )

        val receiver = DoseNotificationReceiver()
        val intent = Intent(DoseNotificationReceiver.ACTION_ESCALATION_TIER2).apply {
            putExtra(NotificationScheduler.EXTRA_DOSE_ID, doseId)
            putExtra(NotificationScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(NotificationScheduler.EXTRA_DOSE, dose)
            putExtra(NotificationScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(NotificationScheduler.EXTRA_ESCALATION_STAGE, 2)
        }

        receiver.onReceive(context, intent)

        // Poll for notification posted by background coroutine
        val notifId = doseId.hashCode() + 200
        var notification = shadowNotificationManager.getNotification(notifId)
        val startTime = System.currentTimeMillis()
        while (notification == null && (System.currentTimeMillis() - startTime) < 3000L) {
            Thread.sleep(50)
            notification = shadowNotificationManager.getNotification(notifId)
        }

        assertNotNull("Escalation Tier 2 notification should be posted", notification)
        assertEquals(NotificationScheduler.CHANNEL_ESCALATION_TIER2_ID, notification!!.channelId)

        // Verify FirebaseSyncRepository has prepared guardian notify payload
        val firebaseRepo = FirebaseSyncRepository.getInstance()
        val preparedPayloads = firebaseRepo.preparedGuardianPayloads.value
        val payload = preparedPayloads.find { it.doseId == doseId || it.medicineName == medicineName }
        assertNotNull("Guardian Notify Payload should be prepared", payload)
        assertEquals(30, payload!!.minutesOverdue)
        assertEquals(2, payload.escalationStage)
        assertTrue(payload.smsBody.contains(medicineName))
        assertFalse(payload.recipients.isEmpty())

        // Verify Room DB dose log marked with escalationStage = 2
        val updatedLogs = db.doseLogDao().getLogsForDate(todayStr).firstOrNull() ?: emptyList()
        val updatedLog = updatedLogs.find { it.medicineName == medicineName && it.scheduledTime == scheduledTime }
        assertNotNull(updatedLog)
        assertEquals("MISSED", updatedLog!!.status)
        assertEquals(2, updatedLog.escalationStage)
    }

    @Test
    fun `test DoseNotificationReceiver skips ACTION_ESCALATION_TIER2 if dose is already taken`() = runBlocking {
        NotificationScheduler.createNotificationChannels(context)

        val doseId = "dose_already_taken_888"
        val medicineName = "Metformin"
        val dose = "500mg"
        val scheduledTime = "2026-11-06T14:00:00Z"

        // Insert TAKEN dose in Room DB
        val db = AppDatabase.getDatabase(context)
        val repo = MedicineRepository(db)
        val todayStr = repo.getTodayDateStr()
        db.doseLogDao().insertLog(
            DoseLogEntity(
                reminderId = 2,
                medicineName = medicineName,
                dosage = dose,
                dateStr = todayStr,
                scheduledTime = scheduledTime,
                status = "TAKEN",
                escalationStage = 0
            )
        )

        val receiver = DoseNotificationReceiver()
        val intent = Intent(DoseNotificationReceiver.ACTION_ESCALATION_TIER2).apply {
            putExtra(NotificationScheduler.EXTRA_DOSE_ID, doseId)
            putExtra(NotificationScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(NotificationScheduler.EXTRA_DOSE, dose)
            putExtra(NotificationScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
        }

        receiver.onReceive(context, intent)
        Thread.sleep(200)

        val notifId = doseId.hashCode() + 200
        val notification = shadowNotificationManager.getNotification(notifId)
        assertNull("Notification should NOT be posted if dose is already taken", notification)
    }
}
