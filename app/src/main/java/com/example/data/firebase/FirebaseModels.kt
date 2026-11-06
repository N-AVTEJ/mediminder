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

data class GuardianNotifyRecipient(
    val guardianId: String,
    val name: String,
    val phone: String,
    val relationship: String,
    val guardianUid: String? = null,
    val status: String = "linked"
)

data class GuardianNotifyPayload(
    val alertId: String = "alert_" + UUID.randomUUID().toString().take(8),
    val doseId: String,
    val medicineName: String,
    val dose: String,
    val scheduledTime: String,
    val formattedTime: String,
    val minutesOverdue: Int = 30,
    val escalationStage: Int = 2,
    val patientId: String,
    val patientName: String,
    val patientPhone: String,
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
    val alertTitle: String = "⚠️ Missed Dose Alert (30m Overdue)",
    val alertMessage: String = "URGENT CARE ALERT: $patientName missed scheduled dose of $medicineName ($dose) scheduled for $formattedTime (30 minutes overdue). Please check in with them immediately.",
    val smsBody: String = "MedRemind Alert: $patientName has missed their dose of $medicineName ($dose) scheduled for $formattedTime by 30 minutes. Please check on them.",
    val recipients: List<GuardianNotifyRecipient> = emptyList(),
    val fcmPayload: Map<String, String> = emptyMap(),
    val isDispatched: Boolean = false
)

/**
 * Phase 26: Guardian FCM Push Notification Record
 * Required Push Format: "{name} missed {medicine} {dose} ({time})"
 * Deep link to check-in screen: medremind://checkin?... or https://medremind.app/checkin?...
 */
data class GuardianFcmPushRecord(
    val id: String = "fcm_push_" + UUID.randomUUID().toString().take(8),
    val patientId: String,
    val patientName: String,
    val medicineName: String,
    val dose: String,
    val scheduledTime: String,
    val formattedTime: String,
    val pushTitle: String = "⚠️ Caregiver Alert: Missed Dose",
    val pushBody: String, // Required format: "{name} missed {medicine} {dose} ({time})"
    val deepLinkUri: String, // "medremind://checkin?..."
    val guardianRecipientId: String,
    val guardianName: String,
    val guardianPhone: String,
    val guardianUid: String?,
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
    val doseId: String = "",
    val delivered: Boolean = true
)

/**
 * Phase 26: Guardian Check-in Screen State
 */
data class GuardianCheckInState(
    val doseId: String,
    val patientId: String = "user_default_1",
    val patientName: String = "Eleanor Vance",
    val patientPhone: String = "+1 (555) 234-5678",
    val medicineName: String = "Lisinopril",
    val dose: String = "10mg",
    val scheduledTime: String = "08:00 AM",
    val minutesOverdue: Int = 45,
    val deepLinkSource: String = "",
    val checkInStatus: String = "PENDING" // "PENDING", "RESOLVED_TAKEN", "CALLED", "NOTE_RECORDED"
)

