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

    // Supabase Inventory & Affiliate Clicks State Flow
    val userInventory = PharmacyAffiliateService.inventoryTable
    val affiliateClicks = PharmacyAffiliateService.affiliateClicksTable

    fun isMedicineInInventory(medicineName: String): Boolean {
        return PharmacyAffiliateService.isMedicineInInventory(medicineName)
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
        _userMessage.value = "Opening ${provider.displayName} for $medicineName (Logged affiliate earnings tracking)"
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
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun clearScanState() {
        _scanResult.value = null
        _scanError.value = null
        _editableScannedList.value = emptyList()
    }

    fun markDoseStatus(logId: Int, status: String) {
        viewModelScope.launch {
            repository.updateDoseStatus(logId, status)

            // Sync with Firebase
            val logs = todayLogs.value
            val target = logs.find { it.id == logId }
            if (target != null) {
                firebaseSyncRepo.markDoseTakenByMedicineAndSchedule(target.medicineName, target.scheduledTime)
            }

            when (status) {
                "TAKEN" -> _userMessage.value = "Great job! Dose logged as taken and synced to Firebase."
                "MISSED" -> _userMessage.value = "Dose marked as missed. Guardian alerted & updated in Firebase."
                "SNOOZED" -> _userMessage.value = "Reminder snoozed for 15 minutes."
            }
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

            // Save to Firebase
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

                // 1. Sync with Firebase Schema (medicines, doses, reminders)
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
            _userMessage.value = "Successfully generated dose schedule & scheduled $totalDosesScheduled notifications across Firebase!"
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
        viewModelScope.launch {
            repository.addGuardian(
                GuardianEntity(
                    name = name,
                    relationship = relationship,
                    phone = phone,
                    notifyOnMissed = true
                )
            )
            _userMessage.value = "Added $name as guardian contact"
        }
    }

    fun deleteGuardian(guardian: GuardianEntity) {
        viewModelScope.launch {
            repository.deleteGuardian(guardian)
            _userMessage.value = "Removed guardian contact"
        }
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
}
