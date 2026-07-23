package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.db.DoseLogEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MedicineViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MedicineViewModel,
    onNavigateToReminders: () -> Unit,
    onNavigateToScan: () -> Unit
) {
    val logs by viewModel.todayLogs.collectAsState()
    val profile by viewModel.profile.collectAsState()
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val totalDoses = logs.size
    val takenDoses = logs.count { it.status == "TAKEN" }
    val missedDoses = logs.count { it.status == "MISSED" }
    val pendingDoses = logs.count { it.status == "PENDING" || it.status == "SNOOZED" }

    val todayFormatted = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    val filteredLogs = remember(logs, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") logs
        else logs.filter { log ->
            val hour = log.scheduledTime.take(2).toIntOrNull() ?: 12
            val isPm = log.scheduledTime.contains("PM", ignoreCase = true)
            val militaryHour = if (isPm && hour < 12) hour + 12 else if (!isPm && hour == 12) 0 else hour
            when (selectedCategoryFilter) {
                "Morning" -> militaryHour in 5..11
                "Afternoon" -> militaryHour in 12..16
                "Evening" -> militaryHour in 17..21
                "Night" -> militaryHour >= 22 || militaryHour < 5
                else -> true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hello, ${profile?.name ?: "Eleanor"}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = todayFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToScan,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_top_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Prescription",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
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
            // Daily Progress Compliance Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_progress_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MintContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Today's Progress",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = OnMintContainer
                                )
                                Text(
                                    text = if (totalDoses == 0) "No doses scheduled for today"
                                    else "$takenDoses of $totalDoses doses completed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnMintContainer.copy(alpha = 0.8f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(TealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (totalDoses > 0) "${(takenDoses * 100 / totalDoses)}%" else "100%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val progressRatio = if (totalDoses > 0) takenDoses.toFloat() / totalDoses else 1.0f
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = TealPrimary,
                            trackColor = Color.White.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Pills Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatChip(
                                label = "Taken",
                                count = takenDoses,
                                badgeColor = TakenGreen,
                                textColor = OnMintContainer
                            )
                            StatChip(
                                label = "Upcoming",
                                count = pendingDoses,
                                badgeColor = BlueSecondary,
                                textColor = OnMintContainer
                            )
                            StatChip(
                                label = "Missed",
                                count = missedDoses,
                                badgeColor = MissedRed,
                                textColor = OnMintContainer
                            )
                        }
                    }
                }
            }

            // Filter Chips Row
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Today's Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onNavigateToReminders,
                        modifier = Modifier.testTag("manage_reminders_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = "Manage Reminders",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("All", "Morning", "Afternoon", "Evening", "Night")
                    items(filters) { category ->
                        FilterChip(
                            selected = selectedCategoryFilter == category,
                            onClick = { selectedCategoryFilter = category },
                            label = {
                                Text(
                                    text = category,
                                    fontSize = 15.sp,
                                    fontWeight = if (selectedCategoryFilter == category) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // Schedule Dose Cards
            if (filteredLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = TealPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No doses in this category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap Scan or Reminders tab to add medicines.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    DoseScheduleCard(
                        log = log,
                        onMarkStatus = { status ->
                            viewModel.markDoseStatus(log.id, status)
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
private fun StatChip(
    label: String,
    count: Int,
    badgeColor: Color,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(badgeColor)
        )
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
fun DoseScheduleCard(
    log: DoseLogEntity,
    onMarkStatus: (String) -> Unit
) {
    val isMissed = log.status == "MISSED"
    val isTaken = log.status == "TAKEN"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dose_card_${log.id}")
            .then(
                if (isMissed) {
                    Modifier.border(2.dp, MissedRed, RoundedCornerShape(16.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isMissed -> MissedRedSurface
                isTaken -> TakenGreenSurface
                else -> Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isMissed) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row: Status Badge & Scheduled Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when {
                            isMissed -> Icons.Default.Warning
                            isTaken -> Icons.Default.CheckCircle
                            else -> Icons.Default.Schedule
                        },
                        contentDescription = null,
                        tint = when {
                            isMissed -> MissedRed
                            isTaken -> TakenGreen
                            else -> BlueSecondary
                        },
                        modifier = Modifier.size(24.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isMissed -> MissedRed
                            isTaken -> TakenGreen
                            else -> BlueSecondary
                        }
                    ) {
                        Text(
                            text = when {
                                isMissed -> "MISSED DOSE"
                                isTaken -> "TAKEN (${log.takenTime ?: "Done"})"
                                else -> "UPCOMING"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = log.scheduledTime,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isMissed) MissedRed else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Medicine Name & Dosage
            Text(
                text = log.medicineName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isMissed) OnMissedRed else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Dosage: ${log.dosage}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons with Large Elderly-Friendly Touch Targets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isTaken) {
                    OutlinedButton(
                        onClick = { onMarkStatus("PENDING") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("undo_taken_btn_${log.id}"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Undo / Mark Un-taken", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (isMissed) {
                    Button(
                        onClick = { onMarkStatus("TAKEN") },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("take_late_btn_${log.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = TakenGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Late", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onMarkStatus("SNOOZED") },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("alert_guardian_btn_${log.id}"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MissedRed),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MissedRed)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Snooze", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { onMarkStatus("TAKEN") },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(54.dp)
                            .testTag("mark_taken_btn_${log.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark Taken", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onMarkStatus("SNOOZED") },
                        modifier = Modifier
                            .weight(0.9f)
                            .height(54.dp)
                            .testTag("snooze_btn_${log.id}"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Snooze", fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