data class DoseFirebaseModel(
    val id: String = UUID.randomUUID().toString(),
    val medicineId: String,
    val scheduledTime: String, // e.g. "2026-07-26T08:00:00" in device timezone
    val status: String = "pending", // "pending", "taken", "missed"
    val medicine_id: String = medicineId,
    val scheduled_time: String = scheduledTime,
    val escalationStage: Int = 0, // 0 = normal, 1 = Tier 1 (15m), 2 = Tier 2 (30m)
    val escalation_stage: Int = escalationStage,
    val guardianNotifyPayload: GuardianNotifyPayload? = null
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
    val inviteToken: String = "tok_" + UUID.randomUUID().toString().replace("-", "").take(10),
    val inviteLink: String = "https://medremind.app/invite?token=$inviteToken",
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
    val linkedAt: String? = null,
    val patient_id: String = patientId,
    val guardian_phone: String = guardianPhone,
    val guardian_uid: String = guardianUid,
    val invite_token: String = inviteToken,
    val invite_link: String = inviteLink,
    val linked_at: String? = linkedAt
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
        relationship: String = "Caregiver",
        inviteToken: String = GuardianInviteService.generateInviteToken(),
        inviteLink: String = GuardianInviteService.buildInviteLink(inviteToken)
    ): GuardianFirestoreModel {
        val guardian = GuardianFirestoreModel(
            patientId = patientId,
            guardianPhone = guardianPhone,
            guardianUid = guardianUid,
            status = status,
            name = name,
            relationship = relationship,
            inviteToken = inviteToken,
            inviteLink = inviteLink,
            invite_token = inviteToken,
            invite_link = inviteLink
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

    fun findGuardianByToken(token: String): GuardianFirestoreModel? {
        val cleanToken = token.trim()
        return _guardians.value.find {
            it.inviteToken.equals(cleanToken, ignoreCase = true) ||
            it.invite_token.equals(cleanToken, ignoreCase = true) ||
            it.id.equals(cleanToken, ignoreCase = true)
        }
    }

    fun findGuardianById(id: String): GuardianFirestoreModel? {
        return _guardians.value.find { it.id == id || it.guardianUid == id }
    }

    fun findGuardianByPhone(phone: String): GuardianFirestoreModel? {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        return _guardians.value.find {
            it.guardianPhone.replace(Regex("[^0-9]"), "") == cleanPhone ||
            it.guardian_phone.replace(Regex("[^0-9]"), "") == cleanPhone
        }
    }

    /**
     * Requirement Phase 23: Guardian accept flow.
     * Guardian opens link, installs app or web view, confirms:
     * update guardians doc status="linked", link guardianUid.
     */
    fun linkGuardianWithToken(
        token: String,
        guardianUid: String,
        guardianName: String? = null,
        guardianPhone: String? = null
    ): GuardianFirestoreModel? {
        val current = _guardians.value.toMutableList()
        val cleanToken = token.trim()
        val index = current.indexOfFirst {
            it.inviteToken.equals(cleanToken, ignoreCase = true) ||
            it.invite_token.equals(cleanToken, ignoreCase = true) ||
            it.id.equals(cleanToken, ignoreCase = true)
        }
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        if (index != -1) {
            val existing = current[index]
            val effectiveUid = if (guardianUid.isNotBlank()) guardianUid else existing.guardianUid
            val effectiveName = if (!guardianName.isNullOrBlank()) guardianName else existing.name
            val effectivePhone = if (!guardianPhone.isNullOrBlank()) guardianPhone else existing.guardianPhone

            val updated = existing.copy(
                status = "linked",
                guardianUid = effectiveUid,
                guardian_uid = effectiveUid,
                name = effectiveName,
                guardianPhone = effectivePhone,
                guardian_phone = effectivePhone,
                linkedAt = nowIso,
                linked_at = nowIso
            )
            current[index] = updated
            _guardians.value = current

            // Add linked guardianUid to patient users collection guardian_ids
            val currentGuards = _currentUser.value.guardian_ids.toMutableList()
            if (!currentGuards.contains(effectiveUid)) {
                currentGuards.add(effectiveUid)
            }
            if (!currentGuards.contains(updated.id)) {
                currentGuards.add(updated.id)
            }
            _currentUser.value = _currentUser.value.copy(guardian_ids = currentGuards)

            return updated
        }
        return null
    }

    fun linkGuardianById(guardianId: String, guardianUid: String): GuardianFirestoreModel? {
        val guardian = findGuardianById(guardianId) ?: return null
        return linkGuardianWithToken(guardian.inviteToken, guardianUid)
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

    // Phase 25: Escalation Tier 2 & Guardian Notification Payloads
    private val _preparedGuardianPayloads = MutableStateFlow<List<GuardianNotifyPayload>>(emptyList())
    val preparedGuardianPayloads: StateFlow<List<GuardianNotifyPayload>> = _preparedGuardianPayloads.asStateFlow()

    fun prepGuardianNotifyPayload(
        doseId: String,
        medicineName: String,
        dose: String,
        scheduledTime: String,
        formattedTime: String
    ): GuardianNotifyPayload {
        val user = _currentUser.value
        val patientName = user.name.ifBlank { "Eleanor Vance" }
        val patientPhone = user.phone.ifBlank { "+1 (555) 234-5678" }
        val activeGuardians = _guardians.value.map { g ->
            GuardianNotifyRecipient(
                guardianId = g.id,
                name = g.name.ifBlank { "Caregiver" },
                phone = g.guardianPhone,
                relationship = g.relationship,
                guardianUid = g.guardianUid,
                status = g.status
            )
        }

        val title = "⚠️ Missed Dose Alert (30m Overdue)"
        val message = "URGENT CARE ALERT: $patientName has missed scheduled dose of $medicineName ($dose) scheduled for $formattedTime (30 minutes overdue). Please check in with them immediately."
        val sms = "MedRemind Alert: $patientName has missed their scheduled dose of $medicineName ($dose) from $formattedTime by 30 minutes. Please check on them."

        val fcmData = mapOf(
            "type" to "GUARDIAN_ESCALATION_STAGE_2",
            "dose_id" to doseId,
            "patient_id" to user.id,
            "patient_name" to patientName,
            "patient_phone" to patientPhone,
            "medicine_name" to medicineName,
            "dose" to dose,
            "scheduled_time" to scheduledTime,
            "formatted_time" to formattedTime,
            "escalation_stage" to "2",
            "minutes_overdue" to "30",
            "alert_title" to title,
            "alert_message" to message
        )

        val payload = GuardianNotifyPayload(
            doseId = doseId,
            medicineName = medicineName,
            dose = dose,
            scheduledTime = scheduledTime,
            formattedTime = formattedTime,
            minutesOverdue = 30,
            escalationStage = 2,
            patientId = user.id,
            patientName = patientName,
            patientPhone = patientPhone,
            alertTitle = title,
            alertMessage = message,
            smsBody = sms,
            recipients = activeGuardians,
            fcmPayload = fcmData,
            isDispatched = false
        )

        val current = _preparedGuardianPayloads.value.toMutableList()
        current.removeAll { it.doseId == doseId }
        current.add(0, payload)
        _preparedGuardianPayloads.value = current

        markEscalationStage(doseId, 2, payload)
        return payload
    }

    fun markEscalationStage(doseId: String, stage: Int, payload: GuardianNotifyPayload? = null) {
        val currentDoses = _doses.value.toMutableList()
        val index = currentDoses.indexOfFirst { it.id == doseId }
        if (index != -1) {
            val updated = currentDoses[index].copy(
                escalationStage = stage,
                escalation_stage = stage,
                guardianNotifyPayload = payload ?: currentDoses[index].guardianNotifyPayload
            )
            currentDoses[index] = updated
            _doses.value = currentDoses
        }
    }

    fun markEscalationStageByMedicineAndSchedule(
        medicineName: String,
        scheduledTime: String,
        stage: Int,
        payload: GuardianNotifyPayload? = null
    ): String? {
        val dose = _doses.value.find { d ->
            val med = _medicines.value.find { it.id == d.medicineId }
            med?.name.equals(medicineName, ignoreCase = true) && d.scheduledTime.contains(scheduledTime)
        }
        if (dose != null) {
            markEscalationStage(dose.id, stage, payload)
            return dose.id
        }
        return null
    }

    fun removePreparedGuardianPayload(alertId: String) {
        val current = _preparedGuardianPayloads.value.toMutableList()
        current.removeAll { it.alertId == alertId }
        _preparedGuardianPayloads.value = current
    }

    fun markGuardianPayloadDispatched(alertId: String) {
        val current = _preparedGuardianPayloads.value.toMutableList()
        val index = current.indexOfFirst { it.alertId == alertId }
        if (index != -1) {
            current[index] = current[index].copy(isDispatched = true)
            _preparedGuardianPayloads.value = current
        }
    }

    // Phase 26: Dispatched FCM Push Records State Flow
    private val _dispatchedFcmPushes = MutableStateFlow<List<GuardianFcmPushRecord>>(emptyList())
    val dispatchedFcmPushes: StateFlow<List<GuardianFcmPushRecord>> = _dispatchedFcmPushes.asStateFlow()

    /**
     * Phase 26: Send FCM push to all linked guardians:
     * Format: "{name} missed {medicine} {dose} ({time})". Deep link to check-in screen.
     */
    fun sendFcmPushToLinkedGuardians(
        doseId: String,
        patientName: String? = null,
        medicineName: String,
        dose: String,
        scheduledTimeStr: String
    ): List<GuardianFcmPushRecord> {
        val user = _currentUser.value
        val effectivePatientName = (patientName ?: user.name).ifBlank { "Eleanor Vance" }
        val effectivePhone = user.phone.ifBlank { "+1 (555) 234-5678" }

        val formattedTime = if (scheduledTimeStr.isNotBlank()) {
            com.example.utils.TimeUtils.fromUtcIsoStringToLocal12Hour(scheduledTimeStr)
        } else "08:00 AM"

        // Required push notification format: "{name} missed {medicine} {dose} ({time})"
        val messageBody = "$effectivePatientName missed $medicineName $dose ($formattedTime)"

        val linkedGuardians = _guardians.value.filter {
            it.status.equals("linked", ignoreCase = true) || it.guardianUid.isNotBlank()
        }.ifEmpty {
            _guardians.value
        }

        val records = linkedGuardians.map { guardian ->
            val encodedName = java.net.URLEncoder.encode(effectivePatientName, "UTF-8")
            val encodedMed = java.net.URLEncoder.encode(medicineName, "UTF-8")
            val encodedDose = java.net.URLEncoder.encode(dose, "UTF-8")
            val encodedTime = java.net.URLEncoder.encode(formattedTime, "UTF-8")
            val encodedPhone = java.net.URLEncoder.encode(effectivePhone, "UTF-8")
            val deepLink = "medremind://checkin?doseId=$doseId&patientId=${user.id}&patientName=$encodedName&medicine=$encodedMed&dose=$encodedDose&time=$encodedTime&phone=$encodedPhone"

            GuardianFcmPushRecord(
                patientId = user.id,
                patientName = effectivePatientName,
                medicineName = medicineName,
                dose = dose,
                scheduledTime = scheduledTimeStr,
                formattedTime = formattedTime,
                pushTitle = "⚠️ Caregiver Alert: Missed Dose",
                pushBody = messageBody,
                deepLinkUri = deepLink,
                guardianRecipientId = guardian.id,
                guardianName = guardian.name.ifBlank { "Caregiver" },
                guardianPhone = guardian.guardianPhone,
                guardianUid = guardian.guardianUid,
                doseId = doseId,
                delivered = true
            )
        }

        val current = _dispatchedFcmPushes.value.toMutableList()
        current.addAll(0, records)
        _dispatchedFcmPushes.value = current

        // Update escalation stage to 3
        markEscalationStage(doseId, 3)

        return records
    }
}
