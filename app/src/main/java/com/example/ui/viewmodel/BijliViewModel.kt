package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BillReminder
import com.example.data.model.BillStatus
import com.example.data.model.ElectricityBill
import com.example.data.model.ElectricityConnection
import com.example.data.model.UsageReading
import com.example.data.repository.BijliRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class BillHistoryFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    OVERDUE("Overdue"),
    PAID("Paid")
}

data class BijliDashboardData(
    val connections: List<ElectricityConnection> = emptyList(),
    val bills: List<ElectricityBill> = emptyList(),
    val reminders: List<BillReminder> = emptyList(),
    val usageReadings: List<UsageReading> = emptyList()
) {
    val totalPendingAmount: Double
        get() = bills
            .filter { it.status != BillStatus.PAID.name }
            .sumOf { it.totalPayable }

    val pendingBills: List<ElectricityBill>
        get() = bills
            .filter { it.status != BillStatus.PAID.name }
            .sortedBy { it.dueDateMillis }

    val paidBills: List<ElectricityBill>
        get() = bills
            .filter { it.status == BillStatus.PAID.name }
            .sortedByDescending { it.paidAtMillis ?: it.dueDateMillis }

    val totalPaidAllTime: Double
        get() = paidBills.sumOf { it.totalPayable }

    val activeRemindersCount: Int
        get() = reminders.count { it.enabled }
}

sealed interface PaymentCheckoutState {
    data object Idle : PaymentCheckoutState
    data object Processing : PaymentCheckoutState
    data class Paid(val bill: ElectricityBill, val transactionId: String, val method: String) : PaymentCheckoutState
    data class Failed(val message: String) : PaymentCheckoutState
}

