package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ElectricityConnection
import com.example.data.model.UsageReading
import com.example.ui.components.UsageBarChartCard
import com.example.ui.components.formatInr
import com.example.ui.components.formatShortDate
import com.example.ui.theme.ElectricEmeraldDark
import com.example.ui.theme.OverdueCrimson
import com.example.ui.theme.SolarAmberDark
import com.example.ui.viewmodel.BijliDashboardData

@Composable
fun UsageScreen(
    data: BijliDashboardData,
    selectedConnectionId: String?,
    onSelectConnection: (String?) -> Unit,
    onLogReadingClick: () -> Unit,
    onUpdateTargetClick: (ElectricityConnection, Long) -> Unit,
    onDeleteReadingClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeConnection = remember(data.connections, selectedConnectionId) {
        data.connections.find { it.id == selectedConnectionId } ?: data.connections.firstOrNull()
    }

    val filteredReadings = remember(data.usageReadings, activeConnection) {
        if (activeConnection == null) {
            data.usageReadings
        } else {
            data.usageReadings.filter { it.connectionId == activeConnection.id }
        }
    }

    val monthlyTarget = activeConnection?.monthlyTargetKwh ?: 300L
    val latestReading = filteredReadings.lastOrNull()
    val totalUnits = filteredReadings.sumOf { it.unitsKwh }
    val avgUnits = if (filteredReadings.isNotEmpty()) totalUnits / filteredReadings.size else 0.0
    val latestUnits = latestReading?.unitsKwh ?: 0.0
    val targetProgress = (latestUnits / monthlyTarget.toDouble()).toFloat().coerceIn(0f, 1f)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (data.connections.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    data.connections.forEach { conn ->
                        FilterChip(
                            selected = activeConnection?.id == conn.id,
                            onClick = { onSelectConnection(conn.id) },
                            label = { Text("${conn.nickname} (${conn.providerName})") }
                        )
                    }
                }
            }
        }

        // KPI Summary Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    label = "Latest Month",
                    value = "${"%.0f".format(latestUnits)} kWh",
                    sub = latestReading?.monthLabel ?: "No log yet",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    label = "Monthly Avg",
                    value = "${"%.0f".format(avgUnits)} kWh",
                    sub = "${"%.1f".format(avgUnits / 30.0)} kWh/day",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    label = "Est. Bill",
                    value = formatInr(latestReading?.estimatedCost ?: 0.0),
                    sub = "₹${activeConnection?.tariffRate ?: 7.5}/kWh",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Monthly Budget Progress Card
        if (activeConnection != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Monthly Usage Budget", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${"%.0f".format(latestUnits)} / $monthlyTarget kWh",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        val nextTarget = (monthlyTarget - 50L).coerceAtLeast(50L)
                                        onUpdateTargetClick(activeConnection, nextTarget)
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("-50")
                                }
                                FilledTonalButton(
                                    onClick = {
                                        onUpdateTargetClick(activeConnection, monthlyTarget + 50L)
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("+50")
                                }
                            }
                        }

                        LinearProgressIndicator(
                            progress = { targetProgress },
                            color = when {
                                latestUnits > monthlyTarget -> OverdueCrimson
                                latestUnits > monthlyTarget * 0.85 -> SolarAmberDark
                                else -> ElectricEmeraldDark
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }

        // Custom Canvas Bar Chart
        item {
            if (filteredReadings.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                        Text("Start Tracking kWh Usage", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Log your meter's opening and closing kWh readings to unlock monthly usage charts and cost projections.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onLogReadingClick,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("empty_log_reading_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log First Reading")
                        }
                    }
                }
            } else {
                UsageBarChartCard(
                    readings = filteredReadings,
                    monthlyTargetKwh = monthlyTarget
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Meter Reading Log (${filteredReadings.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                FilledTonalButton(
                    onClick = onLogReadingClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("usage_log_reading_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Reading")
                }
            }
        }

        items(filteredReadings.sortedByDescending { it.yearMonth }, key = { it.id }) { reading ->
            UsageReadingCard(
                reading = reading,
                onDelete = { onDeleteReadingClick(reading.id) }
            )
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UsageReadingCard(
    reading: UsageReading,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${reading.monthLabel} (${reading.yearMonth})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Meter: ${"%.0f".format(reading.previousReading)} -> ${"%.0f".format(reading.currentReading)} kWh",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Recorded ${formatShortDate(reading.readingDateMillis)}" +
                        if (!reading.notes.isNullOrBlank()) " • ${reading.notes}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${"%.0f".format(reading.unitsKwh)} kWh",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatInr(reading.estimatedCost),
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricEmeraldDark
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete reading",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
