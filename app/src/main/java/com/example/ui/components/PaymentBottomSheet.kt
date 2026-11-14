package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ElectricityBill
import com.example.data.model.PaymentMethod
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.ElectricEmeraldDark
import com.example.ui.theme.MidnightTeal
import com.example.ui.viewmodel.PaymentCheckoutState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentBottomSheet(
    bill: ElectricityBill,
    paymentState: PaymentCheckoutState,
    onPayConfirm: (paymentMethodLabel: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI) }
    var upiId by remember { mutableStateOf("consumer@okaxis") }
    var cardLast4 by remember { mutableStateOf("4242") }
    var selectedBank by remember { mutableStateOf("HDFC Bank") }
    var inputError by remember { mutableStateOf<String?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (paymentState) {
                is PaymentCheckoutState.Paid -> {
                    ReceiptContent(
                        bill = paymentState.bill,
                        onShare = { shareReceipt(context, paymentState.bill) },
                        onDone = onDismiss
                    )
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "JVVNL Online Bill Payment",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "${bill.providerName} • ${bill.nickname}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = formatInr(bill.totalPayable),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SummaryLine("JVVNL K Number", bill.consumerNumber)
                            SummaryLine("Billing Month", bill.billingMonth)
                            SummaryLine("Units Billed", "${"%.0f".format(bill.unitsConsumed)} kWh")
                            SummaryLine("Energy Charges", formatInr(bill.amount))
                            if (bill.lateFee > 0) {
                                SummaryLine("Late Payment Fee", formatInr(bill.lateFee))
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            SummaryLine("Total Payable", formatInr(bill.totalPayable), bold = true)
                        }
                    }

                    // Official JVVNL Portal Direct Links
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Official JVVNL Bill Payment Portals",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        openExternalUrl(context, "https://www.bijlimitra.com")
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("BijliMitra")
                                }
                                OutlinedButton(
                                    onClick = {
                                        openExternalUrl(context, "https://energy.rajasthan.gov.in/jvvnl")
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("JVVNL Site")
                                }
                            }
                            Text(
                                text = "JVVNL Toll-Free Helpline: 1912 / 1800-180-6507",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Choose Payment Mode",
                        style = MaterialTheme.typography.titleSmall
                    )

                    PaymentMethod.entries.forEach { method ->
                        val isSelected = selectedMethod == method
                        val icon = when (method) {
                            PaymentMethod.UPI -> Icons.Filled.QrCode2
                            PaymentMethod.CARD -> Icons.Filled.CreditCard
                            PaymentMethod.NET_BANKING -> Icons.Filled.AccountBalance
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMethod = method
                                    inputError = null
                                }
                                .testTag("payment_method_${method.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedMethod = method
                                        inputError = null
                                    }
                                )
                                Icon(
                                    imageVector = icon,
                                    contentDescription = method.displayName,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = method.displayName,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = method.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    when (selectedMethod) {
                        PaymentMethod.UPI -> {
                            OutlinedTextField(
                                value = upiId,
                                onValueChange = {
                                    upiId = it
                                    inputError = null
                                },
                                label = { Text("UPI ID (VPA)") },
                                placeholder = { Text("name@okicici") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedButton(
                                onClick = {
                                    val uri = Uri.Builder()
                                        .scheme("upi")
                                        .authority("pay")
                                        .appendQueryParameter("pa", upiId.trim().ifBlank { "jvvnl@upi" })
                                        .appendQueryParameter("pn", "JVVNL Jaipur Discom")
                                        .appendQueryParameter("tn", "JVVNL Bill K-No ${bill.consumerNumber}")
                                        .appendQueryParameter("am", "%.2f".format(bill.totalPayable))
                                        .appendQueryParameter("cu", "INR")
                                        .build()
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "No installed UPI app detected. Use Pay & Record below.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Launch Installed UPI App")
                            }
                        }

                        PaymentMethod.CARD -> {
                            OutlinedTextField(
                                value = cardLast4,
                                onValueChange = {
                                    if (it.length <= 4 && it.all(Char::isDigit)) {
                                        cardLast4 = it
                                        inputError = null
                                    }
                                },
                                label = { Text("Card Last 4 Digits") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        PaymentMethod.NET_BANKING -> {
                            val banks = listOf("HDFC Bank", "SBI", "ICICI Bank", "Axis Bank")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                banks.forEach { bank ->
                                    val active = selectedBank == bank
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedBank = bank }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = bank.substringBefore(" "),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (inputError != null || paymentState is PaymentCheckoutState.Failed) {
                        val err = inputError ?: (paymentState as? PaymentCheckoutState.Failed)?.message.orEmpty()
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    val isProcessing = paymentState is PaymentCheckoutState.Processing
                    Button(
                        onClick = {
                            val methodLabel = when (selectedMethod) {
                                PaymentMethod.UPI -> {
                                    if (!upiId.contains("@") || upiId.trim().length < 5) {
                                        inputError = "Enter a valid UPI ID (e.g., name@okaxis)"
                                        return@Button
                                    }
                                    "UPI (${upiId.trim()})"
                                }
                                PaymentMethod.CARD -> {
                                    if (cardLast4.length != 4) {
                                        inputError = "Enter 4 digits of your card"
                                        return@Button
                                    }
                                    "Card •••• $cardLast4"
                                }
                                PaymentMethod.NET_BANKING -> "NetBanking ($selectedBank)"
                            }
                            onPayConfirm(methodLabel)
                        },
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MidnightTeal,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("confirm_payment_button")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Recording Payment...")
                        } else {
                            Icon(Icons.Filled.ElectricBolt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mark JVVNL Bill Paid (${formatInr(bill.totalPayable)})")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptModalSheet(
    bill: ElectricityBill,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp)
                .padding(bottom = 28.dp)
        ) {
            ReceiptContent(
                bill = bill,
                onShare = { shareReceipt(context, bill) },
                onDone = onDismiss
            )
        }
    }
}

@Composable
private fun ReceiptContent(
    bill: ElectricityBill,
    onShare: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = ElectricEmerald.copy(alpha = 0.16f),
            shape = CircleShape,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "JVVNL Payment Recorded",
                    tint = ElectricEmeraldDark,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Text(
            text = "JVVNL Bill Paid",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = formatInr(bill.totalPayable),
            style = MaterialTheme.typography.displaySmall,
            color = ElectricEmeraldDark
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryLine("Transaction ID", bill.transactionId ?: "--", bold = true)
                SummaryLine("Utility Board", bill.providerName)
                SummaryLine("JVVNL K Number", bill.consumerNumber)
                SummaryLine("Connection", bill.nickname)
                SummaryLine("Billing Cycle", bill.billingMonth)
                SummaryLine("Units Consumed", "${"%.0f".format(bill.unitsConsumed)} kWh")
                SummaryLine("Payment Mode", bill.paymentMethod ?: "Online")
                SummaryLine("Paid On", formatDateTime(bill.paidAtMillis ?: System.currentTimeMillis()))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onShare,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Receipt")
            }
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun openExternalUrl(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show()
    }
}

private fun shareReceipt(context: android.content.Context, bill: ElectricityBill) {
    val text = buildString {
        appendLine("BijliPay • JVVNL Electricity Bill Receipt")
        appendLine("--------------------------------")
        appendLine("Status: PAID")
        appendLine("Amount Paid: ${formatInr(bill.totalPayable)}")
        appendLine("Transaction ID: ${bill.transactionId ?: "--"}")
        appendLine("Board: ${bill.providerName} (${bill.nickname})")
        appendLine("JVVNL K Number: ${bill.consumerNumber}")
        appendLine("Billing Month: ${bill.billingMonth}")
        appendLine("Units: ${"%.0f".format(bill.unitsConsumed)} kWh")
        appendLine("Mode: ${bill.paymentMethod ?: "Online"}")
        appendLine("Paid On: ${formatDateTime(bill.paidAtMillis ?: System.currentTimeMillis())}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "JVVNL Bill Receipt - ${bill.billingMonth}")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share JVVNL Receipt"))
}
