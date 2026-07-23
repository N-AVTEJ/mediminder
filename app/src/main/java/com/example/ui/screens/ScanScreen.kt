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
import com.example.data.api.ScannedMedication
import com.example.ui.theme.*
import com.example.ui.viewmodel.MedicineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: MedicineViewModel,
    onSavedSuccessfully: () -> Unit
) {
    val isScanning by viewModel.isScanning.collectAsState()
    val scannedMedication by viewModel.scannedMedication.collectAsState()

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
                            text = "Upload or take a photo of your prescription bottle, box, or paper. Gemini AI will automatically extract medicine details.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnMintContainer.copy(alpha = 0.8f),
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
                        title = "Lisinopril",
                        subtitle = "10mg BP Pill",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sample_rx_1"),
                        onClick = { viewModel.loadSamplePrescription(1) }
                    )
                    SamplePrescriptionCard(
                        title = "Vitamin D3",
                        subtitle = "2000 IU Capsule",
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
                                    text = "Reading medicine name, dosage, and schedule...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Scanned Result Card Preview
            scannedMedication?.let { scanned ->
                item {
                    ScannedResultCard(
                        scanned = scanned,
                        onSave = { updatedScanned ->
                            viewModel.saveScannedMedication(updatedScanned)
                            onSavedSuccessfully()
                        }
                    )
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
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScannedResultCard(
    scanned: ScannedMedication,
    onSave: (ScannedMedication) -> Unit
) {
    var medName by remember(scanned) { mutableStateOf(scanned.medicineName) }
    var dosage by remember(scanned) { mutableStateOf(scanned.dosage) }
    var instructions by remember(scanned) { mutableStateOf(scanned.instructions) }
    var timeCategory by remember(scanned) { mutableStateOf(scanned.timeCategory) }
    var suggestedTime by remember(scanned) { mutableStateOf(scanned.suggestedTimes.firstOrNull() ?: "08:00 AM") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scanned_result_card")
            .border(2.dp, TealPrimary, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    text = "AI Extracted Prescription",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TakenGreenSurface
                ) {
                    Text(
                        text = "${scanned.confidence}% Match",
                        style = MaterialTheme.typography.labelLarge,
                        color = TakenGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            OutlinedTextField(
                value = medName,
                onValueChange = { medName = it },
                label = { Text("Medicine Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_scanned_name"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("Dosage") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_scanned_dosage"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Instructions") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_scanned_instructions"),
                shape = RoundedCornerShape(12.dp)
            )

            // Time Category Chips
            Text(
                text = "Reminder Schedule Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        },
                        label = { Text(cat, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onSave(
                        scanned.copy(
                            medicineName = medName,
                            dosage = dosage,
                            instructions = instructions,
                            timeCategory = timeCategory,
                            suggestedTimes = listOf(suggestedTime)
                        )
                    )
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
                Text("Save to Active Reminders", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
