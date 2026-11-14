package com.example.ui.components

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val inrFormat: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
}

fun formatInr(amount: Double): String = inrFormat.format(amount)

fun formatShortDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return "--"
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(epochMillis))
}

fun formatDateTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "--"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(epochMillis))
}

fun daysUntilDue(dueDateMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {
    val diff = dueDateMillis - nowMillis
    return diff / 86_400_000L
}
