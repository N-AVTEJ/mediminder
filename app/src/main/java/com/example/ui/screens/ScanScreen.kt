package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.ScannedMedicationItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MedicineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: MedicineViewModel,
    onSavedSuccessfully: () -> Unit
) {
    val isScanning by viewModel.isScanning.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val scanError by viewModel.scanError.collectAsState()
    val editableList by viewModel.editableScannedList.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.scanPrescriptionUri(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scan Prescription",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Scanner Instruction Hero Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MintContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(TealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "AI Prescription Scanner",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnMintContainer
                        )

                        Text(
                            text = "Upload a photo of your prescription or medicine container. Gemini AI will extract details into an editable list.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnMintContainer.copy(alpha = 0.85f),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("upload_prescription_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Upload / Take Prescription Photo",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Test Sample Prescriptions
            item {
                Text(
                    text = "Or Try Sample Prescriptions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SamplePrescriptionCard(
                        title = "Amoxicillin",
                        subtitle = "500mg Antibiotic",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sample_rx_0"),
                        onClick = { viewModel.loadSamplePrescription(0) }
                    )
                    SamplePrescriptionCard(
                        title = "Lisinopril + Stat",
                        subtitle = "Dual RX Sample",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sample_rx_1"),
                        onClick = { viewModel.loadSamplePrescription(1) }
                    )
                    SamplePrescriptionCard(
                        title = "Metformin",
                        subtitle = "500mg ER Meal",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sample_rx_2"),
                        onClick = { viewModel.loadSamplePrescription(2) }
                    )
                }
            }

            // Scanning Loading Spinner
            if (isScanning) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BlueContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = BlueSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Column {
                                Text(
                                    text = "Analyzing Prescription with Gemini AI...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Extracting medicine name, dose, frequency & duration...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Friendly Error Card
            scanError?.let { error ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scan_error_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraEnhance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Prescription Read Issue",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }

                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Try Clearer Photo")
                                }

                                OutlinedButton(
                                    onClick = { viewModel.clearScanState() },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }
            }

            // Scanned Editable Results List
            if (editableList.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Extracted Medications (${editableList.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                        }

                        // Requirement 5: Cache indicator badge
                        if (scanResult?.isFromCache == true) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BlueContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BlueSecondary)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = BlueSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Instant Hash Cache",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BlueSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Review and edit any extracted details before adding to your active reminders.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                // Render editable card for each extracted medication item
                itemsIndexed(editableList, key = { index, item -> item.id }) { index, item ->
                    EditableMedicationCard(
                        index = index,
                        item = item,
                        canDelete = editableList.size > 1,
                        onUpdate = { updated -> viewModel.updateEditableItem(index, updated) },
                        onDelete = { viewModel.removeEditableItem(index) }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { viewModel.addEmptyEditableItem() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_another_scanned_med_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Another Medicine Row")
                    }
                }

                item {
                    Button(
                        onClick = {
                            viewModel.confirmAndSaveAllScannedMedications()
                            onSavedSuccessfully()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("save_scanned_reminder_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm & Save All Reminders", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    TextButton(
                        onClick = { viewModel.clearScanState() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("discard_scanned_results_btn")
                    ) {
                        Text("Discard Results", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SamplePrescriptionCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Medication,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableMedicationCard(
    index: Int,
    item: ScannedMedicationItem,
    canDelete: Boolean,
    onUpdate: (ScannedMedicationItem) -> Unit,
    onDelete: () -> Unit
) {
    var medicineName by remember(item) { mutableStateOf(item.medicine) }
    var dose by remember(item) { mutableStateOf(item.dose) }
    var frequency by remember(item) { mutableStateOf(item.frequency) }
    var durationDaysStr by remember(item) { mutableStateOf(item.durationDays.toString()) }
    var timeCategory by remember(item) { mutableStateOf(item.timeCategory) }
    var suggestedTime by remember(item) { mutableStateOf(item.time) }
    var instructions by remember(item) { mutableStateOf(item.instructions) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scanned_result_card_$index")
            .border(1.5.dp, TealPrimary.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Medicine #${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary
                )

                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove Item",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Medicine Name
            OutlinedTextField(
                value = medicineName,
                onValueChange = {
                    medicineName = it
                    onUpdate(item.copy(medicine = it))
                },
                label = { Text("Medicine Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_scanned_name_$index"),
                shape = RoundedCornerShape(12.dp)
            )

            // Dose & Duration Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = dose,
                    onValueChange = {
                        dose = it
                        onUpdate(item.copy(dose = it))
                    },
                    label = { Text("Dose") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_scanned_dosage_$index"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = durationDaysStr,
                    onValueChange = {
                        durationDaysStr = it
                        val parsed = it.toIntOrNull() ?: 7
                        onUpdate(item.copy(durationDays = parsed))
                    },
                    label = { Text("Duration (Days)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_scanned_duration_$index"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Frequency
            OutlinedTextField(
                value = frequency,
                onValueChange = {
                    frequency = it
                    onUpdate(item.copy(frequency = it))
                },
                label = { Text("Frequency") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_scanned_frequency_$index"),
                shape = RoundedCornerShape(12.dp)
            )

            // Schedule Category Chips
            Text(
                text = "Reminder Schedule",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val categories = listOf("Morning", "Afternoon", "Evening", "Night")
                categories.forEach { cat ->
                    FilterChip(
                        selected = timeCategory == cat,
                        onClick = {
                            timeCategory = cat
                            suggestedTime = when (cat) {
                                "Morning" -> "08:00 AM"
                                "Afternoon" -> "01:00 PM"
                                "Evening" -> "06:30 PM"
                                else -> "09:00 PM"
                            }
                            onUpdate(
                                item.copy(
                                    timeCategory = cat,
                                    time = suggestedTime
                                )
                            )
                        },
                        label = { Text(cat, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Special Instructions
            OutlinedTextField(
                value = instructions,
                onValueChange = {
                    instructions = it
                    onUpdate(item.copy(instructions = it))
                },
                label = { Text("Instructions") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
