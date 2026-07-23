package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.PrescriptionScanner
import com.example.data.api.ScannedMedication
import com.example.data.db.*
import com.example.data.repository.MedicineRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MedicineViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = MedicineRepository(db)
    private val prescriptionScanner = PrescriptionScanner(application)

    val reminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayLogs: StateFlow<List<DoseLogEntity>> = repository.getTodayLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val guardians: StateFlow<List<GuardianEntity>> = repository.allGuardians
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profile: StateFlow<ProfileEntity?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedMedication = MutableStateFlow<ScannedMedication?>(null)
    val scannedMedication: StateFlow<ScannedMedication?> = _scannedMedication.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultDataSeeded()
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun markDoseStatus(logId: Int, status: String) {
        viewModelScope.launch {
            repository.updateDoseStatus(logId, status)
            when (status) {
                "TAKEN" -> _userMessage.value = "Great job! Dose logged as taken."
                "MISSED" -> _userMessage.value = "Dose marked as missed. Guardian alerted."
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
            val colorHex = when (timeCategory) {
                "Morning" -> "#00796B"
                "Afternoon" -> "#0288D1"
                "Evening" -> "#E65100"
                else -> "#6A1B9A"
            }
            repository.addReminder(
                ReminderEntity(
                    name = name,
                    dosage = dosage,
                    time = time,
                    timeCategory = timeCategory,
                    instructions = instructions,
                    isActive = true,
                    colorHex = colorHex,
                    shape = shape
                )
            )
            _userMessage.value = "Added $name to reminders"
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
            _scannedMedication.value = null
            val result = prescriptionScanner.analyzePrescription(uri)
            _isScanning.value = false
            result.onSuccess { scanned ->
                _scannedMedication.value = scanned
            }.onFailure {
                _userMessage.value = "Could not scan image. Try selecting a sample prescription."
            }
        }
    }

    fun loadSamplePrescription(sampleIndex: Int) {
        viewModelScope.launch {
            _isScanning.value = true
            _scannedMedication.value = null
            val scanned = when (sampleIndex) {
                0 -> ScannedMedication(
                    medicineName = "Amoxicillin",
                    dosage = "500 mg Capsule",
                    frequency = "Twice Daily (Morning & Night)",
                    instructions = "Take after meal with a full glass of water.",
                    timeCategory = "Morning",
                    suggestedTimes = listOf("08:00 AM", "08:00 PM"),
                    confidence = 98
                )
                1 -> ScannedMedication(
                    medicineName = "Lisinopril",
                    dosage = "10 mg Tablet",
                    frequency = "Once Daily in Morning",
                    instructions = "Take in the morning with or without food.",
                    timeCategory = "Morning",
                    suggestedTimes = listOf("08:00 AM"),
                    confidence = 96
                )
                else -> ScannedMedication(
                    medicineName = "Vitamin D3",
                    dosage = "2000 IU Softgel",
                    frequency = "Once Daily with lunch",
                    instructions = "Take with main meal containing healthy fats.",
                    timeCategory = "Afternoon",
                    suggestedTimes = listOf("01:00 PM"),
                    confidence = 99
                )
            }
            _isScanning.value = false
            _scannedMedication.value = scanned
        }
    }

    fun saveScannedMedication(scanned: ScannedMedication) {
        viewModelScope.launch {
            addReminder(
                name = scanned.medicineName,
                dosage = scanned.dosage,
                time = scanned.suggestedTimes.firstOrNull() ?: "08:00 AM",
                timeCategory = scanned.timeCategory,
                instructions = scanned.instructions,
                shape = "Pill"
            )
            _scannedMedication.value = null
            _userMessage.value = "Saved ${scanned.medicineName} to active reminders!"
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
            _userMessage.value = "Profile updated successfully"
        }
    }
}
