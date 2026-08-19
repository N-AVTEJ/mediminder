package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.firebase.AffiliateClickFirebaseModel
import com.example.ui.theme.*
import com.example.ui.viewmodel.MedicineViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AffiliateAnalyticsCard(
    viewModel: MedicineViewModel,
    modifier: Modifier = Modifier
) {
    val affiliateClicks by viewModel.affiliateClicks.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = By Pharmacy, 1 = By Medicine, 2 = Recent Logs

    // Aggregations
    val totalClicks = affiliateClicks.size
    val estimatedEarnings = totalClicks * 0.75 // $0.75 per referral click

    val clicksByPharmacy = remember(affiliateClicks) {
        affiliateClicks.groupBy { it.pharmacy }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    val clicksByMedicine = remember(affiliateClicks) {
        affiliateClicks.groupBy { it.medicine }
            .mapValues { entry ->
                val count = entry.value.size
                val pharmacyBreakdown = entry.value.groupBy { it.pharmacy }.mapValues { it.value.size }
                Triple(entry.key, count, pharmacyBreakdown)
            }
            .values
            .sortedByDescending { it.second }
    }

    val topPharmacy = clicksByPharmacy.firstOrNull()?.first ?: "N/A"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("affiliate_analytics_card")
            .border(1.5.dp, TealPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(TealPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Analytics",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Affiliate Click Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Earnings Tracking & Referrals",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = "ADMIN VIEW",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            // Summary Metric Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Clicks Metric
                MetricSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Clicks",
                    value = "$totalClicks",
                    icon = Icons.Default.TouchApp,
                    color = TealPrimary
                )

                // Est Earnings Metric
                MetricSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Est. Revenue",
                    value = "$${String.format(Locale.US, "%.2f", estimatedEarnings)}",
                    icon = Icons.Default.AttachMoney,
                    color = Color(0xFF2E7D32)
                )

                // Top Pharmacy Metric
                MetricSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Top Partner",
                    value = topPharmacy,
                    icon = Icons.Default.Store,
                    color = Color(0xFF0288D1)
                )
            }

            // Filter Tabs Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SoftBackground,
                contentColor = TealPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .height(42.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("admin_analytics_tab_pharmacy"),
                    text = { Text("By Pharmacy", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("admin_analytics_tab_medicine"),
                    text = { Text("By Medicine", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.testTag("admin_analytics_tab_recent"),
                    text = { Text("Click Logs", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> PharmacyBreakdownView(
                    clicksByPharmacy = clicksByPharmacy,
                    totalClicks = totalClicks
                )
                1 -> MedicineBreakdownView(
                    clicksByMedicine = clicksByMedicine,
                    totalClicks = totalClicks
                )
                2 -> RecentClickLogsView(
                    affiliateClicks = affiliateClicks
                )
            }

            HorizontalDivider(color = DividerColor)

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.seedSampleAffiliateClicks() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("seed_sample_clicks_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddChart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Test Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.clearAffiliateClicks() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clear_clicks_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Logs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetricSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun PharmacyBreakdownView(
    clicksByPharmacy: List<Pair<String, Int>>,
    totalClicks: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (clicksByPharmacy.isEmpty()) {
            Text(
                text = "No pharmacy clicks recorded yet. Click 'Buy Now' on medicine scan results to trigger affiliate referral events.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            clicksByPharmacy.forEach { (pharmacy, count) ->
                val fraction = if (totalClicks > 0) count.toFloat() / totalClicks else 0f
                val percentage = (fraction * 100).toInt()
                val providerColor = when (pharmacy.lowercase(Locale.ROOT)) {
                    "1mg" -> Color(0xFFFF6F00)
                    "pharmeasy" -> TealPrimary
                    "netmeds" -> Color(0xFF0288D1)
                    else -> TealPrimary
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pharmacy_analytics_item_$pharmacy"),
                    shape = RoundedCornerShape(12.dp),
                    color = SoftBackground
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = providerColor
                                ) {
                                    Text(
                                        text = pharmacy,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "$count clicks ($percentage%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = "$${String.format(Locale.US, "%.2f", count * 0.75)} est.",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }

                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = providerColor,
                            trackColor = providerColor.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicineBreakdownView(
    clicksByMedicine: List<Triple<String, Int, Map<String, Int>>>,
    totalClicks: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (clicksByMedicine.isEmpty()) {
            Text(
                text = "No medicine referrals logged yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            val maxClicks = clicksByMedicine.firstOrNull()?.second ?: 1
            clicksByMedicine.forEach { (medicine, count, pharmacyMap) ->
                val fraction = if (maxClicks > 0) count.toFloat() / maxClicks else 0f

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medicine_analytics_item_$medicine"),
                    shape = RoundedCornerShape(12.dp),
                    color = SoftBackground
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = medicine,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$count clicks",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = TealPrimary,
                            trackColor = TealPrimary.copy(alpha = 0.15f)
                        )

                        val breakdownText = pharmacyMap.entries.joinToString(" • ") { "${it.key}: ${it.value}" }
                        Text(
                            text = "Partners: $breakdownText",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentClickLogsView(
    affiliateClicks: List<AffiliateClickFirebaseModel>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (affiliateClicks.isEmpty()) {
            Text(
                text = "No recent click events found.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            Text(
                text = "Showing latest ${affiliateClicks.take(8).size} of ${affiliateClicks.size} clicks:",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )

            affiliateClicks.take(8).forEach { click ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = SoftBackground
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = click.medicine,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (click.pharmacy.lowercase(Locale.ROOT)) {
                                        "1mg" -> Color(0xFFFF6F00)
                                        "pharmeasy" -> TealPrimary
                                        "netmeds" -> Color(0xFF0288D1)
                                        else -> TealPrimary
                                    }
                                ) {
                                    Text(
                                        text = click.pharmacy,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "User: ${click.userId.take(12)} • ${click.timestamp.take(19)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
