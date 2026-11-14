package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BillReminder
import com.example.notifications.ReminderScheduler
import com.example.ui.components.formatDateTime
import com.example.ui.components.formatInr
import com.example.ui.components.formatShortDate
import com.example.ui.theme.ElectricEmeraldDark
import com.example.ui.theme.SolarAmberDark
import com.example.ui.viewmodel.BijliDashboardData

@Composable
fun RemindersScreen(
    data: BijliDashboardData,
    onAddReminderClick: () -> Unit,
    onToggleReminder: (BillReminder, Boolean) -> Unit,
    onDeleteReminder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ReminderScheduler.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            ReminderScheduler.triggerInstantTestNotification(
                context = context,
                title = "BijliPay Alerts Enabled",
                message = "You will receive electricity bill reminders before your due dates."
            )
        } else {
            Toast.makeText(
                context,
                "Notification permission denied. Reminders will still be saved in-app.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Notification Permission / Status Banner
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (hasPermission) {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (hasPermission) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                            contentDescription = "Notification Status",
                            tint = if (hasPermission) ElectricEmeraldDark else SolarAmberDark
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasPermission) "Bill Alerts Active" else "Enable Bill Notifications",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (hasPermission) {
                                    "${data.activeRemindersCount} active reminder(s) scheduled on your device."
                                } else {
                                    "Allow notifications so BijliPay can alert you before electricity due dates."
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("enable_notifications_button")
                            ) {
                                Text("Allow Alerts")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                if (hasPermission || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                    ReminderScheduler.triggerInstantTestNotification(
                                        context = context,
                                        title = "Electricity Bill Reminder",
                                        message = "Reminder test: Pay your electricity bill before due date to avoid late fees."
                                    )
                                    Toast.makeText(context, "Test reminder notification sent", Toast.LENGTH_SHORT).show()
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("test_notification_button")
                        ) {
                            Text("Test Alert")
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scheduled Reminders (${data.reminders.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                FilledTonalButton(
                    onClick = onAddReminderClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("new_reminder_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Set Reminder")
                }
            }
        }

        if (data.reminders.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text("No Bill Reminders Set", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Schedule automatic reminders 1 to 5 days before your bill due date so you never incur a late payment surcharge.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onAddReminderClick,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Create First Reminder")
                        }
                    }
                }
            }
        } else {
            items(data.reminders, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onToggle = { enabled -> onToggleReminder(reminder, enabled) },
                    onDelete = { onDeleteReminder(reminder.id) }
                )
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: BillReminder,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
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
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (reminder.amount > 0 || reminder.consumerNumber.isNotBlank()) {
                    Text(
                        text = buildString {
                            if (reminder.amount > 0) append(formatInr(reminder.amount))
                            if (reminder.consumerNumber.isNotBlank()) {
                                if (isNotEmpty()) append(" • ")
                                append("CA ${reminder.consumerNumber}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "Alert: ${formatDateTime(reminder.remindAtMillis)} (${reminder.daysBeforeDue}d before due)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Due Date: ${formatShortDate(reminder.dueDateMillis)}" +
                        if (!reminder.note.isNullOrBlank()) " • ${reminder.note}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = onToggle
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete reminder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
