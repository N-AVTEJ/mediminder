package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.MedicineViewModel
import kotlinx.coroutines.launch

sealed class BottomTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    object Home : BottomTab("home", "Home", Icons.Default.Home, Icons.Outlined.Home, "tab_home")
    object Scan : BottomTab("scan", "Scan", Icons.Default.DocumentScanner, Icons.Outlined.DocumentScanner, "tab_scan")
    object Reminders : BottomTab("reminders", "Reminders", Icons.Default.NotificationsActive, Icons.Outlined.Notifications, "tab_reminders")
    object Profile : BottomTab("profile", "Profile", Icons.Default.Person, Icons.Outlined.Person, "tab_profile")
}

@Composable
fun MainAppContainer(
    viewModel: MedicineViewModel
) {
    var selectedTabItem by remember { mutableStateOf<BottomTab>(BottomTab.Home) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val userMessage by viewModel.userMessage.collectAsState()

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearUserMessage()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar")
            ) {
                val tabs = listOf(BottomTab.Home, BottomTab.Scan, BottomTab.Reminders, BottomTab.Profile)
                tabs.forEach { tab ->
                    val isSelected = selectedTabItem == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabItem = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = TealPrimary,
                            indicatorColor = TealPrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabItem) {
                BottomTab.Home -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToReminders = { selectedTabItem = BottomTab.Reminders },
                    onNavigateToScan = { selectedTabItem = BottomTab.Scan }
                )
                BottomTab.Scan -> ScanScreen(
                    viewModel = viewModel,
                    onSavedSuccessfully = { selectedTabItem = BottomTab.Reminders }
                )
                BottomTab.Reminders -> RemindersScreen(
                    viewModel = viewModel
                )
                BottomTab.Profile -> ProfileScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
