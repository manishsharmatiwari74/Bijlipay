package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BillStatus
import com.example.data.model.ElectricityBill
import com.example.data.model.ElectricityConnection
import com.example.ui.components.daysUntilDue
import com.example.ui.components.formatInr
import com.example.ui.components.formatShortDate
import com.example.ui.theme.DeepSlate
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.ElectricEmeraldDark
import com.example.ui.theme.MidnightTeal
import com.example.ui.theme.OverdueCrimson
import com.example.ui.theme.SolarAmber
import com.example.ui.viewmodel.BijliDashboardData

@Composable
fun DashboardScreen(
    data: BijliDashboardData,
    onAddConnectionClick: () -> Unit,
    onAddBillClick: () -> Unit,
    onAddReminderClick: () -> Unit,
    onLogReadingClick: () -> Unit,
    onPayBillClick: (ElectricityBill) -> Unit,
    onViewReceiptClick: (ElectricityBill) -> Unit,
    onDeleteConnectionClick: (String) -> Unit,
    onNavigateToUsage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nextDueBill = data.pendingBills.firstOrNull()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Hero Outstanding Card
        item {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(MidnightTeal, DeepSlate, Color(0xFF1A4B6E))
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Electricity Due",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                                Text(
                                    text = formatInr(data.totalPendingAmount),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = SolarAmber.copy(alpha = 0.22f),
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = "Electricity Balance",
                                        tint = SolarAmber,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        if (nextDueBill != null) {
                            val daysLeft = daysUntilDue(nextDueBill.dueDateMillis)
                            val dueLabel = when {
                                daysLeft < 0 -> "Overdue by ${-daysLeft} day(s)"
                                daysLeft == 0L -> "Due today!"
                                else -> "Due in $daysLeft day(s) • ${formatShortDate(nextDueBill.dueDateMillis)}"
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${nextDueBill.nickname} • ${nextDueBill.providerName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                        Text(
                                            text = dueLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (daysLeft < 0) Color(0xFFFCA5A5) else SolarAmber
                                        )
                                    }
                                    Button(
                                        onClick = { onPayBillClick(nextDueBill) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SolarAmber,
                                            contentColor = MidnightTeal
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("hero_pay_now_button")
                                    ) {
                                        Text("Pay ${formatInr(nextDueBill.totalPayable)}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ElectricMeter,
                                    contentDescription = null,
                                    tint = ElectricEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (data.connections.isEmpty()) {
                                        "Add your meter connection to manage bills"
                                    } else {
                                        "All electricity bills are settled!"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    icon = Icons.Filled.ElectricMeter,
                    label = "Add Meter",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onAddConnectionClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_connection")
                )
                QuickActionTile(
                    icon = Icons.Filled.Receipt,
                    label = "Add Bill",
                    tint = SolarAmber,
                    onClick = onAddBillClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_bill")
                )
                QuickActionTile(
                    icon = Icons.Filled.NotificationsActive,
                    label = "Reminder",
                    tint = ElectricEmeraldDark,
                    onClick = onAddReminderClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_reminder")
                )
                QuickActionTile(
                    icon = Icons.Filled.ShowChart,
                    label = "Log kWh",
                    tint = Color(0xFF0284C7),
                    onClick = onLogReadingClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_log_reading")
                )
            }
        }

        // Saved Meter Connections
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Meters (${data.connections.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                FilledTonalButton(
                    onClick = onAddConnectionClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Meter")
                }
            }
        }

        if (data.connections.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_bills),
                            contentDescription = "No electricity connections added yet",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Text(
                            text = "No Electricity Meter Linked Yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Add your DISCOM consumer number (Tata Power, BSES, MSEDCL, BESCOM, etc.) to pay bills, schedule due reminders, and track kWh usage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onAddConnectionClick,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("empty_add_meter_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Link Electricity Meter")
                        }
                    }
                }
            }
        } else {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(data.connections, key = { it.id }) { conn ->
                        ConnectionCard(
                            connection = conn,
                            onTrackUsage = onNavigateToUsage,
                            onDelete = { onDeleteConnectionClick(conn.id) }
                        )
                    }
                }
            }
        }

        // Pending & Overdue Bills Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Due & Upcoming Bills (${data.pendingBills.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                if (data.connections.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onAddBillClick,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Bill")
                    }
                }
            }
        }

        if (data.pendingBills.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = ElectricEmerald.copy(alpha = 0.16f),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = ElectricEmeraldDark
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No Pending Bills", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (data.connections.isEmpty()) {
                                    "Link a meter above, then add your upcoming electricity bill."
                                } else {
                                    "You're caught up! Tap Add Bill to record a new bill cycle."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(data.pendingBills, key = { it.id }) { bill ->
                BillListCard(
                    bill = bill,
                    onPayClick = { onPayBillClick(bill) },
                    onReceiptClick = { onViewReceiptClick(bill) }
                )
            }
        }

        if (data.paidBills.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Payments",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(data.paidBills.take(3), key = { it.id }) { bill ->
                BillListCard(
                    bill = bill,
                    onPayClick = {},
                    onReceiptClick = { onViewReceiptClick(bill) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier
            .height(82.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = tint.copy(alpha = 0.14f),
                shape = CircleShape,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ConnectionCard(
    connection: ElectricityConnection,
    onTrackUsage: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .width(230.dp)
            .clickable(onClick = onTrackUsage)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = connection.nickname,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Remove ${connection.nickname}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = connection.providerName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "CA: ${connection.consumerNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "₹${connection.tariffRate}/kWh • Target ${connection.monthlyTargetKwh} kWh",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BillListCard(
    bill: ElectricityBill,
    onPayClick: () -> Unit,
    onReceiptClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPaid = bill.status == BillStatus.PAID.name
    val isOverdue = bill.status == BillStatus.OVERDUE.name || bill.isCurrentlyOverdue()

    val (badgeBg, badgeText, statusLabel) = when {
        isPaid -> Triple(ElectricEmerald.copy(alpha = 0.16f), ElectricEmeraldDark, "PAID")
        isOverdue -> Triple(OverdueCrimson.copy(alpha = 0.14f), OverdueCrimson, "OVERDUE")
        else -> Triple(SolarAmber.copy(alpha = 0.18f), Color(0xFFB45309), "PENDING")
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable { if (isPaid) onReceiptClick() else onPayClick() }
            .testTag("bill_card_${bill.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = bill.nickname,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeText,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "${bill.providerName} • ${bill.billingMonth}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "CA ${bill.consumerNumber} • ${"%.0f".format(bill.unitsConsumed)} kWh",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isPaid) {
                        "Paid ${formatShortDate(bill.paidAtMillis ?: 0L)} • ${bill.paymentMethod ?: "Online"}"
                    } else {
                        "Due ${formatShortDate(bill.dueDateMillis)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverdue && !isPaid) OverdueCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatInr(bill.totalPayable),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isPaid) {
                    OutlinedButton(
                        onClick = onReceiptClick,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Receipt")
                    }
                } else {
                    Button(
                        onClick = onPayClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MidnightTeal,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Pay Now")
                    }
                }
            }
        }
    }
}
