package com.example.data.repository

import com.example.data.db.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicineRepository(
    private val db: AppDatabase
) {
    private val reminderDao = db.reminderDao()
    private val doseLogDao = db.doseLogDao()
    private val guardianDao = db.guardianDao()
    private val profileDao = db.profileDao()

    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()
    val activeReminders: Flow<List<ReminderEntity>> = reminderDao.getActiveReminders()
    val allGuardians: Flow<List<GuardianEntity>> = guardianDao.getAllGuardians()
    val profile: Flow<ProfileEntity?> = profileDao.getProfile()

    fun getTodayLogs(): Flow<List<DoseLogEntity>> {
        val todayStr = getTodayDateStr()
        return doseLogDao.getLogsForDate(todayStr)
    }

    suspend fun ensureDefaultDataSeeded() {
        // Seed Profile if empty
        val existingProfile = profileDao.getProfile().firstOrNull()
        if (existingProfile == null) {
            profileDao.insertOrUpdateProfile(
                ProfileEntity(
                    id = 1,
                    name = "Eleanor Vance",
                    phone = "+1 (555) 234-5678",
                    emergencyInfo = "Blood Type: O+, Allergy: Penicillin",
                    isPremium = true,
                    largeTextMode = true
                )
            )
        }

        // Seed Guardians if empty
        val existingGuardians = guardianDao.getAllGuardians().firstOrNull()
        if (existingGuardians.isNullOrEmpty()) {
            guardianDao.insertGuardian(
                GuardianEntity(
                    name = "Emily Vance",
                    relationship = "Daughter & Caregiver",
                    phone = "+1 (555) 987-6543",
                    notifyOnMissed = true
                )
            )
            guardianDao.insertGuardian(
                GuardianEntity(
                    name = "Dr. Sarah Jenkins",
                    relationship = "Primary Physician",
                    phone = "+1 (555) 432-1098",
                    notifyOnMissed = false
                )
            )
        }

        // Seed Reminders & Dose Logs if empty
        val existingReminders = reminderDao.getAllReminders().firstOrNull()
        if (existingReminders.isNullOrEmpty()) {
            val id1 = reminderDao.insertReminder(
                ReminderEntity(
                    name = "Lisinopril",
                    dosage = "10 mg Tablet",
                    time = "08:00 AM",
                    timeCategory = "Morning",
                    instructions = "Take after breakfast with water",
                    isActive = true,
                    colorHex = "#00796B",
                    shape = "Pill"
                )
            ).toInt()

            val id2 = reminderDao.insertReminder(
                ReminderEntity(
                    name = "Metformin",
                    dosage = "500 mg Tablet",
                    time = "01:00 PM",
                    timeCategory = "Afternoon",
                    instructions = "Take during lunch to prevent stomach upset",
                    isActive = true,
                    colorHex = "#0288D1",
                    shape = "Tablet"
                )
            ).toInt()

            val id3 = reminderDao.insertReminder(
                ReminderEntity(
                    name = "Atorvastatin",
                    dosage = "20 mg Capsule",
                    time = "08:00 PM",
                    timeCategory = "Night",
                    instructions = "Take before bedtime",
                    isActive = true,
                    colorHex = "#D32F2F",
                    shape = "Capsule"
                )
            ).toInt()

            val todayStr = getTodayDateStr()
            doseLogDao.insertLogs(
                listOf(
                    DoseLogEntity(
                        reminderId = id1,
                        medicineName = "Lisinopril",
                        dosage = "10 mg Tablet",
                        dateStr = todayStr,
                        scheduledTime = "08:00 AM",
                        status = "TAKEN",
                        takenTime = "08:05 AM"
                    ),
                    DoseLogEntity(
                        reminderId = id2,
                        medicineName = "Metformin",
                        dosage = "500 mg Tablet",
                        dateStr = todayStr,
                        scheduledTime = "01:00 PM",
                        status = "PENDING"
                    ),
                    DoseLogEntity(
                        reminderId = id3,
                        medicineName = "Atorvastatin",
                        dosage = "20 mg Capsule",
                        dateStr = todayStr,
                        scheduledTime = "08:00 PM",
                        status = "MISSED" // Highlighted RED on Home screen!
                    )
                )
            )
        } else {
            // Check if today's dose logs exist
            val todayStr = getTodayDateStr()
            val logs = doseLogDao.getLogsForDate(todayStr).firstOrNull()
            if (logs.isNullOrEmpty()) {
                val activeMeds = reminderDao.getActiveReminders().firstOrNull() ?: emptyList()
                val newLogs = activeMeds.mapIndexed { index, med ->
                    val initialStatus = when (index % 3) {
                        0 -> "TAKEN"
                        1 -> "PENDING"
                        else -> "MISSED"
                    }
                    DoseLogEntity(
                        reminderId = med.id,
                        medicineName = med.name,
                        dosage = med.dosage,
                        dateStr = todayStr,
                        scheduledTime = med.time,
                        status = initialStatus,
                        takenTime = if (initialStatus == "TAKEN") "08:05 AM" else null
                    )
                }
                if (newLogs.isNotEmpty()) {
                    doseLogDao.insertLogs(newLogs)
                }
            }
        }
    }

    suspend fun addReminder(reminder: ReminderEntity) {
        val newId = reminderDao.insertReminder(reminder).toInt()
        val todayStr = getTodayDateStr()
        doseLogDao.insertLog(
            DoseLogEntity(
                reminderId = newId,
                medicineName = reminder.name,
                dosage = reminder.dosage,
                dateStr = todayStr,
                scheduledTime = reminder.time,
                status = "PENDING"
            )
        )
    }

    suspend fun toggleReminderActive(id: Int, isActive: Boolean) {
        reminderDao.updateActiveStatus(id, isActive)
    }

    suspend fun updateReminderTime(id: Int, newTime: String, timeCategory: String) {
        reminderDao.updateTime(id, newTime, timeCategory)
    }

    suspend fun deleteReminder(reminder: ReminderEntity) {
        reminderDao.deleteReminder(reminder)
        doseLogDao.deleteLogsForReminder(reminder.id)
    }

    suspend fun updateDoseStatus(logId: Int, status: String) {
        val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        doseLogDao.updateLogStatus(logId, status, if (status == "TAKEN") nowTime else null)
    }

    suspend fun addGuardian(guardian: GuardianEntity) {
        guardianDao.insertGuardian(guardian)
    }

    suspend fun deleteGuardian(guardian: GuardianEntity) {
        guardianDao.deleteGuardian(guardian)
    }

    suspend fun updateProfile(profile: ProfileEntity) {
        profileDao.insertOrUpdateProfile(profile)
    }

    fun getTodayDateStr(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
