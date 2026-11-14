package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

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

@IgnoreExtraProperties
data class ElectricityConnection(
    @DocumentId val id: String = "",
    val userId: String = "",
    val providerName: String = "",
    val consumerNumber: String = "",
    val nickname: String = "",
    val meterType: String = MeterType.SMART_METER.name,
    val tariffRate: Double = 7.5,
    val monthlyTargetKwh: Long = 300L,
    val billingCycleDay: Long = 5L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "providerName" to providerName,
        "consumerNumber" to consumerNumber,
        "nickname" to nickname,
        "meterType" to meterType,
        "tariffRate" to tariffRate,
        "monthlyTargetKwh" to monthlyTargetKwh,
        "billingCycleDay" to billingCycleDay,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ElectricityBill(
    @DocumentId val id: String = "",
    val userId: String = "",
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
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val totalPayable: Double
        get() = amount + lateFee

    val billStatusEnum: BillStatus
        get() = runCatching { BillStatus.valueOf(status) }.getOrDefault(BillStatus.PENDING)

    fun isCurrentlyOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return status != BillStatus.PAID.name && dueDateMillis > 0L && dueDateMillis < nowMillis
    }

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "connectionId" to connectionId,
            "providerName" to providerName,
            "consumerNumber" to consumerNumber,
            "nickname" to nickname,
            "billingMonth" to billingMonth,
            "unitsConsumed" to unitsConsumed,
            "amount" to amount,
            "lateFee" to lateFee,
            "dueDateMillis" to dueDateMillis,
            "status" to status,
            "paidAtMillis" to paidAtMillis,
            "paymentMethod" to paymentMethod,
            "transactionId" to transactionId,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class BillReminder(
    @DocumentId val id: String = "",
    val userId: String = "",
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
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "billId" to billId,
            "title" to title,
            "consumerNumber" to consumerNumber,
            "amount" to amount,
            "dueDateMillis" to dueDateMillis,
            "remindAtMillis" to remindAtMillis,
            "daysBeforeDue" to daysBeforeDue,
            "enabled" to enabled,
            "notified" to notified,
            "note" to note?.takeIf { it.isNotBlank() },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class UsageReading(
    @DocumentId val id: String = "",
    val userId: String = "",
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
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "connectionId" to connectionId,
            "providerName" to providerName,
            "monthLabel" to monthLabel,
            "yearMonth" to yearMonth,
            "previousReading" to previousReading,
            "currentReading" to currentReading,
            "unitsKwh" to unitsKwh,
            "estimatedCost" to estimatedCost,
            "readingDateMillis" to readingDateMillis,
            "notes" to notes?.takeIf { it.isNotBlank() },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}
