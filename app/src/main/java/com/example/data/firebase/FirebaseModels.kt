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
    val id: String = "user_default_1",
    val name: String = "Eleanor Vance",
    val phone: String = "+1 (555) 234-5678",
    val guardian_ids: List<String> = listOf("guardian_1", "guardian_2")
)

data class MedicineFirebaseModel(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String = "user_default_1",
    val name: String,
    val dose: String,
    val frequency: String,
    val duration_days: Int,
    val start_date: String // e.g. "2026-07-23" formatted using device locale
)

data class DoseFirebaseModel(
    val id: String = UUID.randomUUID().toString(),
    val medicine_id: String,
    val scheduled_time: String, // e.g. "2026-07-23T08:00:00" in device timezone
    val status: String = "pending" // "pending", "taken", "missed"
)

data class ReminderFirebaseModel(
    val id: String = UUID.randomUUID().toString(),
    val dose_id: String,
    val notify_time: String, // e.g. "2026-07-23T08:00:00"
    val sent: Boolean = false
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
            val remIndex = currentReminders.indexOfFirst { it.dose_id == doseId }
            if (remIndex != -1) {
                currentReminders[remIndex] = currentReminders[remIndex].copy(sent = true)
                _reminders.value = currentReminders
            }
        }
    }

    fun markDoseTakenByMedicineAndSchedule(medicineName: String, scheduledTime: String) {
        val currentDoses = _doses.value.toMutableList()
        val index = currentDoses.indexOfFirst { dose ->
            val med = _medicines.value.find { it.id == dose.medicine_id }
            med?.name.equals(medicineName, ignoreCase = true) && dose.scheduled_time.contains(scheduledTime)
        }
        if (index != -1) {
            currentDoses[index] = currentDoses[index].copy(status = "taken")
            _doses.value = currentDoses
        }
    }
}
