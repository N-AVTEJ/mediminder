package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ReminderEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MedicineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: MedicineViewModel
) {
    val reminders by viewModel.reminders.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingReminderForTime by remember { mutableStateOf<ReminderEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Medicine Reminders",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("top_add_reminder_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add Reminder",
                            tint = TealPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TealPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("fab_add_reminder")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(28.dp))
                    Text("Add Medicine", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MintContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "Active Reminders: ${reminders.count { it.isActive }}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = OnMintContainer
                            )
                            Text(
                                text = "Toggle switches on or off, or tap 'Edit Time' to adjust schedules.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnMintContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (reminders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Medication,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = TealPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Reminders Added Yet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap '+ Add Medicine' below or scan a prescription to get started.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(reminders, key = { it.id }) { reminder ->
                    ReminderCardItem(
                        reminder = reminder,
                        onToggleActive = { isChecked ->
                            viewModel.toggleReminder(reminder.id, isChecked)
                        },
                        onEditTime = {
                            editingReminderForTime = reminder
                        },
                        onDelete = {
                            viewModel.deleteReminder(reminder)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add New Reminder Dialog
    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, dosage, time, category, instructions, shape ->
                viewModel.addReminder(name, dosage, time, category, instructions, shape)
                showAddDialog = false
            }
        )
    }

    // Edit Time Dialog
    editingReminderForTime?.let { reminder ->
        EditTimeDialog(
            reminder = reminder,
            onDismiss = { editingReminderForTime = null },
            onConfirm = { newTime, newCategory ->
                viewModel.updateReminderTime(reminder.id, newTime, newCategory)
                editingReminderForTime = null
            }
        )
    }
}

@Composable
fun ReminderCardItem(
    reminder: ReminderEntity,
    onToggleActive: (Boolean) -> Unit,
    onEditTime: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_card_${reminder.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isActive) MaterialTheme.colorScheme.surface
            else Color(0xFFF1F3F4)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (reminder.isActive) 3.dp else 0.dp)
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
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (reminder.isActive) TealPrimary
                                else Color.Gray
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (reminder.shape) {
                                "Capsule" -> Icons.Default.MedicalServices
                                "Liquid" -> Icons.Default.WaterDrop
                                "Injection" -> Icons.Default.Vaccines
                                else -> Icons.Default.Medication
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = reminder.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (reminder.isActive) MaterialTheme.colorScheme.onSurface
                            else TextSecondary
                        )
                        Text(
                            text = reminder.dosage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }

                // Elderly-Friendly Large Toggle Switch
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = onToggleActive,
                    modifier = Modifier
                        .scaleToLarge()
                        .testTag("toggle_reminder_${reminder.id}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TealPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time & Instructions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BlueContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = BlueSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${reminder.time} (${reminder.timeCategory})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = BlueSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onEditTime,
                        modifier = Modifier.testTag("edit_time_btn_${reminder.id}")
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Time", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_reminder_btn_${reminder.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Reminder",
                            tint = MissedRed
                        )
                    }
                }
            }

            if (reminder.instructions.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 ${reminder.instructions}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

private fun Modifier.scaleToLarge(): Modifier = this.padding(horizontal = 4.dp)

@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, dosage: String, time: String, category: String, instructions: String, shape: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("08:00 AM") }
    var selectedCategory by remember { mutableStateOf("Morning") }
    var instructions by remember { mutableStateOf("Take with full glass of water") }
    var shape by remember { mutableStateOf("Pill") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Medicine Reminder",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine Name (e.g. Aspirin)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_med_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage (e.g. 100mg, 1 tablet)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_med_dosage_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category & Time picker
                Text("Schedule Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val cats = listOf("Morning", "Afternoon", "Evening", "Night")
                    cats.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                selectedTime = when (cat) {
                                    "Morning" -> "08:00 AM"
                                    "Afternoon" -> "01:00 PM"
                                    "Evening" -> "06:30 PM"
                                    else -> "09:00 PM"
                                }
                            },
                            label = { Text(cat, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = { selectedTime = it },
                    label = { Text("Reminder Time (e.g. 08:00 AM)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_med_time_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions (e.g. Take after food)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_med_instructions_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name,
                            if (dosage.isBlank()) "1 Tablet" else dosage,
                            selectedTime,
                            selectedCategory,
                            instructions,
                            shape
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_add_reminder_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Save Reminder", fontWeight = FontWeight.Bold)
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
fun EditTimeDialog(
    reminder: ReminderEntity,
    onDismiss: () -> Unit,
    onConfirm: (newTime: String, newCategory: String) -> Unit
) {
    var newTime by remember { mutableStateOf(reminder.time) }
    var newCategory by remember { mutableStateOf(reminder.timeCategory) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Time for ${reminder.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Current: ${reminder.time} (${reminder.timeCategory})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = newTime,
                    onValueChange = { newTime = it },
                    label = { Text("Set New Time (e.g. 08:30 AM)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_time_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val cats = listOf("Morning", "Afternoon", "Evening", "Night")
                    cats.forEach { cat ->
                        FilterChip(
                            selected = newCategory == cat,
                            onClick = { newCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newTime, newCategory) },
                modifier = Modifier.testTag("confirm_edit_time_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Save New Time", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
