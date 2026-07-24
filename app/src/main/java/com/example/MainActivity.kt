package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.notifications.DoseNotificationReceiver
import com.example.notifications.NotificationScheduler
import com.example.ui.components.MainAppContainer
import com.example.ui.theme.MedicineReminderTheme
import com.example.ui.viewmodel.MedicineViewModel

class MainActivity : ComponentActivity() {

    private val medicineViewModel: MedicineViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification reminders enabled!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()
        handleNotificationIntent(intent)

        setContent {
            MedicineReminderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainAppContainer(viewModel = medicineViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.getStringExtra("ACTION") ?: intent.action
        if (action == DoseNotificationReceiver.ACTION_MARK_TAKEN) {
            val doseId = intent.getStringExtra(NotificationScheduler.EXTRA_DOSE_ID) ?: ""
            val medicineName = intent.getStringExtra(NotificationScheduler.EXTRA_MEDICINE_NAME) ?: ""
            val scheduledTime = intent.getStringExtra(NotificationScheduler.EXTRA_SCHEDULED_TIME) ?: ""

            if (medicineName.isNotBlank()) {
                medicineViewModel.firebaseMedicines.value
                // Find matching log in today's logs and mark taken
                val logs = medicineViewModel.todayLogs.value
                val matchedLog = logs.find { it.medicineName.equals(medicineName, ignoreCase = true) }
                if (matchedLog != null) {
                    medicineViewModel.markDoseStatus(matchedLog.id, "TAKEN")
                } else {
                    Toast.makeText(this, "Marked $medicineName dose as TAKEN!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
