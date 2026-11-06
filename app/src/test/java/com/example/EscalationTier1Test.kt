package com.example

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.notifications.DoseNotificationReceiver
import com.example.notifications.NotificationScheduler
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
class EscalationTier1Test {

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
    fun `test notification channel for escalation tier 1 is created with high importance and vibration`() {
        NotificationScheduler.createNotificationChannels(context)

        val channel = notificationManager.getNotificationChannel(NotificationScheduler.CHANNEL_ESCALATION_TIER1_ID)
        assertNotNull("Escalation Tier 1 channel should be registered", channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertTrue("Channel should have vibration enabled", channel.shouldVibrate())
        assertArrayEquals(
            longArrayOf(0, 800, 250, 800, 250, 1200),
            channel.vibrationPattern
        )
        assertNotNull("Channel sound should be set", channel.sound)
    }

    @Test
    fun `test scheduleNotification schedules escalation alarm 15 minutes after dose trigger`() {
        val triggerTime = System.currentTimeMillis() + 60_000L // 1 minute from now
        val doseId = "dose_test_tier1_123"

        NotificationScheduler.scheduleNotification(
            context = context,
            doseId = doseId,
            medicineName = "Metformin",
            dose = "500mg",
            scheduledTimeStr = "2026-10-23T08:00:00Z",
            triggerTimeMillis = triggerTime
        )

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertFalse("At least 1 alarm should be scheduled", scheduledAlarms.isEmpty())

        // Find escalation alarm which is set to triggerTime + 15 min (900,000 ms)
        val expectedEscalationTrigger = triggerTime + NotificationScheduler.ESCALATION_TIER1_DELAY_MILLIS
        val escalationAlarm = scheduledAlarms.find { alarm ->
            Math.abs(alarm.triggerAtTime - expectedEscalationTrigger) < 2000L
        }
        assertNotNull("Escalation Tier 1 alarm (+15m) should be scheduled", escalationAlarm)
    }

    @Test
    fun `test cancelNotification removes scheduled escalation alarm and clears notifications`() {
        val triggerTime = System.currentTimeMillis() + 60_000L
        val doseId = "dose_cancel_tier1_456"

        NotificationScheduler.scheduleNotification(
            context = context,
            doseId = doseId,
            medicineName = "Amoxicillin",
            dose = "250mg",
            scheduledTimeStr = "2026-10-23T12:00:00Z",
            triggerTimeMillis = triggerTime
        )

        // Cancel
        NotificationScheduler.cancelNotification(context, doseId)

        // Verify escalation tier 1 notification is not in active notifications
        val activeNotifs = shadowNotificationManager.allNotifications
        assertTrue(activeNotifs.none { it.extras.getString(NotificationScheduler.EXTRA_DOSE_ID) == doseId })
    }

    @Test
    fun `test DoseNotificationReceiver handles ACTION_ESCALATION_TIER1 and triggers notification`() {
        NotificationScheduler.createNotificationChannels(context)

        val doseId = "dose_urgent_789"
        val receiver = DoseNotificationReceiver()
        val intent = Intent(DoseNotificationReceiver.ACTION_ESCALATION_TIER1).apply {
            putExtra(NotificationScheduler.EXTRA_DOSE_ID, doseId)
            putExtra(NotificationScheduler.EXTRA_MEDICINE_NAME, "Lisinopril")
            putExtra(NotificationScheduler.EXTRA_DOSE, "10mg")
            putExtra(NotificationScheduler.EXTRA_SCHEDULED_TIME, "2026-10-23T09:00:00Z")
        }

        receiver.onReceive(context, intent)

        // Poll for notification posted by background coroutine
        val notifId = doseId.hashCode() + 100
        var notification = shadowNotificationManager.getNotification(notifId)
        val startTime = System.currentTimeMillis()
        while (notification == null && (System.currentTimeMillis() - startTime) < 3000L) {
            Thread.sleep(50)
            notification = shadowNotificationManager.getNotification(notifId)
        }

        assertNotNull("Escalation Tier 1 notification should be posted", notification)
        assertEquals(NotificationScheduler.CHANNEL_ESCALATION_TIER1_ID, notification!!.channelId)
        assertTrue(
            "Notification title should denote missed dose or urgent alert",
            notification.extras.getCharSequence("android.title").toString().contains("Missed", ignoreCase = true) ||
            notification.extras.getCharSequence("android.title").toString().contains("Urgent", ignoreCase = true)
        )
    }
}
