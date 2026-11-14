package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.UsageReading
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.OverdueCrimson
import com.example.ui.theme.SolarAmber

@Composable
fun UsageBarChartCard(
    readings: List<UsageReading>,
    monthlyTargetKwh: Long,
    modifier: Modifier = Modifier
) {
    val displayed = remember(readings) { readings.takeLast(6) }
    var selectedIndex by remember(displayed) {
        mutableIntStateOf((displayed.lastIndex).coerceAtLeast(0))
    }

    val selected = displayed.getOrNull(selectedIndex)
    val maxUnits = remember(displayed, monthlyTargetKwh) {
        val peak = displayed.maxOfOrNull { it.unitsKwh } ?: 0.0
        maxOf(peak, monthlyTargetKwh.toDouble(), 100.0) * 1.15
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("usage_chart_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly kWh Trend",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Target: $monthlyTargetKwh kWh/mo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selected != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "${"%.0f".format(selected.unitsKwh)} kWh",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${selected.monthLabel} • Est. ₹${"%.0f".format(selected.estimatedCost)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val animatedProgress by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 550),
                label = "chartBarAnim"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val chartHeight = size.height
                    val chartWidth = size.width

                    // Dashed target threshold line
                    val targetY = chartHeight - ((monthlyTargetKwh.toDouble() / maxUnits).toFloat() * chartHeight)
                        .coerceIn(4f, chartHeight - 4f)
                    drawLine(
                        color = SolarAmber,
                        start = Offset(0f, targetY),
                        end = Offset(chartWidth, targetY),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )

                    // Baseline
                    drawLine(
                        color = surfaceVariantColor,
                        start = Offset(0f, chartHeight - 1f),
                        end = Offset(chartWidth, chartHeight - 1f),
                        strokeWidth = 2f
                    )

                    if (displayed.isNotEmpty()) {
                        val slotWidth = chartWidth / displayed.size
                        val barWidth = (slotWidth * 0.48f).coerceIn(22f, 56f)
                        displayed.forEachIndexed { idx, reading ->
                            val ratio = (reading.unitsKwh / maxUnits).toFloat().coerceIn(0.04f, 1f)
                            val barHeight = ratio * chartHeight * animatedProgress
                            val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                            val top = chartHeight - barHeight
                            val isSelected = idx == selectedIndex
                            val isOverTarget = reading.unitsKwh > monthlyTargetKwh

                            val barBrush = when {
                                isOverTarget -> Brush.verticalGradient(
                                    listOf(OverdueCrimson, OverdueCrimson.copy(alpha = 0.72f))
                                )
                                isSelected -> Brush.verticalGradient(
                                    listOf(SolarAmber, ElectricEmerald)
                                )
                                else -> Brush.verticalGradient(
                                    listOf(primaryColor.copy(alpha = 0.85f), primaryColor.copy(alpha = 0.45f))
                                )
                            }

                            drawRoundRect(
                                brush = barBrush,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(14f, 14f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                displayed.forEachIndexed { idx, reading ->
                    val isSelected = idx == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedIndex = idx }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = reading.monthLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