class BijliViewModel(
    private val repository: BijliRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(BillHistoryFilter.ALL)
    val selectedFilter: StateFlow<BillHistoryFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedConnectionIdForUsage = MutableStateFlow<String?>(null)
    val selectedConnectionIdForUsage: StateFlow<String?> = _selectedConnectionIdForUsage.asStateFlow()

    private val _paymentState = MutableStateFlow<PaymentCheckoutState>(PaymentCheckoutState.Idle)
    val paymentState: StateFlow<PaymentCheckoutState> = _paymentState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    val dashboardState: StateFlow<UiState<BijliDashboardData>> = combine(
        repository.observeConnections(),
        repository.observeBills(),
        repository.observeReminders(),
        repository.observeUsageReadings()
    ) { connections, bills, reminders, readings ->
        val now = System.currentTimeMillis()
        val normalizedBills = bills.map { bill ->
            if (bill.status == BillStatus.PENDING.name && bill.dueDateMillis > 0L && bill.dueDateMillis < now) {
                bill.copy(status = BillStatus.OVERDUE.name)
            } else {
                bill
            }
        }
        UiState.Success(
            BijliDashboardData(
                connections = connections.sortedBy { it.nickname },
                bills = normalizedBills.sortedByDescending { it.dueDateMillis },
                reminders = reminders.sortedBy { it.remindAtMillis },
                usageReadings = readings.sortedBy { it.yearMonth }
            )
        ) as UiState<BijliDashboardData>
    }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing BijliPay data", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load electricity data"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun setHistoryFilter(filter: BillHistoryFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectConnectionForUsage(connectionId: String?) {
        _selectedConnectionIdForUsage.value = connectionId
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun resetPaymentState() {
        _paymentState.value = PaymentCheckoutState.Idle
    }

    fun addConnection(
        providerName: String,
        consumerNumber: String,
        nickname: String,
        meterType: String,
        tariffRate: Double,
        monthlyTargetKwh: Long,
        billingCycleDay: Long,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.createConnection(
                ElectricityConnection(
                    providerName = providerName.trim(),
                    consumerNumber = consumerNumber.trim(),
                    nickname = nickname.trim(),
                    meterType = meterType,
                    tariffRate = tariffRate,
                    monthlyTargetKwh = monthlyTargetKwh,
                    billingCycleDay = billingCycleDay
                )
            )
            result.onSuccess {
                _actionMessage.value = "Meter connection added"
                onComplete()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to add connection"
            }
        }
    }

    fun deleteConnection(connectionId: String) {
        viewModelScope.launch {
            repository.deleteConnection(connectionId)
                .onSuccess { _actionMessage.value = "Connection removed" }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to delete connection" }
        }
    }

    fun updateConnectionTarget(connection: ElectricityConnection, newTargetKwh: Long) {
        viewModelScope.launch {
            repository.updateConnectionTarget(connection, newTargetKwh)
                .onSuccess { _actionMessage.value = "Monthly target updated to $newTargetKwh kWh" }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to update target" }
        }
    }

    fun addBill(
        connection: ElectricityConnection,
        billingMonth: String,
        unitsConsumed: Double,
        amount: Double,
        lateFee: Double,
        dueDateMillis: Long,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val status = if (dueDateMillis < System.currentTimeMillis()) {
                BillStatus.OVERDUE.name
            } else {
                BillStatus.PENDING.name
            }
            val result = repository.createBill(
                ElectricityBill(
                    connectionId = connection.id,
                    providerName = connection.providerName,
                    consumerNumber = connection.consumerNumber,
                    nickname = connection.nickname,
                    billingMonth = billingMonth.trim(),
                    unitsConsumed = unitsConsumed,
                    amount = amount,
                    lateFee = lateFee,
                    dueDateMillis = dueDateMillis,
                    status = status
                )
            )
            result.onSuccess {
                _actionMessage.value = "JVVNL bill added for ${connection.nickname}"
                onComplete()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to add bill"
            }
        }
    }

    fun prepareQuickJvvnlBill(
        existingConnection: ElectricityConnection?,
        circleName: String,
        kNumber: String,
        nickname: String,
        billingMonth: String,
        unitsConsumed: Double,
        amount: Double,
        dueDateMillis: Long,
        onReadyToPay: (ElectricityBill) -> Unit
    ) {
        viewModelScope.launch {
            val cleanKNo = kNumber.trim()
            val cleanNick = nickname.trim().ifBlank { "JVVNL Meter" }
            val provider = "JVVNL ($circleName)"

            val connectionId = if (existingConnection != null) {
                existingConnection.id
            } else {
                val newConn = ElectricityConnection(
                    providerName = provider,
                    consumerNumber = cleanKNo,
                    nickname = cleanNick
                )
                val connRes = repository.createConnection(newConn)
                if (connRes.isFailure) {
                    _actionMessage.value = connRes.exceptionOrNull()?.localizedMessage
                        ?: "Failed to save JVVNL K-Number"
                    return@launch
                }
                connRes.getOrThrow()
            }

            val draftBill = ElectricityBill(
                connectionId = connectionId,
                providerName = existingConnection?.providerName ?: provider,
                consumerNumber = cleanKNo,
                nickname = existingConnection?.nickname ?: cleanNick,
                billingMonth = billingMonth.trim(),
                unitsConsumed = unitsConsumed,
                amount = amount,
                lateFee = 0.0,
                dueDateMillis = dueDateMillis,
                status = BillStatus.PENDING.name
            )
            repository.createBill(draftBill)
                .onSuccess { newBillId ->
                    onReadyToPay(draftBill.copy(id = newBillId))
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to prepare JVVNL bill"
                }
        }
    }

    fun payBillOnline(
        bill: ElectricityBill,
        paymentMethodLabel: String,
        onSuccess: (ElectricityBill) -> Unit = {}
    ) {
        viewModelScope.launch {
            _paymentState.value = PaymentCheckoutState.Processing
            val txnId = "BP${System.currentTimeMillis().toString().takeLast(8)}${
                UUID.randomUUID().toString().take(4).uppercase()
            }"
            val paidAt = System.currentTimeMillis()
            val result = repository.payBill(
                billId = bill.id,
                paymentMethod = paymentMethodLabel,
                transactionId = txnId,
                paidAtMillis = paidAt,
                lateFee = bill.lateFee
            )
            result.onSuccess {
                val updated = bill.copy(
                    status = BillStatus.PAID.name,
                    paymentMethod = paymentMethodLabel,
                    transactionId = txnId,
                    paidAtMillis = paidAt
                )
                _paymentState.value = PaymentCheckoutState.Paid(updated, txnId, paymentMethodLabel)
                onSuccess(updated)
            }.onFailure { err ->
                _paymentState.value = PaymentCheckoutState.Failed(
                    err.localizedMessage ?: "Payment failed. Please retry."
                )
            }
        }
    }

    fun deleteBill(billId: String) {
        viewModelScope.launch {
            repository.deleteBill(billId)
                .onSuccess { _actionMessage.value = "Bill removed" }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to delete bill" }
        }
    }

    fun addReminder(
        billId: String,
        title: String,
        consumerNumber: String,
        amount: Double,
        dueDateMillis: Long,
        daysBeforeDue: Long,
        note: String,
        onCreated: (BillReminder) -> Unit = {}
    ) {
        viewModelScope.launch {
            val remindAt = (dueDateMillis - daysBeforeDue * 86_400_000L)
                .coerceAtLeast(System.currentTimeMillis() + 60_000L)
            val draft = BillReminder(
                billId = billId,
                title = title.trim(),
                consumerNumber = consumerNumber.trim(),
                amount = amount,
                dueDateMillis = dueDateMillis,
                remindAtMillis = remindAt,
                daysBeforeDue = daysBeforeDue,
                enabled = true,
                notified = false,
                note = note.trim().takeIf { it.isNotEmpty() }
            )
            repository.createReminder(draft)
                .onSuccess { newId ->
                    _actionMessage.value = "Bill reminder scheduled"
                    onCreated(draft.copy(id = newId))
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to save reminder"
                }
        }
    }

    fun toggleReminder(reminder: BillReminder, enabled: Boolean, onToggled: (BillReminder) -> Unit = {}) {
        viewModelScope.launch {
            repository.toggleReminder(reminder.id, enabled)
                .onSuccess {
                    onToggled(reminder.copy(enabled = enabled))
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to update reminder"
                }
        }
    }

    fun deleteReminder(reminderId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteReminder(reminderId)
                .onSuccess {
                    _actionMessage.value = "Reminder deleted"
                    onDeleted()
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to delete reminder"
                }
        }
    }

    fun addUsageReading(
        connection: ElectricityConnection,
        monthLabel: String,
        yearMonth: String,
        previousReading: Double,
        currentReading: Double,
        readingDateMillis: Long,
        notes: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val units = (currentReading - previousReading).coerceAtLeast(0.0)
            val estCost = units * connection.tariffRate
            val reading = UsageReading(
                connectionId = connection.id,
                providerName = connection.providerName,
                monthLabel = monthLabel.trim(),
                yearMonth = yearMonth.trim(),
                previousReading = previousReading,
                currentReading = currentReading,
                unitsKwh = units,
                estimatedCost = estCost,
                readingDateMillis = readingDateMillis,
                notes = notes.trim().takeIf { it.isNotEmpty() }
            )
            repository.createUsageReading(reading)
                .onSuccess {
                    _actionMessage.value = "Logged ${"%.1f".format(units)} kWh for $monthLabel"
                    onComplete()
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to log meter reading"
                }
        }
    }

    fun deleteUsageReading(readingId: String) {
        viewModelScope.launch {
            repository.deleteUsageReading(readingId)
                .onSuccess { _actionMessage.value = "Reading removed" }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to remove reading" }
        }
    }

    private companion object {
        const val TAG = "BijliVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
