package com.example.data.repository

import com.example.data.db.DoseLogEntity
import com.example.data.db.ReminderEntity
import com.example.data.firebase.DoseFirebaseModel
import com.example.data.firebase.MedicineFirebaseModel
import com.example.data.firebase.ReminderFirebaseModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

data class GeneratedDoseSchedule(
    val firebaseMedicine: MedicineFirebaseModel,
    val firebaseDoses: List<DoseFirebaseModel>,
    val firebaseReminders: List<ReminderFirebaseModel>,
    val roomReminder: ReminderEntity,
    val roomDoseLogs: List<DoseLogEntity>,
    val notificationTriggers: List<NotificationTrigger>
)

data class NotificationTrigger(
    val doseId: String,
    val medicineName: String,
    val dose: String,
    val triggerTimeMillis: Long,
    val formattedTime: String,
    val dateStr: String
)

object DoseScheduleGenerator {

    /**
     * Parses frequency and maps to daily times in device local timezone.
     */
    fun parseDailyTimesFromFrequency(frequency: String): List<String> {
        val f = frequency.lowercase(Locale.getDefault())
        return when {
            f.contains("twice") || f.contains("2 time") || f.contains("12 hour") || f.contains("bd") || f.contains("bid") -> {
                listOf("08:00 AM", "08:00 PM")
            }
            f.contains("three") || f.contains("3 time") || f.contains("8 hour") || f.contains("tds") || f.contains("tid") || f.contains("thrice") -> {
                listOf("08:00 AM", "02:00 PM", "08:00 PM")
            }
            f.contains("four") || f.contains("4 time") || f.contains("6 hour") || f.contains("qds") || f.contains("qid") -> {
                listOf("08:00 AM", "12:00 PM", "04:00 PM", "08:00 PM")
            }
            f.contains("night") || f.contains("bedtime") || f.contains("sleep") -> {
                listOf("09:00 PM")
            }
            f.contains("evening") -> {
                listOf("06:30 PM")
            }
            f.contains("afternoon") || f.contains("lunch") -> {
                listOf("01:00 PM")
            }
            else -> {
                listOf("08:00 AM")
            }
        }
    }

    /**
     * Auto-generates dose schedule for duration_days based on frequency using device locale timezone.
     */
    fun generateSchedule(
        userId: String = "user_default_1",
        medicineName: String,
        dose: String,
        frequency: String,
        durationDays: Int = 7,
        instructions: String = "Take as prescribed"
    ): GeneratedDoseSchedule {
        val zoneId = ZoneId.systemDefault()
        val startDateLocal = LocalDate.now(zoneId)
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
        val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val startDateStr = startDateLocal.format(dateFormatter)

        val dailyTimes = parseDailyTimesFromFrequency(frequency)
        val medicineId = UUID.randomUUID().toString()

        val firebaseMedicine = MedicineFirebaseModel(
            id = medicineId,
            user_id = userId,
            name = medicineName,
            dose = dose,
            frequency = frequency,
            duration_days = durationDays,
            start_date = startDateStr
        )

        val firebaseDoses = mutableListOf<DoseFirebaseModel>()
        val firebaseReminders = mutableListOf<ReminderFirebaseModel>()
        val roomDoseLogs = mutableListOf<DoseLogEntity>()
        val notificationTriggers = mutableListOf<NotificationTrigger>()

        val primaryTime = dailyTimes.firstOrNull() ?: "08:00 AM"
        val timeCategory = inferCategoryFromTimeStr(primaryTime)
        val colorHex = when (timeCategory) {
            "Morning" -> "#00796B"
            "Afternoon" -> "#0288D1"
            "Evening" -> "#E65100"
            else -> "#6A1B9A"
        }

        val roomReminder = ReminderEntity(
            name = medicineName,
            dosage = "$dose ($frequency, $durationDays days)",
            time = primaryTime,
            timeCategory = timeCategory,
            instructions = instructions,
            isActive = true,
            colorHex = colorHex,
            shape = "Pill"
        )

        val time12Formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

        for (dayOffset in 0 until durationDays) {
            val currentDate = startDateLocal.plusDays(dayOffset.toLong())
            val dateStr = currentDate.format(dateFormatter)

            for (timeStr in dailyTimes) {
                val doseId = UUID.randomUUID().toString()
                val reminderId = UUID.randomUUID().toString()

                val localTime = parse12HourTime(timeStr)
                val zonedDateTime = ZonedDateTime.of(currentDate, localTime, zoneId)
                val scheduledIsoStr = zonedDateTime.format(isoFormatter)
                val triggerMillis = zonedDateTime.toInstant().toEpochMilli()

                // Firebase Dose
                val doseFb = DoseFirebaseModel(
                    id = doseId,
                    medicine_id = medicineId,
                    scheduled_time = scheduledIsoStr,
                    status = "pending"
                )
                firebaseDoses.add(doseFb)

                // Firebase Reminder
                val reminderFb = ReminderFirebaseModel(
                    id = reminderId,
                    dose_id = doseId,
                    notify_time = scheduledIsoStr,
                    sent = false
                )
                firebaseReminders.add(reminderFb)

                // Room Dose Log
                val roomLog = DoseLogEntity(
                    reminderId = 0, // Will map to Room reminder id
                    medicineName = medicineName,
                    dosage = dose,
                    dateStr = dateStr,
                    scheduledTime = timeStr,
                    status = "PENDING"
                )
                roomDoseLogs.add(roomLog)

                // Notification Trigger
                notificationTriggers.add(
                    NotificationTrigger(
                        doseId = doseId,
                        medicineName = medicineName,
                        dose = dose,
                        triggerTimeMillis = triggerMillis,
                        formattedTime = timeStr,
                        dateStr = dateStr
                    )
                )
            }
        }

        return GeneratedDoseSchedule(
            firebaseMedicine = firebaseMedicine,
            firebaseDoses = firebaseDoses,
            firebaseReminders = firebaseReminders,
            roomReminder = roomReminder,
            roomDoseLogs = roomDoseLogs,
            notificationTriggers = notificationTriggers
        )
    }

    private fun parse12HourTime(timeStr: String): LocalTime {
        return try {
            val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
            LocalTime.parse(timeStr.trim().uppercase(Locale.US), formatter)
        } catch (e: Exception) {
            LocalTime.of(8, 0)
        }
    }

    private fun inferCategoryFromTimeStr(timeStr: String): String {
        val t = timeStr.lowercase(Locale.getDefault())
        return when {
            t.contains("pm") && (t.startsWith("08") || t.startsWith("09") || t.startsWith("10") || t.startsWith("11")) -> "Night"
            t.contains("pm") && (t.startsWith("05") || t.startsWith("06") || t.startsWith("07")) -> "Evening"
            t.contains("pm") && (t.startsWith("12") || t.startsWith("01") || t.startsWith("02") || t.startsWith("03") || t.startsWith("04")) -> "Afternoon"
            else -> "Morning"
        }
    }
}
