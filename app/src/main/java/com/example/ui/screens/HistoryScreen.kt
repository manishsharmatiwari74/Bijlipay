package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BillStatus
import com.example.data.model.ElectricityBill
import com.example.ui.components.formatInr
import com.example.ui.theme.ElectricEmeraldDark
import com.example.ui.viewmodel.BijliDashboardData
import com.example.ui.viewmodel.BillHistoryFilter

@Composable
fun HistoryScreen(
    data: BijliDashboardData,
    selectedFilter: BillHistoryFilter,
    searchQuery: String,
    onFilterChange: (BillHistoryFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onPayBillClick: (ElectricityBill) -> Unit,
    onViewReceiptClick: (ElectricityBill) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredBills = remember(data.bills, selectedFilter, searchQuery) {
        data.bills.filter { bill ->
            val statusMatch = when (selectedFilter) {
                BillHistoryFilter.ALL -> true
                BillHistoryFilter.PAID -> bill.status == BillStatus.PAID.name
                BillHistoryFilter.OVERDUE -> bill.status == BillStatus.OVERDUE.name || bill.isCurrentlyOverdue()
                BillHistoryFilter.PENDING -> bill.status == BillStatus.PENDING.name && !bill.isCurrentlyOverdue()
            }
            val query = searchQuery.trim()
            val queryMatch = query.isEmpty() ||
                bill.providerName.contains(query, ignoreCase = true) ||
                bill.consumerNumber.contains(query, ignoreCase = true) ||
                bill.nickname.contains(query, ignoreCase = true) ||
                bill.billingMonth.contains(query, ignoreCase = true) ||
                (bill.transactionId?.contains(query, ignoreCase = true) == true)
            statusMatch && queryMatch
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Paid History",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatInr(data.totalPaidAllTime),
                            style = MaterialTheme.typography.headlineMedium,
                            color = ElectricEmeraldDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${data.paidBills.size} paid • ${data.pendingBills.size} pending",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ReceiptLong,
                        contentDescription = "Bill History Summary",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search JVVNL K-Number, circle, month, TXN...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input")
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BillHistoryFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.label) },
                        modifier = Modifier.testTag("history_filter_${filter.name.lowercase()}")
                    )
                }
            }
        }

        if (filteredBills.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "No matching JVVNL bills found",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "JVVNL bills you add or pay will appear here with shareable payment receipts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredBills, key = { it.id }) { bill ->
                BillListCard(
                    bill = bill,
                    onPayClick = { onPayBillClick(bill) },
                    onReceiptClick = { onViewReceiptClick(bill) }
                )
            }
        }
    }
}
