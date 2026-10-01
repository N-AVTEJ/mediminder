package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.firebase.GuardianInviteService
import com.example.ui.theme.*
import com.example.ui.viewmodel.MedicineViewModel

enum class AcceptViewMode {
    APP_NATIVE,
    WEB_VIEW
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianAcceptDialog(
    state: MedicineViewModel.GuardianAcceptState,
    onDismiss: () -> Unit,
    onConfirm: (token: String, guardianUid: String, name: String, phone: String) -> Unit
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(AcceptViewMode.APP_NATIVE) }

    var guardianUidInput by remember(state.guardianUid) { mutableStateOf(state.guardianUid) }
    var guardianPhoneInput by remember(state.guardianPhone) { mutableStateOf(state.guardianPhone) }
    var guardianNameInput by remember(state.guardian?.name) {
        mutableStateOf(state.guardian?.name ?: "David Vance")
    }

    val isLinked = state.isLinked

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("guardian_accept_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MintContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Guardian Link Portal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Phase 23 Acceptance Flow",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_accept_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Selector: App Native vs Web View
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedMode == AcceptViewMode.APP_NATIVE,
                        onClick = { selectedMode = AcceptViewMode.APP_NATIVE },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("In-App View", fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_app_view"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = selectedMode == AcceptViewMode.WEB_VIEW,
                        onClick = { selectedMode = AcceptViewMode.WEB_VIEW },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Web View Portal", fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_web_view"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content Section based on selectedMode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedMode) {
                        AcceptViewMode.APP_NATIVE -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Patient Invitation Banner
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MintContainer),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Caregiver Invitation",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = OnMintContainer
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isLinked) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                            ) {
                                                Text(
                                                    text = if (isLinked) "LINKED" else "PENDING",
                                                    modifier = Modifier
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        .testTag("guardian_status_badge"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isLinked) Color(0xFF2E7D32) else Color(0xFFE65100)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "${state.patientName} has invited you to monitor their daily medication reminders and receive missed-dose emergency alerts.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = OnMintContainer.copy(alpha = 0.9f)
                                        )
                                    }
                                }

                                // Invite Token Card
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = TealPrimary)
                                        Column {
                                            Text(
                                                text = "Invite Token",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = state.token,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = TealPrimary
                                            )
                                        }
                                    }
                                }

                                // Guardian Information Inputs
                                OutlinedTextField(
                                    value = guardianNameInput,
                                    onValueChange = { guardianNameInput = it },
                                    label = { Text("Guardian Full Name") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("guardian_name_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = guardianPhoneInput,
                                    onValueChange = { guardianPhoneInput = it },
                                    label = { Text("Guardian Mobile Phone") },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("guardian_phone_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = guardianUidInput,
                                    onValueChange = { guardianUidInput = it },
                                    label = { Text("Guardian UID (Firestore Identifier)") },
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("guardian_uid_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                // Guardian Caregiver Privileges
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Caregiver Permissions:",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TealPrimary
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TakenGreen, modifier = Modifier.size(18.dp))
                                            Text("Real-time SMS alerts if doses are missed", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TakenGreen, modifier = Modifier.size(18.dp))
                                            Text("Read-only access to adherence logs & active meds", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TakenGreen, modifier = Modifier.size(18.dp))
                                            Text("Direct emergency call priority", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }

                                if (isLinked) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFE8F5E9),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                                            Column {
                                                Text(
                                                    text = "Account Linked & Active",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                                Text(
                                                    text = "Firestore guardians doc updated: status='linked', guardianUid='$guardianUidInput'",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF1B5E20)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        AcceptViewMode.WEB_VIEW -> {
                            val webHtml = remember(state.token, state.patientName, guardianPhoneInput, guardianUidInput, isLinked) {
                                GuardianInviteService.generateCaregiverWebHtml(
                                    token = state.token,
                                    patientName = state.patientName,
                                    guardianPhone = guardianPhoneInput,
                                    guardianUid = guardianUidInput,
                                    isLinked = isLinked
                                )
                            }

                            Column(modifier = Modifier.fillMaxSize()) {
                                // Web URL Bar
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = "https://medremind.app/invite?token=${state.token}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                settings.javaScriptEnabled = true
                                                settings.domStorageEnabled = true
                                                webViewClient = object : WebViewClient() {
                                                    override fun shouldOverrideUrlLoading(
                                                        view: WebView?,
                                                        request: WebResourceRequest?
                                                    ): Boolean {
                                                        val url = request?.url?.toString() ?: ""
                                                        if (url.startsWith("medremind://confirm") || url.startsWith("medremind://invite")) {
                                                            onConfirm(state.token, guardianUidInput, guardianNameInput, guardianPhoneInput)
                                                            return true
                                                        }
                                                        return false
                                                    }
                                                }
                                                addJavascriptInterface(
                                                    GuardianInviteService.CaregiverWebBridge { token, uid ->
                                                        onConfirm(token, uid, guardianNameInput, guardianPhoneInput)
                                                    },
                                                    "AndroidBridge"
                                                )
                                                loadDataWithBaseURL("https://medremind.app", webHtml, "text/html", "UTF-8", null)
                                            }
                                        },
                                        update = { webView ->
                                            webView.loadDataWithBaseURL("https://medremind.app", webHtml, "text/html", "UTF-8", null)
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("caregiver_web_view")
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("dismiss_accept_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isLinked) "Done" else "Cancel", fontWeight = FontWeight.Bold)
                    }

                    if (!isLinked) {
                        Button(
                            onClick = {
                                onConfirm(state.token, guardianUidInput, guardianNameInput, guardianPhoneInput)
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(50.dp)
                                .testTag("confirm_guardian_accept_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm & Link Guardian", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(2f)
                                .height(50.dp)
                                .testTag("completed_guardian_accept_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Linked in Firestore", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
