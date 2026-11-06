package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.GuardianCheckInState
import com.example.ui.theme.TakenGreen
import com.example.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    state: GuardianCheckInState,
    onDismiss: () -> Unit,
    onMarkTakenOnBehalf: (doseId: String, medicineName: String) -> Unit,
    onRecordNote: (note: String) -> Unit = {}
) {
    val context = LocalContext.current
    var noteText by remember { mutableStateOf("") }
    var noteSaved by remember { mutableStateOf(false) }
    var resolvedTaken by remember(state.checkInStatus) { mutableStateOf(state.checkInStatus == "RESOLVED_TAKEN") }

    BackHandler {
        onDismiss()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Guardian Check-in",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tier 3 Missed Dose Escalation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_checkin_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("checkin_screen_container")
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Urgent Missed Dose Alert Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkin_dose_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = Color(0xFFB71C1C),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "⚠️ TIER 3 ESCALATION • 45m OVERDUE",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (resolvedTaken) {
                            Surface(
                                color = TakenGreen,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "RESOLVED ✓",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "${state.medicineName} — ${state.dose}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Text(
                        text = "Scheduled at ${state.scheduledTime}. Patient missed standard alert & 15-min and 30-min escalations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Patient Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkin_patient_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(TealPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.patientName.take(2).uppercase(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.patientName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Phone: ${state.patientPhone}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Caregiver Quick Actions Header
            Text(
                text = "Caregiver Check-in Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 1. Call Patient Button
            FilledTonalButton(
                onClick = {
                    try {
                        val cleanPhone = state.patientPhone.replace(" ", "")
                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                        context.startActivity(callIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Dialing ${state.patientPhone}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("call_patient_checkin_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Call ${state.patientName}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. Send SMS Check-in Text Button
            OutlinedButton(
                onClick = {
                    try {
                        val cleanPhone = state.patientPhone.replace(" ", "")
                        val textBody = "Hi ${state.patientName}, I received a caregiver alert that you missed your ${state.medicineName} (${state.dose}) scheduled for ${state.scheduledTime}. Are you doing okay? Did you take it?"
                        val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("sms:$cleanPhone")
                            putExtra("sms_body", textBody)
                        }
                        context.startActivity(smsIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "SMS text prepped for ${state.patientPhone}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sms_patient_checkin_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send Check-in SMS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 3. Mark Dose Taken on Behalf of Patient
            Button(
                onClick = {
                    resolvedTaken = true
                    onMarkTakenOnBehalf(state.doseId, state.medicineName)
                    Toast.makeText(context, "Confirmed! Dose marked TAKEN on behalf of patient.", Toast.LENGTH_LONG).show()
                },
                enabled = !resolvedTaken,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("guardian_mark_taken_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TakenGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (resolvedTaken) "Verified & Resolved ✓" else "Verify: Patient Took Medication",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 4. Log Caregiver Note
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Caregiver Check-in Log",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("e.g., Spoke with Eleanor on phone; she took the pill with warm water.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("guardian_note_input"),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 2
                    )

                    Button(
                        onClick = {
                            if (noteText.isNotBlank()) {
                                noteSaved = true
                                onRecordNote(noteText)
                                Toast.makeText(context, "Check-in note logged to patient record", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("guardian_save_note_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (noteSaved) "Note Saved ✓" else "Save Note")
                    }
                }
            }
        }
    }
}
