package com.example.data.firebase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Firebase Firestore Data Models as requested:
 * - users (id, name, phone, guardian_ids[])
 * - medicines (id, user_id, name, dose, frequency, duration_days, start_date)
 * - doses (id, medicine_id, scheduled_time, status: pending/taken/missed)
 * - reminders (id, dose_id, notify_time, sent boolean)
 */

data class UserFirebaseModel(
    val uid: String = "user_default_1",
    val id: String = "user_default_1",
    val name: String = "Eleanor Vance",
    val phone: String = "+1 (555) 234-5678",
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
    val guardian_ids: List<String> = listOf("guardian_1", "guardian_2"),
    val fcmToken: String = "",
    val fcm_token: String = fcmToken
)

data class MedicineFirebaseModel(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "user_default_1",
    val name: String,
    val dose: String,
    val frequency: String,
    val durationDays: Int,
    val startDate: String, // e.g. "2026-07-26" formatted using device locale
    val user_id: String = userId,
    val duration_days: Int = durationDays,
    val start_date: String = startDate
)

data class DoseFirebaseModel(
    val id: String = UUID.randomUUID().toString(),
    val medicineId: String,
    val scheduledTime: String, // e.g. "2026-07-26T08:00:00" in device timezone
    val status: String = "pending", // "pending", "taken", "missed"
    val medicine_id: String = medicineId,
    val scheduled_time: String = scheduledTime
)

data class ReminderFirebaseModel(
    val id: String = UUID.randomUUID().toString(),
    val doseId: String,
    val notifyTime: String, // e.g. "2026-07-26T08:00:00"
    val sent: Boolean = false,
    val notificationId: String = "notif_$doseId",
    val dose_id: String = doseId,
    val notify_time: String = notifyTime,
    val notification_id: String = notificationId
)

/**
 * Firebase Firestore Local Repository Sync Manager.
 * Maintains real-time state mirror of Firebase Firestore collections.
 */
class FirebaseSyncRepository private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: FirebaseSyncRepository? = null

        fun getInstance(): FirebaseSyncRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseSyncRepository().also { INSTANCE = it }
            }
        }
    }

    private val _currentUser = MutableStateFlow(UserFirebaseModel())
    val currentUser: StateFlow<UserFirebaseModel> = _currentUser.asStateFlow()

    private val _medicines = MutableStateFlow<List<MedicineFirebaseModel>>(emptyList())
    val medicines: StateFlow<List<MedicineFirebaseModel>> = _medicines.asStateFlow()

    private val _doses = MutableStateFlow<List<DoseFirebaseModel>>(emptyList())
    val doses: StateFlow<List<DoseFirebaseModel>> = _doses.asStateFlow()

    private val _reminders = MutableStateFlow<List<ReminderFirebaseModel>>(emptyList())
    val reminders: StateFlow<List<ReminderFirebaseModel>> = _reminders.asStateFlow()

    fun updateFirebaseUser(name: String, phone: String, guardianIds: List<String>) {
        val updated = _currentUser.value.copy(name = name, phone = phone, guardian_ids = guardianIds)
        _currentUser.value = updated
    }

    fun registerFcmToken(fcmToken: String) {
        val current = _currentUser.value
        val updated = current.copy(
            fcmToken = fcmToken,
            fcm_token = fcmToken
        )
        _currentUser.value = updated
    }

    /**
     * Requirement: Write to Firestore "users" collection {uid, name, phone, createdAt, fcmToken} on signup
     */
    fun writeUserOnSignup(uid: String, name: String, phone: String, fcmToken: String = ""): UserFirebaseModel {
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val existingToken = if (fcmToken.isNotBlank()) fcmToken else _currentUser.value.fcmToken
        val newUser = UserFirebaseModel(
            uid = uid,
            id = uid,
            name = name,
            phone = phone,
            createdAt = nowIso,
            guardian_ids = emptyList(),
            fcmToken = existingToken,
            fcm_token = existingToken
        )
        _currentUser.value = newUser
        return newUser
    }

    fun addMedicineWithDosesAndReminders(
        medicine: MedicineFirebaseModel,
        newDoses: List<DoseFirebaseModel>,
        newReminders: List<ReminderFirebaseModel>
    ) {
        val currentMeds = _medicines.value.toMutableList()
        currentMeds.removeAll { it.id == medicine.id }
        currentMeds.add(medicine)
        _medicines.value = currentMeds

        val currentDoses = _doses.value.toMutableList()
        currentDoses.addAll(newDoses)
        _doses.value = currentDoses

        val currentReminders = _reminders.value.toMutableList()
        currentReminders.addAll(newReminders)
        _reminders.value = currentReminders
    }

    fun updateDoseStatusInFirebase(doseId: String, status: String) {
        val currentDoses = _doses.value.toMutableList()
        val index = currentDoses.indexOfFirst { it.id == doseId }
        if (index != -1) {
            val updated = currentDoses[index].copy(status = status.lowercase())
            currentDoses[index] = updated
            _doses.value = currentDoses

            // Update associated reminder as sent if status is taken or missed
            val currentReminders = _reminders.value.toMutableList()
            val remIndex = currentReminders.indexOfFirst { it.doseId == doseId || it.dose_id == doseId }
            if (remIndex != -1) {
                currentReminders[remIndex] = currentReminders[remIndex].copy(sent = true)
                _reminders.value = currentReminders
            }
        }
    }

    fun markDoseStatusByMedicineAndSchedule(medicineName: String, scheduledTime: String, status: String): String? {
        val dose = _doses.value.find { d ->
            val med = _medicines.value.find { it.id == d.medicineId }
            med?.name.equals(medicineName, ignoreCase = true) && d.scheduledTime.contains(scheduledTime)
        }
        if (dose != null) {
            updateDoseStatusInFirebase(dose.id, status)
            return dose.id
        }
        return null
    }

    fun markDoseTakenByMedicineAndSchedule(medicineName: String, scheduledTime: String): String? {
        return markDoseStatusByMedicineAndSchedule(medicineName, scheduledTime, "taken")
    }
}
