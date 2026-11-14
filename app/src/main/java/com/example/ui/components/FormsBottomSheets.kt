package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ElectricityBill
import com.example.data.model.ElectricityConnection
import com.example.data.model.MeterType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val popularProviders = listOf(
    "Tata Power",
    "BSES Rajdhani",
    "MSEDCL",
    "BESCOM",
    "Adani Electricity",
    "UPPCL"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConnectionSheet(
    onSave: (
        providerName: String,
        consumerNumber: String,
        nickname: String,
        meterType: String,
        tariffRate: Double,
        monthlyTargetKwh: Long,
        billingCycleDay: Long
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var providerName by remember { mutableStateOf("Tata Power") }
    var consumerNumber by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("Home Meter") }
    var meterType by remember { mutableStateOf(MeterType.SMART_METER) }
    var tariffRateText by remember { mutableStateOf("7.5") }
    var targetKwhText by remember { mutableStateOf("350") }
    var billingDayText by remember { mutableStateOf("5") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add Electricity Connection", style = MaterialTheme.typography.titleLarge)
            Text(
                "Link your DISCOM meter to pay bills, track usage & get reminders.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                popularProviders.forEach { preset ->
                    FilterChip(
                        selected = providerName.equals(preset, ignoreCase = true),
                        onClick = { providerName = preset },
                        label = { Text(preset) }
                    )
                }
            }

            OutlinedTextField(
                value = providerName,
                onValueChange = { providerName = it; errorMsg = null },
                label = { Text("Electricity Board / DISCOM") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = consumerNumber,
                onValueChange = { consumerNumber = it; errorMsg = null },
                label = { Text("Consumer / CA Number") },
                placeholder = { Text("e.g. CA900123456") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connection_consumer_input")
            )
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it; errorMsg = null },
                label = { Text("Meter Nickname") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MeterType.entries.forEach { type ->
                    FilterChip(
                        selected = meterType == type,
                        onClick = { meterType = type },
                        label = { Text(type.label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = tariffRateText,
                    onValueChange = { tariffRateText = it; errorMsg = null },
                    label = { Text("Rate (₹/kWh)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = targetKwhText,
                    onValueChange = { targetKwhText = it; errorMsg = null },
                    label = { Text("Target (kWh)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = billingDayText,
                    onValueChange = { billingDayText = it; errorMsg = null },
                    label = { Text("Bill Day") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
            }

            if (errorMsg != null) {
                Text(
                    text = errorMsg.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    val tariff = tariffRateText.toDoubleOrNull()
                    val target = targetKwhText.toLongOrNull()
                    val day = billingDayText.toLongOrNull()
                    when {
                        providerName.trim().length < 2 -> errorMsg = "Enter electricity board name"
                        consumerNumber.trim().length < 3 -> errorMsg = "Consumer number must be at least 3 characters"
                        nickname.trim().isEmpty() -> errorMsg = "Enter a meter nickname"
                        tariff == null || tariff <= 0 || tariff > 100 -> errorMsg = "Enter a valid ₹/kWh tariff (1-100)"
                        target == null || target < 10 -> errorMsg = "Monthly target must be at least 10 kWh"
                        day == null || day !in 1L..28L -> errorMsg = "Billing day must be between 1 and 28"
                        else -> onSave(
                            providerName.trim(),
                            consumerNumber.trim(),
                            nickname.trim(),
                            meterType.name,
                            tariff,
                            target,
                            day
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_connection_button")
            ) {
                Text("Save Meter Connection")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillSheet(
    connections: List<ElectricityConnection>,
    onSave: (
        connection: ElectricityConnection,
        billingMonth: String,
        unitsConsumed: Double,
        amount: Double,
        lateFee: Double,
        dueDateMillis: Long
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedConn by remember(connections) { mutableStateOf(connections.firstOrNull()) }
    val currentMonthDefault = remember {
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
    }
    var billingMonth by remember { mutableStateOf(currentMonthDefault) }
    var unitsText by remember { mutableStateOf("260") }
    var amountText by remember {
        mutableStateOf(
            selectedConn?.let { "%.0f".format(260.0 * it.tariffRate) } ?: "1950"
        )
    }
    var dueInDays by remember { mutableIntStateOf(6) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add Electricity Bill", style = MaterialTheme.typography.titleLarge)

            if (connections.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Please add a meter connection first before creating a bill.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    connections.forEach { conn ->
                        FilterChip(
                            selected = selectedConn?.id == conn.id,
                            onClick = {
                                selectedConn = conn
                                val units = unitsText.toDoubleOrNull() ?: 0.0
                                if (units > 0) {
                                    amountText = "%.0f".format(units * conn.tariffRate)
                                }
                            },
                            label = { Text("${conn.nickname} (${conn.providerName})") }
                        )
                    }
                }

                OutlinedTextField(
                    value = billingMonth,
                    onValueChange = { billingMonth = it; errorMsg = null },
                    label = { Text("Billing Month (e.g., Nov 2026)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = unitsText,
                        onValueChange = {
                            unitsText = it
                            val u = it.toDoubleOrNull()
                            val rate = selectedConn?.tariffRate ?: 7.5
                            if (u != null && u >= 0) {
                                amountText = "%.0f".format(u * rate)
                            }
                            errorMsg = null
                        },
                        label = { Text("Units (kWh)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it; errorMsg = null },
                        label = { Text("Bill Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bill_amount_input")
                    )
                }

                Text("Due Date", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(-2 to "2 days ago (Overdue)", 2 to "In 2 days", 6 to "In 6 days", 15 to "In 15 days").forEach { (days, label) ->
                        FilterChip(
                            selected = dueInDays == days,
                            onClick = { dueInDays = days },
                            label = { Text(label) }
                        )
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        val conn = selectedConn
                        val units = unitsText.toDoubleOrNull()
                        val amt = amountText.toDoubleOrNull()
                        when {
                            conn == null -> errorMsg = "Select a meter connection"
                            billingMonth.trim().length < 3 -> errorMsg = "Enter valid billing month"
                            units == null || units < 0 -> errorMsg = "Enter valid kWh units"
                            amt == null || amt <= 0 -> errorMsg = "Enter bill amount greater than 0"
                            else -> {
                                val dueMillis = System.currentTimeMillis() + dueInDays * 86_400_000L
                                val lateFee = if (dueInDays < 0) (amt * 0.02).coerceAtLeast(50.0) else 0.0
                                onSave(conn, billingMonth.trim(), units, amt, lateFee, dueMillis)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_bill_button")
                ) {
                    Text("Save Electricity Bill")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderSheet(
    pendingBills: List<ElectricityBill>,
    onSave: (
        billId: String,
        title: String,
        consumerNumber: String,
        amount: Double,
        dueDateMillis: Long,
        daysBeforeDue: Long,
        note: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedBill by remember { mutableStateOf(pendingBills.firstOrNull()) }
    var title by remember {
        mutableStateOf(
            selectedBill?.let { "Pay ${it.nickname} (${it.providerName})" } ?: "Monthly Electricity Bill"
        )
    }
    var consumerNumber by remember { mutableStateOf(selectedBill?.consumerNumber ?: "") }
    var amountText by remember { mutableStateOf(selectedBill?.let { "%.0f".format(it.totalPayable) } ?: "1500") }
    var daysBefore by remember { mutableIntStateOf(2) }
    var dueInDays by remember { mutableIntStateOf(5) }
    var note by remember { mutableStateOf("Avoid late payment penalty") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Schedule Bill Reminder", style = MaterialTheme.typography.titleLarge)

            if (pendingBills.isNotEmpty()) {
                Text("Link to Pending Bill (Optional)", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedBill == null,
                        onClick = { selectedBill = null },
                        label = { Text("Custom") }
                    )
                    pendingBills.forEach { bill ->
                        FilterChip(
                            selected = selectedBill?.id == bill.id,
                            onClick = {
                                selectedBill = bill
                                title = "Pay ${bill.nickname} (${bill.billingMonth})"
                                consumerNumber = bill.consumerNumber
                                amountText = "%.0f".format(bill.totalPayable)
                            },
                            label = { Text("${bill.nickname} • ${formatInr(bill.totalPayable)}") }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; errorMsg = null },
                label = { Text("Reminder Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = consumerNumber,
                    onValueChange = { consumerNumber = it },
                    label = { Text("CA / Meter No.") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMsg = null },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Remind Me", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to "On Due Date", 1 to "1 Day Before", 2 to "2 Days Before", 3 to "3 Days Before", 5 to "5 Days Before").forEach { (days, label) ->
                    FilterChip(
                        selected = daysBefore == days,
                        onClick = { daysBefore = days },
                        label = { Text(label) }
                    )
                }
            }

            if (selectedBill == null) {
                Text("Due In", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1 to "Tomorrow", 5 to "5 Days", 10 to "10 Days", 20 to "20 Days").forEach { (days, label) ->
                        FilterChip(
                            selected = dueInDays == days,
                            onClick = { dueInDays = days },
                            label = { Text(label) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMsg != null) {
                Text(
                    text = errorMsg.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (title.trim().isEmpty()) {
                        errorMsg = "Enter a reminder title"
                        return@Button
                    }
                    val dueMillis = selectedBill?.dueDateMillis
                        ?: (System.currentTimeMillis() + dueInDays * 86_400_000L)
                    onSave(
                        selectedBill?.id ?: "",
                        title.trim(),
                        consumerNumber.trim(),
                        amt,
                        dueMillis,
                        daysBefore.toLong(),
                        note.trim()
                    )
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_reminder_button")
            ) {
                Text("Save & Schedule Reminder")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReadingSheet(
    connections: List<ElectricityConnection>,
    lastReadingForConnection: Double?,
    onSave: (
        connection: ElectricityConnection,
        monthLabel: String,
        yearMonth: String,
        previousReading: Double,
        currentReading: Double,
        readingDateMillis: Long,
        notes: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedConn by remember(connections) { mutableStateOf(connections.firstOrNull()) }

    val cal = remember { Calendar.getInstance() }
    val defaultMonthLabel = remember { SimpleDateFormat("MMM yy", Locale.getDefault()).format(cal.time) }
    val defaultYearMonth = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time) }

    var monthLabel by remember { mutableStateOf(defaultMonthLabel) }
    var yearMonth by remember { mutableStateOf(defaultYearMonth) }
    var previousText by remember { mutableStateOf(lastReadingForConnection?.toInt()?.toString() ?: "14200") }
    var currentText by remember {
        val base = lastReadingForConnection?.toInt() ?: 14200
        mutableStateOf((base + 245).toString())
    }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val prevVal = previousText.toDoubleOrNull() ?: 0.0
    val currVal = currentText.toDoubleOrNull() ?: 0.0
    val computedUnits = (currVal - prevVal).coerceAtLeast(0.0)
    val estCost = computedUnits * (selectedConn?.tariffRate ?: 7.5)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Log Meter Reading", style = MaterialTheme.typography.titleLarge)

            if (connections.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Add a meter connection first to track monthly kWh usage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    connections.forEach { conn ->
                        FilterChip(
                            selected = selectedConn?.id == conn.id,
                            onClick = { selectedConn = conn },
                            label = { Text(conn.nickname) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = monthLabel,
                        onValueChange = { monthLabel = it; errorMsg = null },
                        label = { Text("Label (e.g. Nov 26)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = yearMonth,
                        onValueChange = { yearMonth = it; errorMsg = null },
                        label = { Text("YYYY-MM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = previousText,
                        onValueChange = { previousText = it; errorMsg = null },
                        label = { Text("Previous (kWh)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currentText,
                        onValueChange = { currentText = it; errorMsg = null },
                        label = { Text("Current (kWh)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Units: ${"%.1f".format(computedUnits)} kWh",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Est. ${formatInr(estCost)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g. AC usage high)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        val conn = selectedConn
                        val prev = previousText.toDoubleOrNull()
                        val curr = currentText.toDoubleOrNull()
                        when {
                            conn == null -> errorMsg = "Select a meter connection"
                            monthLabel.trim().length < 2 -> errorMsg = "Enter a short month label"
                            !Regex("^\\d{4}-\\d{2}$").matches(yearMonth.trim()) -> errorMsg = "Use YYYY-MM format (e.g. 2026-11)"
                            prev == null || prev < 0 -> errorMsg = "Enter valid previous reading"
                            curr == null || curr < prev -> errorMsg = "Current reading must be >= previous reading"
                            else -> onSave(
                                conn,
                                monthLabel.trim(),
                                yearMonth.trim(),
                                prev,
                                curr,
                                System.currentTimeMillis(),
                                notes.trim()
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_reading_button")
                ) {
                    Text("Save Meter Reading")
                }
            }
        }
    }
}
