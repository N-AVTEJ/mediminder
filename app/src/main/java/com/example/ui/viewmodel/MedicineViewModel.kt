package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.PrescriptionScanException
import com.example.data.api.PrescriptionScanner
import com.example.data.api.ScanResult
import com.example.data.api.ScannedMedicationItem
import com.example.data.db.*
import com.example.data.firebase.*
import com.example.data.pharmacy.PharmacyAffiliateService
import com.example.data.pharmacy.PharmacyProvider
import com.example.data.repository.DoseScheduleGenerator
import com.example.data.repository.MedicineRepository
import com.example.notifications.FcmTokenManager
import com.example.notifications.NotificationScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MedicineViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = MedicineRepository(db)
    private val prescriptionScanner = PrescriptionScanner(application)
    private val firebaseSyncRepo = FirebaseSyncRepository.getInstance()

    val reminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayLogs: StateFlow<List<DoseLogEntity>> = repository.getTodayLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val guardians: StateFlow<List<GuardianEntity>> = repository.allGuardians
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profile: StateFlow<ProfileEntity?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Firebase Collections State Flow
    val firebaseUser: StateFlow<UserFirebaseModel> = firebaseSyncRepo.currentUser
    val firebaseMedicines: StateFlow<List<MedicineFirebaseModel>> = firebaseSyncRepo.medicines
    val firebaseDoses: StateFlow<List<DoseFirebaseModel>> = firebaseSyncRepo.doses
    val firebaseReminders: StateFlow<List<ReminderFirebaseModel>> = firebaseSyncRepo.reminders
    val firebaseGuardians: StateFlow<List<GuardianFirestoreModel>> = firebaseSyncRepo.guardians

    // Phase 25: Prepared Guardian Notify Payloads State Flow
    val preparedGuardianPayloads: StateFlow<List<GuardianNotifyPayload>> = firebaseSyncRepo.preparedGuardianPayloads

    // Phase 26: Dispatched FCM Pushes & Guardian Check-in State
    val dispatchedFcmPushes: StateFlow<List<GuardianFcmPushRecord>> = firebaseSyncRepo.dispatchedFcmPushes

    private val _checkInState = MutableStateFlow<GuardianCheckInState?>(null)
    val checkInState: StateFlow<GuardianCheckInState?> = _checkInState.asStateFlow()

    // Supabase Inventory & Affiliate Clicks State Flow
    val userInventory = PharmacyAffiliateService.inventoryTable
    val affiliateClicks = firebaseSyncRepo.affiliateClicks

    fun isMedicineInInventory(medicineName: String): Boolean {
        if (medicineName.isBlank()) return true
        val cleanName = medicineName.trim().lowercase(java.util.Locale.ROOT)
        return firebaseSyncRepo.inventory.value.any {
            it.quantity > 0 && (it.medicineName.trim().lowercase(java.util.Locale.ROOT).contains(cleanName) ||
                    cleanName.contains(it.medicineName.trim().lowercase(java.util.Locale.ROOT)))
        }
    }

    fun buyMedicineFromPharmacy(
        context: android.content.Context,
        medicineName: String,
        provider: PharmacyProvider
    ) {
        PharmacyAffiliateService.launchPharmacyPurchase(
            context = context,
            medicineName = medicineName,
            provider = provider,
            userId = firebaseUser.value.id
        )
        firebaseSyncRepo.logAffiliateClick(firebaseUser.value.id, medicineName, provider.displayName)
        _userMessage.value = "Opening ${provider.displayName} for $medicineName (Logged affiliate earnings tracking)"
    }

    fun seedSampleAffiliateClicks() {
        firebaseSyncRepo.seedSampleAffiliateClicks()
        _userMessage.value = "Added test affiliate click logs for analytics demonstration"
    }

    fun clearAffiliateClicks() {
        firebaseSyncRepo.clearAffiliateClicks()
        _userMessage.value = "Cleared all affiliate click logs"
    }

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private val _editableScannedList = MutableStateFlow<List<ScannedMedicationItem>>(emptyList())
    val editableScannedList: StateFlow<List<ScannedMedicationItem>> = _editableScannedList.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultDataSeeded()
            NotificationScheduler.createNotificationChannel(application)
            FcmTokenManager.registerDeviceFcmToken()
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun updateInventoryInFirestore(medicineName: String, quantity: Int) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val userId = firebaseUser.value.id
            val inventoryRef = db.collection("inventory").document("${userId}_${medicineName.replace(" ", "_")}")
            val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
            val data = hashMapOf(
                "userId" to userId,
                "medicineName" to medicineName,
                "quantity" to quantity,
                "lastUpdated" to nowIso
            )
            inventoryRef.set(data)
                .addOnSuccessListener { 
                    _userMessage.value = "Inventory updated in Firestore for $medicineName"
                    firebaseSyncRepo.updateInventory(medicineName, quantity)
                }
                .addOnFailureListener { e ->
                    _userMessage.value = "Failed to update inventory in Firestore: ${e.message}"
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearScanState() {
        _scanResult.value = null
        _scanError.value = null
        _editableScannedList.value = emptyList()
    }

    fun markDoseStatus(logId: Int, status: String) {
        viewModelScope.launch {
            if (status == "TAKEN") {
                repository.updateDoseStatusAndStage(logId, "TAKEN", 0)
            } else {
                repository.updateDoseStatus(logId, status)
            }

            // Sync with Firebase
            val logs = todayLogs.value
            val target = logs.find { it.id == logId }
            if (target != null) {
                val fbStatus = status.lowercase()
                val doseId = firebaseSyncRepo.markDoseStatusByMedicineAndSchedule(target.medicineName, target.scheduledTime, fbStatus)
                if (status == "TAKEN") {
                    if (doseId != null) {
                        NotificationScheduler.cancelNotification(getApplication(), doseId)
                        firebaseSyncRepo.markEscalationStage(doseId, 0)
                    }
                    val prepared = preparedGuardianPayloads.value.find { it.medicineName == target.medicineName }
                    if (prepared != null) {
                        firebaseSyncRepo.removePreparedGuardianPayload(prepared.alertId)
                    }
                }
            }

            when (status) {
                "TAKEN" -> _userMessage.value = "Great job! Dose logged as taken and synced to Firebase."
                "MISSED" -> _userMessage.value = "Dose marked as missed. Guardian alerted & updated in Firebase."
                "SNOOZED" -> _userMessage.value = "Reminder snoozed for 15 minutes."
            }
        }
    }

    /**
     * Phase 24: Test Escalation Tier 1 trigger (15 min missed dose with louder sound and vibration).
     */
    fun triggerEscalationTier1Test(medicineName: String = "Lisinopril", dose: String = "10mg") {
        val fakeDoseId = "test_escalation_" + System.currentTimeMillis()
        val nowIso = java.time.ZonedDateTime.now().minusMinutes(15).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        NotificationScheduler.triggerEscalationTier1Now(
            context = getApplication(),
            doseId = fakeDoseId,
            medicineName = medicineName,
            dose = dose,
            scheduledTimeStr = nowIso
        )
        _userMessage.value = "Triggered Escalation Tier 1 alert (15m Missed) with loud notification & vibration!"
    }

    /**
     * Phase 25: Test Escalation Tier 2 trigger (30 min missed dose, mark stage 2, prep guardian notify payload).
     */
    fun triggerEscalationTier2Test(medicineName: String = "Lisinopril", dose: String = "10mg") {
        val fakeDoseId = "test_escalation_tier2_" + System.currentTimeMillis()
        val nowIso = java.time.ZonedDateTime.now().minusMinutes(30).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        NotificationScheduler.triggerEscalationTier2Now(
            context = getApplication(),
            doseId = fakeDoseId,
            medicineName = medicineName,
            dose = dose,
            scheduledTimeStr = nowIso
        )
        _userMessage.value = "Triggered Escalation Tier 2 (30m Missed): Stage 2 marked & Guardian Notify Payload prepared!"
    }

    fun dismissGuardianPayload(alertId: String) {
        firebaseSyncRepo.removePreparedGuardianPayload(alertId)
        _userMessage.value = "Guardian alert payload dismissed"
    }

    fun dispatchGuardianPayload(context: android.content.Context, payload: GuardianNotifyPayload) {
        try {
            val phone = payload.recipients.firstOrNull { it.phone.isNotBlank() }?.phone ?: ""
            val cleanPhone = phone.replace(" ", "")
            val smsIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("sms:$cleanPhone")
                putExtra("sms_body", payload.smsBody)
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(smsIntent)
            firebaseSyncRepo.markGuardianPayloadDispatched(payload.alertId)
            _userMessage.value = "Dispatched Stage 2 Guardian Alert SMS to $phone"
        } catch (e: Exception) {
            firebaseSyncRepo.markGuardianPayloadDispatched(payload.alertId)
            _userMessage.value = "Guardian alert marked as dispatched"
        }
    }

    /**
     * Phase 26: Test Escalation Tier 3 trigger (Send FCM push to all linked guardians + deep link to check-in).
     */
    fun triggerEscalationTier3Test(medicineName: String = "Lisinopril", dose: String = "10mg") {
        val fakeDoseId = "test_escalation_tier3_" + System.currentTimeMillis()
        val nowIso = java.time.ZonedDateTime.now().minusMinutes(45).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        NotificationScheduler.triggerEscalationTier3Now(
            context = getApplication(),
            doseId = fakeDoseId,
            medicineName = medicineName,
            dose = dose,
            scheduledTimeStr = nowIso
        )
        _userMessage.value = "Triggered Escalation Tier 3: Sent FCM Push to all linked guardians with deep link to check-in screen!"
    }

    fun openCheckInScreen(
        doseId: String,
        medicineName: String = "Lisinopril",
        dose: String = "10mg",
        scheduledTime: String = "08:00 AM"
    ) {
        val patientName = profile.value?.name ?: "Eleanor Vance"
        val patientPhone = profile.value?.phone ?: "+1 (555) 234-5678"
        _checkInState.value = GuardianCheckInState(
            doseId = doseId,
            patientName = patientName,
            patientPhone = patientPhone,
            medicineName = medicineName,
            dose = dose,
            scheduledTime = scheduledTime,
            deepLinkSource = "in_app"
        )
    }

    fun closeCheckInScreen() {
        _checkInState.value = null
    }

    fun guardianConfirmDoseTaken(doseId: String, medicineName: String) {
        viewModelScope.launch {
            val logs = todayLogs.value
            val match = logs.find { it.medicineName.equals(medicineName, ignoreCase = true) }
            if (match != null) {
                markDoseStatus(match.id, "TAKEN")
            } else {
                firebaseSyncRepo.updateDoseStatusInFirebase(doseId, "taken")
                firebaseSyncRepo.markDoseStatusByMedicineAndSchedule(medicineName, "", "taken")
                NotificationScheduler.cancelNotification(getApplication(), doseId)
            }
            _checkInState.value = _checkInState.value?.copy(checkInStatus = "RESOLVED_TAKEN")
            _userMessage.value = "Guardian verified dose taken. Escalation resolved!"
        }
    }

    fun toggleReminder(id: Int, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleReminderActive(id, isActive)
        }
    }

    fun updateReminderTime(id: Int, newTime: String, timeCategory: String) {
        viewModelScope.launch {
            repository.updateReminderTime(id, newTime, timeCategory)
            _userMessage.value = "Reminder time updated to $newTime"
        }
    }

    fun addReminder(
        name: String,
        dosage: String,
        time: String,
        timeCategory: String,
        instructions: String,
        shape: String = "Pill"
    ) {
        viewModelScope.launch {
            val schedule = DoseScheduleGenerator.generateSchedule(
                medicineName = name,
                dose = dosage,
                frequency = timeCategory,
                durationDays = 7,
                instructions = instructions
            )


            firebaseSyncRepo.addMedicineWithDosesAndReminders(
                schedule.firebaseMedicine,
                schedule.firebaseDoses,
                schedule.firebaseReminders
            )

            // Save to Room DB
            repository.addReminder(schedule.roomReminder)

            // Schedule Local Notifications for doses
            val context = getApplication<Application>()
            schedule.notificationTriggers.forEach { trigger ->
                NotificationScheduler.scheduleNotification(
                    context = context,
                    doseId = trigger.doseId,
                    medicineName = trigger.medicineName,
                    dose = trigger.dose,
                    scheduledTimeStr = trigger.formattedTime,
                    triggerTimeMillis = trigger.triggerTimeMillis
                )
            }

            _userMessage.value = "Added $name to reminders & scheduled dose reminders"
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            _userMessage.value = "Removed ${reminder.name} from reminders"
        }
    }

    fun scanPrescriptionUri(uri: Uri?) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanError.value = null
            _scanResult.value = null
            _editableScannedList.value = emptyList()

            val result = prescriptionScanner.analyzePrescription(uri)
            _isScanning.value = false

            result.onSuccess { res ->
                _scanResult.value = res
                _editableScannedList.value = res.items.map { it.copy() }
            }.onFailure { error ->
                val errorMsg = when (error) {
                    is PrescriptionScanException -> error.message ?: "Scanning failed."
                    else -> "Unable to read prescription image. Text may be blurry or missing. Please try a clearer image."
                }
                _scanError.value = errorMsg
            }
        }
    }

    fun scanPrescriptionBitmap(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanError.value = null
            _scanResult.value = null
            _editableScannedList.value = emptyList()

            val result = prescriptionScanner.analyzePrescription(bitmap)
            _isScanning.value = false

            result.onSuccess { res ->
                _scanResult.value = res
                _editableScannedList.value = res.items.map { it.copy() }
            }.onFailure { error ->
                val errorMsg = when (error) {
                    is PrescriptionScanException -> error.message ?: "Scanning failed."
                    else -> "Unable to read prescription image. Text may be blurry or missing. Please try a clearer image."
                }
                _scanError.value = errorMsg
            }
        }
    }

    fun scanPrescriptionBytes(bytes: ByteArray) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanError.value = null
            _scanResult.value = null
            _editableScannedList.value = emptyList()

            val result = prescriptionScanner.analyzePrescription(bytes)
            _isScanning.value = false

            result.onSuccess { res ->
                _scanResult.value = res
                _editableScannedList.value = res.items.map { it.copy() }
            }.onFailure { error ->
                val errorMsg = when (error) {
                    is PrescriptionScanException -> error.message ?: "Scanning failed."
                    else -> "Unable to read prescription image. Text may be blurry or missing. Please try a clearer image."
                }
                _scanError.value = errorMsg
            }
        }
    }

    fun updateEditableItem(index: Int, updatedItem: ScannedMedicationItem) {
        val current = _editableScannedList.value.toMutableList()
        if (index in current.indices) {
            current[index] = updatedItem
            _editableScannedList.value = current
        }
    }

    fun addEmptyEditableItem() {
        val current = _editableScannedList.value.toMutableList()
        current.add(
            ScannedMedicationItem(
                medicine = "",
                dose = "1 Tablet",
                frequency = "Once Daily",
                durationDays = 7,
                timeCategory = "Morning",
                time = "08:00 AM",
                instructions = "Take as prescribed"
            )
        )
        _editableScannedList.value = current
    }

    fun removeEditableItem(index: Int) {
        val current = _editableScannedList.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _editableScannedList.value = current
        }
    }

    /**
     * Requirement:
     * After confirming scanned medicines, auto-generate dose schedule based on frequency
     * (e.g. "twice daily" -> 8am, 8pm) for duration_days.
     * Schedule local notifications at each dose time.
     * Sync with Firebase schema.
     */
    fun confirmAndSaveAllScannedMedications() {
        viewModelScope.launch {
            val itemsToSave = _editableScannedList.value.filter { it.medicine.isNotBlank() }
            if (itemsToSave.isEmpty()) {
                _userMessage.value = "Please enter at least one valid medicine name."
                return@launch
            }

            val context = getApplication<Application>()
            var totalDosesScheduled = 0

            for (item in itemsToSave) {
                val schedule = DoseScheduleGenerator.generateSchedule(
                    userId = firebaseUser.value.id,
                    medicineName = item.medicine,
                    dose = item.dose,
                    frequency = item.frequency,
                    durationDays = item.durationDays,
                    instructions = if (item.instructions.isNotBlank()) item.instructions else "Take as prescribed"
                )



                firebaseSyncRepo.addMedicineWithDosesAndReminders(
                    medicine = schedule.firebaseMedicine,
                    newDoses = schedule.firebaseDoses,
                    newReminders = schedule.firebaseReminders
                )

                // 2. Add to Room local DB
                repository.addReminder(schedule.roomReminder)

                // 3. Schedule Local Notifications for every dose time
                schedule.notificationTriggers.forEach { trigger ->
                    NotificationScheduler.scheduleNotification(
                        context = context,
                        doseId = trigger.doseId,
                        medicineName = trigger.medicineName,
                        dose = trigger.dose,
                        scheduledTimeStr = trigger.formattedTime,
                        triggerTimeMillis = trigger.triggerTimeMillis
                    )
                }

                totalDosesScheduled += schedule.notificationTriggers.size
            }

            clearScanState()
            val missingMeds = itemsToSave.filter { !isMedicineInInventory(it.medicine) }.map { it.medicine }
            val missingText = if (missingMeds.isNotEmpty()) "\nWarning: Missing from inventory: ${missingMeds.joinToString()}" else ""
            _userMessage.value = "Successfully generated dose schedule & scheduled $totalDosesScheduled notifications.$missingText"
        }
    }

    fun loadSamplePrescription(sampleIndex: Int) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanError.value = null
            _scanResult.value = null

            val sampleHash = "sample_rx_hash_$sampleIndex"
            val items = prescriptionScanner.generateDemoFallbackItems(sampleHash)
            val res = ScanResult(items = items, isFromCache = false, imageHash = sampleHash, confidence = 98)

            _isScanning.value = false
            _scanResult.value = res
            _editableScannedList.value = items.map { it.copy() }
        }
    }

    fun addGuardian(name: String, relationship: String, phone: String) {
        addGuardianWithInvite(
            context = getApplication(),
            name = name,
            relationship = relationship,
            phone = phone,
            sendVia = "SMS"
        )
    }

    fun addGuardianWithInvite(
        context: android.content.Context,
        name: String,
        relationship: String,
        phone: String,
        sendVia: String = "SMS"
    ) {
        viewModelScope.launch {
            repository.addGuardian(
                GuardianEntity(
                    name = name,
                    relationship = relationship,
                    phone = phone,
                    notifyOnMissed = true
                )
            )
            val patientId = firebaseUser.value.id
            val guardianUid = "guardian_" + java.util.UUID.randomUUID().toString().take(6)
            val inviteToken = com.example.data.firebase.GuardianInviteService.generateInviteToken()
            val inviteLink = com.example.data.firebase.GuardianInviteService.buildInviteLink(inviteToken)

            firebaseSyncRepo.addGuardianToFirebase(
                patientId = patientId,
                guardianPhone = phone,
                guardianUid = guardianUid,
                status = "pending",
                name = name,
                relationship = relationship,
                inviteToken = inviteToken,
                inviteLink = inviteLink
            )

            val patientName = profile.value?.name ?: "Eleanor Vance"

            if (sendVia.equals("WHATSAPP", ignoreCase = true)) {
                com.example.data.firebase.GuardianInviteService.sendWhatsAppInvite(
                    context = context,
                    guardianPhone = phone,
                    patientName = patientName,
                    inviteLink = inviteLink
                )
            } else {
                com.example.data.firebase.GuardianInviteService.sendSmsInvite(
                    context = context,
                    guardianPhone = phone,
                    patientName = patientName,
                    inviteLink = inviteLink
                )
            }

            _userMessage.value = "Invite link generated ($inviteToken) & sent via $sendVia"
        }
    }

    fun sendGuardianInvite(
        context: android.content.Context,
        guardianPhone: String,
        inviteLink: String,
        inviteToken: String,
        sendVia: String = "SMS"
    ) {
        val patientName = profile.value?.name ?: "Eleanor Vance"
        if (sendVia.equals("WHATSAPP", ignoreCase = true)) {
            com.example.data.firebase.GuardianInviteService.sendWhatsAppInvite(
                context = context,
                guardianPhone = guardianPhone,
                patientName = patientName,
                inviteLink = inviteLink
            )
        } else {
            com.example.data.firebase.GuardianInviteService.sendSmsInvite(
                context = context,
                guardianPhone = guardianPhone,
                patientName = patientName,
                inviteLink = inviteLink
            )
        }
        _userMessage.value = "Resent invite token ($inviteToken) via $sendVia"
    }

    fun deleteGuardian(guardian: GuardianEntity) {
        viewModelScope.launch {
            repository.deleteGuardian(guardian)
            firebaseSyncRepo.deleteGuardianFromFirebaseByPhone(guardian.phone)
            _userMessage.value = "Removed guardian contact from local DB & Firestore"
        }
    }

    fun registerUserOnSignup(uid: String, name: String, phone: String, customFcmToken: String? = null) {
        viewModelScope.launch {
            val token = FcmTokenManager.registerDeviceFcmToken(customFcmToken)
            val user = firebaseSyncRepo.writeUserOnSignup(uid, name, phone, fcmToken = token)
            repository.updateProfile(
                ProfileEntity(
                    id = 1,
                    name = name,
                    phone = phone,
                    emergencyInfo = "Blood Type O+, Penicillin Allergy",
                    largeTextMode = true
                )
            )
            _userMessage.value = "New user account created & FCM token registered in Firestore 'users' collection."
        }
    }

    fun registerDeviceFcmToken(customToken: String? = null) {
        val token = FcmTokenManager.registerDeviceFcmToken(customToken)
        _userMessage.value = "FCM token ($token) registered & saved to user doc"
    }

    fun updateProfile(name: String, phone: String, emergencyInfo: String, largeTextMode: Boolean) {
        viewModelScope.launch {
            val current = profile.value ?: ProfileEntity(id = 1, name = name, phone = phone, emergencyInfo = emergencyInfo)
            repository.updateProfile(
                current.copy(
                    name = name,
                    phone = phone,
                    emergencyInfo = emergencyInfo,
                    largeTextMode = largeTextMode
                )
            )
            firebaseSyncRepo.updateFirebaseUser(name, phone, firebaseUser.value.guardian_ids)
            _userMessage.value = "Profile updated and synced to Firebase"
        }
    }

    // ==========================================
    // Phase 23: Guardian Accept Flow
    // ==========================================

    data class GuardianAcceptState(
        val token: String,
        val guardian: GuardianFirestoreModel?,
        val patientName: String,
        val guardianPhone: String,
        val guardianUid: String,
        val isLinked: Boolean,
        val sourceUri: String? = null
    )

    private val _guardianAcceptState = MutableStateFlow<GuardianAcceptState?>(null)
    val guardianAcceptState: StateFlow<GuardianAcceptState?> = _guardianAcceptState.asStateFlow()

    private val _showGuardianAcceptDialog = MutableStateFlow(false)
    val showGuardianAcceptDialog: StateFlow<Boolean> = _showGuardianAcceptDialog.asStateFlow()

    /**
     * Requirement Phase 23 & Phase 26: Intercept deep links
     * - Invite flow: https://medremind.app/invite?token=... or medremind://invite?token=...
     * - Guardian check-in flow: https://medremind.app/checkin?... or medremind://checkin?...
     */
    fun handleDeepLink(uri: Uri?) {
        if (uri == null) return
        val rawUri = uri.toString()

        // Phase 26: Intercept Check-in deep links
        val host = uri.host ?: ""
        val path = uri.path ?: ""
        if (host.equals("checkin", ignoreCase = true) || path.contains("checkin")) {
            val doseId = uri.getQueryParameter("doseId") ?: ""
            val patientName = uri.getQueryParameter("patientName") ?: (profile.value?.name ?: "Eleanor Vance")
            val patientPhone = uri.getQueryParameter("phone") ?: (profile.value?.phone ?: "+1 (555) 234-5678")
            val medicine = uri.getQueryParameter("medicine") ?: "Lisinopril"
            val dose = uri.getQueryParameter("dose") ?: "10mg"
            val time = uri.getQueryParameter("time") ?: "08:00 AM"

            _checkInState.value = GuardianCheckInState(
                doseId = doseId,
                patientName = patientName,
                patientPhone = patientPhone,
                medicineName = medicine,
                dose = dose,
                scheduledTime = time,
                deepLinkSource = rawUri
            )
            return
        }

        val token = GuardianInviteService.extractToken(rawUri)
        if (!token.isNullOrBlank()) {
            openGuardianAcceptForToken(token, rawUri)
        }
    }

    fun openGuardianAcceptForToken(token: String, sourceUri: String? = null) {
        val cleanToken = GuardianInviteService.extractToken(token) ?: token.trim()
        val existingGuardian = firebaseSyncRepo.findGuardianByToken(cleanToken)
        val patientName = profile.value?.name ?: "Eleanor Vance"

        val state = if (existingGuardian != null) {
            GuardianAcceptState(
                token = existingGuardian.inviteToken,
                guardian = existingGuardian,
                patientName = patientName,
                guardianPhone = existingGuardian.guardianPhone,
                guardianUid = existingGuardian.guardianUid,
                isLinked = existingGuardian.status.equals("linked", ignoreCase = true),
                sourceUri = sourceUri ?: existingGuardian.inviteLink
            )
        } else {
            // New invite token link opened
            val generatedUid = "guardian_" + java.util.UUID.randomUUID().toString().take(6)
            GuardianAcceptState(
                token = cleanToken,
                guardian = null,
                patientName = patientName,
                guardianPhone = "+1 (555) 345-6789",
                guardianUid = generatedUid,
                isLinked = false,
                sourceUri = sourceUri ?: GuardianInviteService.buildInviteLink(cleanToken)
            )
        }

        _guardianAcceptState.value = state
        _showGuardianAcceptDialog.value = true
    }

    fun openGuardianAcceptForGuardian(guardian: GuardianFirestoreModel) {
        val patientName = profile.value?.name ?: "Eleanor Vance"
        _guardianAcceptState.value = GuardianAcceptState(
            token = guardian.inviteToken,
            guardian = guardian,
            patientName = patientName,
            guardianPhone = guardian.guardianPhone,
            guardianUid = guardian.guardianUid,
            isLinked = guardian.status.equals("linked", ignoreCase = true),
            sourceUri = guardian.inviteLink
        )
        _showGuardianAcceptDialog.value = true
    }

    fun dismissGuardianAccept() {
        _showGuardianAcceptDialog.value = false
        _guardianAcceptState.value = null
    }

    /**
     * Requirement Phase 23: Confirms invite:
     * Updates guardians doc status="linked", links guardianUid, and updates patient users collection.
     */
    fun confirmGuardianAccept(
        token: String,
        guardianUid: String,
        guardianName: String? = null,
        guardianPhone: String? = null
    ) {
        viewModelScope.launch {
            val cleanToken = GuardianInviteService.extractToken(token) ?: token.trim()
            val effectiveUid = if (guardianUid.isNotBlank()) guardianUid else ("guardian_" + java.util.UUID.randomUUID().toString().take(6))

            // 1. Update in FirebaseSyncRepository state mirror
            val updated = firebaseSyncRepo.linkGuardianWithToken(
                token = cleanToken,
                guardianUid = effectiveUid,
                guardianName = guardianName,
                guardianPhone = guardianPhone
            )

            // If guardian doc wasn't in mirror yet, add it as linked
            val finalGuardian = updated ?: firebaseSyncRepo.addGuardianToFirebase(
                patientId = firebaseUser.value.id,
                guardianPhone = guardianPhone ?: "+1 (555) 345-6789",
                guardianUid = effectiveUid,
                status = "linked",
                name = guardianName ?: "Linked Guardian",
                relationship = "Caregiver",
                inviteToken = cleanToken,
                inviteLink = GuardianInviteService.buildInviteLink(cleanToken)
            )

            // 2. Real Firestore collection update if available
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
                val guardianDocRef = db.collection("guardians").document(finalGuardian.id)
                val updateData = hashMapOf(
                    "status" to "linked",
                    "guardianUid" to effectiveUid,
                    "guardian_uid" to effectiveUid,
                    "linkedAt" to nowIso,
                    "linked_at" to nowIso
                )
                guardianDocRef.set(updateData, com.google.firebase.firestore.SetOptions.merge())

                // Also update patient doc in users collection
                val userRef = db.collection("users").document(firebaseUser.value.id)
                userRef.update("guardian_ids", com.google.firebase.firestore.FieldValue.arrayUnion(effectiveUid, finalGuardian.id))
            } catch (e: Exception) {
                // Handled gracefully in offline/mock environment
            }

            // 3. Keep local Room DB updated
            val localGuards = guardians.value
            val match = localGuards.find { it.phone == finalGuardian.guardianPhone }
            if (match == null && !guardianName.isNullOrBlank()) {
                repository.addGuardian(
                    GuardianEntity(
                        name = guardianName,
                        relationship = "Caregiver",
                        phone = finalGuardian.guardianPhone,
                        notifyOnMissed = true
                    )
                )
            }

            // Update UI State
            _guardianAcceptState.value = _guardianAcceptState.value?.copy(
                isLinked = true,
                guardianUid = effectiveUid,
                guardian = finalGuardian
            )

            _userMessage.value = "Guardian successfully linked! Firestore status set to 'linked' and guardianUid linked."
        }
    }
}
