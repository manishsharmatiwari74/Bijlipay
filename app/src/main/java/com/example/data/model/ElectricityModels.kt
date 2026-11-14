package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MeterType(val label: String) {
    SINGLE_PHASE("Single Phase"),
    THREE_PHASE("Three Phase"),
    SMART_METER("Smart Meter")
}

enum class BillStatus(val label: String) {
    PENDING("Pending"),
    OVERDUE("Overdue"),
    PAID("Paid")
}

enum class PaymentMethod(val displayName: String, val subtitle: String) {
    UPI("UPI", "GPay, PhonePe, Paytm, BHIM"),
    CARD("Debit / Credit Card", "Visa, Mastercard, RuPay"),
    NET_BANKING("Net Banking", "HDFC, SBI, ICICI, Axis & more")
}

@Entity(tableName = "connections")
data class ElectricityConnection(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val providerName: String = "",
    val consumerNumber: String = "",
    val nickname: String = "",
    val meterType: String = MeterType.SMART_METER.name,
    val tariffRate: Double = 7.5,
    val monthlyTargetKwh: Long = 300L,
    val billingCycleDay: Long = 5L,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "bills")
data class ElectricityBill(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val connectionId: String = "",
    val providerName: String = "",
    val consumerNumber: String = "",
    val nickname: String = "",
    val billingMonth: String = "",
    val unitsConsumed: Double = 0.0,
    val amount: Double = 0.0,
    val lateFee: Double = 0.0,
    val dueDateMillis: Long = 0L,
    val status: String = BillStatus.PENDING.name,
    val paidAtMillis: Long? = null,
    val paymentMethod: String? = null,
    val transactionId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val totalPayable: Double
        get() = amount + lateFee

    val billStatusEnum: BillStatus
        get() = runCatching { BillStatus.valueOf(status) }.getOrDefault(BillStatus.PENDING)

    fun isCurrentlyOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return status != BillStatus.PAID.name && dueDateMillis > 0L && dueDateMillis < nowMillis
    }
}

@Entity(tableName = "reminders")
data class BillReminder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val billId: String = "",
    val title: String = "",
    val consumerNumber: String = "",
    val amount: Double = 0.0,
    val dueDateMillis: Long = 0L,
    val remindAtMillis: Long = 0L,
    val daysBeforeDue: Long = 2L,
    val enabled: Boolean = true,
    val notified: Boolean = false,
    val note: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "usage_readings")
data class UsageReading(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val connectionId: String = "",
    val providerName: String = "",
    val monthLabel: String = "",
    val yearMonth: String = "",
    val previousReading: Double = 0.0,
    val currentReading: Double = 0.0,
    val unitsKwh: Double = 0.0,
    val estimatedCost: Double = 0.0,
    val readingDateMillis: Long = 0L,
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
