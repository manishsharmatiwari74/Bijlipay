package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.BillReminder

object ReminderScheduler {
    const val CHANNEL_ID = "bijlipay_bill_reminders"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_desc)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun scheduleReminder(context: Context, reminder: BillReminder) {
        ensureChannel(context)
        if (!reminder.enabled) {
            cancelReminder(context, reminder.id)
            return
        }
        val triggerAt = reminder.remindAtMillis.coerceAtLeast(System.currentTimeMillis() + 5_000L)
        val notificationId = reminder.id.hashCode()
        val message = buildString {
            if (reminder.amount > 0) {
                append("₹${"%.0f".format(reminder.amount)} due")
            } else {
                append("Electricity bill due")
            }
            if (reminder.consumerNumber.isNotBlank()) {
                append(" • CA ${reminder.consumerNumber}")
            }
            if (!reminder.note.isNullOrBlank()) {
                append(" (${reminder.note})")
            }
        }

        val intent = Intent(context, BillReminderReceiver::class.java).apply {
            putExtra(BillReminderReceiver.EXTRA_TITLE, reminder.title)
            putExtra(BillReminderReceiver.EXTRA_MESSAGE, message)
            putExtra(BillReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Non-exact setAndAllowWhileIdle avoids restricted SCHEDULE_EXACT_ALARM permission
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun cancelReminder(context: Context, reminderId: String) {
        val notificationId = reminderId.hashCode()
        val intent = Intent(context, BillReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }

    fun triggerInstantTestNotification(context: Context, title: String, message: String) {
        ensureChannel(context)
        val intent = Intent(context, BillReminderReceiver::class.java).apply {
            putExtra(BillReminderReceiver.EXTRA_TITLE, title)
            putExtra(BillReminderReceiver.EXTRA_MESSAGE, message)
            putExtra(BillReminderReceiver.EXTRA_NOTIFICATION_ID, 9999)
        }
        BillReminderReceiver().onReceive(context, intent)
    }
}
