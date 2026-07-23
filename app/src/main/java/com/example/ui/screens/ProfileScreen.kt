package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.GuardianEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MedicineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MedicineViewModel
) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val guardians by viewModel.guardians.collectAsState()

    var showEditProfileModal by remember { mutableStateOf(false) }
    var showAddGuardianModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "User Profile & Care",
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
            // User Header Profile Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_profile_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(TealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile?.name ?: "Eleanor Vance",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = profile?.phone ?: "+1 (555) 234-5678",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Emergency: ${profile?.emergencyInfo ?: "Blood Type O+, Penicillin Allergy"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TealPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            IconButton(
                                onClick = { showEditProfileModal = true },
                                modifier = Modifier.testTag("edit_profile_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = TealPrimary
                                )
                            }
                        }
                    }
                }
            }

            // ⭐ Premium Status Badge
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("premium_status_card")
                        .border(2.dp, Color(0xFFFFB300), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF8E1)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = Color(0xFFFF8F00),
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = "CAREPLUS PREMIUM ACTIVE",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                                Text(
                                    text = "All elder care features unlocked for your account.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFBF360C)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PremiumFeatureChip(text = "Unlimited Scan")
                            PremiumFeatureChip(text = "SMS Guardian Alerts")
                            PremiumFeatureChip(text = "Cloud Backup")
                        }
                    }
                }
            }

            // Guardian Contacts List Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Guardian Contacts (${guardians.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { showAddGuardianModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_guardian_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Guardian", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (guardians.isEmpty()) {
                item {
                    Text(
                        text = "No guardians added yet. Guardians receive alerts when doses are missed.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(guardians, key = { it.id }) { guardian ->
                    GuardianCardItem(
                        guardian = guardian,
                        onCall = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${guardian.phone}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Dialing ${guardian.phone}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSendAlert = {
                            Toast.makeText(
                                context,
                                "Test missed-dose SMS alert sent to ${guardian.name} (${guardian.phone})",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        onDelete = {
                            viewModel.deleteGuardian(guardian)
                        }
                    )
                }
            }

            // Accessibility Settings Card
            item {
                Text(
                    text = "Accessibility & Alert Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        var largeText by remember { mutableStateOf(profile?.largeTextMode ?: true) }
                        var highContrastMissed by remember { mutableStateOf(true) }
                        var voiceAlarms by remember { mutableStateOf(true) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Elderly Large Text Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Increases font size across schedule and cards",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = largeText,
                                onCheckedChange = {
                                    largeText = it
                                    viewModel.updateProfile(
                                        profile?.name ?: "Eleanor Vance",
                                        profile?.phone ?: "+1 (555) 234-5678",
                                        profile?.emergencyInfo ?: "",
                                        largeText
                                    )
                                },
                                modifier = Modifier.testTag("toggle_large_text")
                            )
                        }

                        Divider(color = DividerColor)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Bold Red Missed Dose Highlighting",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Prominently highlights overdue doses in red",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = highContrastMissed,
                                onCheckedChange = { highContrastMissed = it },
                                modifier = Modifier.testTag("toggle_missed_red_highlight")
                            )
                        }

                        Divider(color = DividerColor)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Voice Reminder Announcements",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Reads medicine name out loud at scheduled time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = voiceAlarms,
                                onCheckedChange = { voiceAlarms = it },
                                modifier = Modifier.testTag("toggle_voice_alarms")
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileModal) {
        EditProfileDialog(
            currentName = profile?.name ?: "Eleanor Vance",
            currentPhone = profile?.phone ?: "+1 (555) 234-5678",
            currentEmergency = profile?.emergencyInfo ?: "Blood Type O+, Allergy: Penicillin",
            onDismiss = { showEditProfileModal = false },
            onConfirm = { name, phone, emergency ->
                viewModel.updateProfile(name, phone, emergency, profile?.largeTextMode ?: true)
                showEditProfileModal = false
            }
        )
    }

    // Add Guardian Dialog
    if (showAddGuardianModal) {
        AddGuardianDialog(
            onDismiss = { showAddGuardianModal = false },
            onConfirm = { name, relationship, phone ->
                viewModel.addGuardian(name, relationship, phone)
                showAddGuardianModal = false
            }
        )
    }
}

@Composable
private fun PremiumFeatureChip(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFECB3)
    ) {
        Text(
            text = "✓ $text",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE65100),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun GuardianCardItem(
    guardian: GuardianEntity,
    onCall: () -> Unit,
    onSendAlert: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("guardian_card_${guardian.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MintContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = guardian.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${guardian.relationship} • ${guardian.phone}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete Guardian", tint = MissedRed)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCall,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("call_guardian_btn_${guardian.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call Guardian", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSendAlert,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("test_sms_alert_btn_${guardian.id}"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Alert", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentPhone: String,
    currentEmergency: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, emergency: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf(currentPhone) }
    var emergency by remember { mutableStateOf(currentEmergency) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit User Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_profile_name"),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_profile_phone"),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = emergency,
                    onValueChange = { emergency = it },
                    label = { Text("Emergency & Allergy Info") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_profile_emergency"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, phone, emergency) },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Save Profile", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddGuardianDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, relationship: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Caregiver") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Guardian Contact", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Guardian Name (e.g. Emily)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_guardian_name"),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship (e.g. Daughter / Nurse)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_guardian_relationship"),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_guardian_phone"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onConfirm(name, relationship, phone)
                    }
                },
                modifier = Modifier.testTag("confirm_add_guardian_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Add Contact", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
