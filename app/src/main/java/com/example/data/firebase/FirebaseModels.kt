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

data class AffiliateClickFirebaseModel(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val medicine: String,
    val pharmacy: String,
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
    val user_id: String = userId
)


data class InventoryFirebaseModel(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "user_default_1",
    val medicineName: String = "",
    val quantity: Int = 0,
    val lastUpdated: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
    val user_id: String = userId,
    val medicine_name: String = medicineName,
    val last_updated: String = lastUpdated
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
 * Requirement Phase 21: Firestore "guardians" collection data model:
 * {patientId, guardianPhone, guardianUid, status: pending/linked}
 */
data class GuardianFirestoreModel(
    val id: String = UUID.randomUUID().toString(),
    val patientId: String = "user_default_1",
    val guardianPhone: String,
    val guardianUid: String = "guardian_" + UUID.randomUUID().toString().take(6),
    val status: String = "pending", // "pending" or "linked"
    val name: String = "",
    val relationship: String = "Caregiver",
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
    val patient_id: String = patientId,
    val guardian_phone: String = guardianPhone,
    val guardian_uid: String = guardianUid
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

    private val _affiliateClicks = MutableStateFlow<List<AffiliateClickFirebaseModel>>(
        listOf(
            AffiliateClickFirebaseModel(userId = "user_default_1", medicine = "Amoxicillin", pharmacy = "1mg"),
            AffiliateClickFirebaseModel(userId = "user_default_1", medicine = "Amoxicillin", pharmacy = "PharmEasy"),
            AffiliateClickFirebaseModel(userId = "user_default_1", medicine = "Lisinopril", pharmacy = "Netmeds"),
            AffiliateClickFirebaseModel(userId = "user_default_1", medicine = "Amoxicillin", pharmacy = "1mg"),
            AffiliateClickFirebaseModel(userId = "user_default_1", medicine = "Metformin", pharmacy = "PharmEasy"),
            AffiliateClickFirebaseModel(userId = "user_default_1", medicine = "Atorvastatin", pharmacy = "Netmeds"),
            AffiliateClickFirebaseModel(userId = "user_default_1", medicine = "Lisinopril", pharmacy = "1mg")
        )
    )
    val affiliateClicks: StateFlow<List<AffiliateClickFirebaseModel>> = _affiliateClicks.asStateFlow()

    fun logAffiliateClick(userId: String, medicine: String, pharmacy: String) {
        val click = AffiliateClickFirebaseModel(userId = userId, medicine = medicine, pharmacy = pharmacy)
        val current = _affiliateClicks.value.toMutableList()
        current.add(0, click)
        _affiliateClicks.value = current
    }

    fun seedSampleAffiliateClicks() {
        val samples = listOf(
            AffiliateClickFirebaseModel(userId = "user_default_1", medicine = "Amoxicillin", pharmacy = "1mg"),
            AffiliateClickFirebaseModel(userId = "user_default_1", medicine = "Omeprazole", pharmacy = "PharmEasy"),
            AffiliateClickFirebaseModel(userId = "user_default_1", medicine = "Metformin", pharmacy = "Netmeds")
        )
        val current = _affiliateClicks.value.toMutableList()
        current.addAll(0, samples)
        _affiliateClicks.value = current
    }

    fun clearAffiliateClicks() {
        _affiliateClicks.value = emptyList()
    }



    private val _reminders = MutableStateFlow<List<ReminderFirebaseModel>>(emptyList())
    val reminders: StateFlow<List<ReminderFirebaseModel>> = _reminders.asStateFlow()
    private val _inventory = MutableStateFlow<List<InventoryFirebaseModel>>(emptyList())
    val inventory: StateFlow<List<InventoryFirebaseModel>> = _inventory.asStateFlow()

    private val _guardians = MutableStateFlow<List<GuardianFirestoreModel>>(
        listOf(
            GuardianFirestoreModel(
                id = "guardian_1",
                patientId = "user_default_1",
                guardianPhone = "+1 (555) 987-6543",
                guardianUid = "guardian_user_987",
                status = "linked",
                name = "Dr. Sarah Jenkins",
                relationship = "Primary Caregiver"
            ),
            GuardianFirestoreModel(
                id = "guardian_2",
                patientId = "user_default_1",
                guardianPhone = "+1 (555) 345-6789",
                guardianUid = "guardian_user_345",
                status = "pending",
                name = "David Vance",
                relationship = "Son"
            )
        )
    )
    val guardians: StateFlow<List<GuardianFirestoreModel>> = _guardians.asStateFlow()

    fun addGuardianToFirebase(
        patientId: String,
        guardianPhone: String,
        guardianUid: String = "guardian_" + UUID.randomUUID().toString().take(6),
        status: String = "pending",
        name: String = "",
        relationship: String = "Caregiver"
    ): GuardianFirestoreModel {
        val guardian = GuardianFirestoreModel(
            patientId = patientId,
            guardianPhone = guardianPhone,
            guardianUid = guardianUid,
            status = status,
            name = name,
            relationship = relationship
        )
        val current = _guardians.value.toMutableList()
        current.add(guardian)
        _guardians.value = current

        val currentGuards = _currentUser.value.guardian_ids.toMutableList()
        if (!currentGuards.contains(guardian.id)) {
            currentGuards.add(guardian.id)
            _currentUser.value = _currentUser.value.copy(guardian_ids = currentGuards)
        }
        return guardian
    }

    fun updateGuardianStatus(guardianId: String, newStatus: String) {
        val current = _guardians.value.toMutableList()
        val index = current.indexOfFirst { it.id == guardianId || it.guardianUid == guardianId }
        if (index != -1) {
            current[index] = current[index].copy(status = newStatus)
            _guardians.value = current
        }
    }

    fun deleteGuardianFromFirebaseByPhone(phone: String) {
        val current = _guardians.value.toMutableList()
        current.removeAll { it.guardianPhone == phone || it.guardian_phone == phone }
        _guardians.value = current
    }

    fun updateInventory(medicineName: String, quantity: Int) {
        val currentInventory = _inventory.value.toMutableList()
        val index = currentInventory.indexOfFirst { it.medicineName.equals(medicineName, ignoreCase = true) || it.medicine_name.equals(medicineName, ignoreCase = true) }
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        if (index != -1) {
            val updated = currentInventory[index].copy(quantity = quantity, lastUpdated = nowIso, last_updated = nowIso)
            currentInventory[index] = updated
        } else {
            currentInventory.add(InventoryFirebaseModel(medicineName = medicineName, medicine_name = medicineName, quantity = quantity, lastUpdated = nowIso, last_updated = nowIso))
        }
        _inventory.value = currentInventory
    }

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
