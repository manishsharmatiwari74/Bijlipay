package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BijliDatabase
import com.example.data.model.BillReminder
import com.example.data.model.BillStatus
import com.example.data.model.ElectricityBill
import com.example.data.model.ElectricityConnection
import com.example.data.model.UsageReading
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BijliRepositoryTest {

    private lateinit var db: BijliDatabase
    private lateinit var repository: BijliRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BijliDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BijliRepository(db.bijliDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun createAndPayBillUpdatesStatusAndTransaction() = runTest {
        val connResult = repository.createConnection(
            ElectricityConnection(
                providerName = "JVVNL (Jaipur City)",
                consumerNumber = "210112345678",
                nickname = "Home Meter",
                tariffRate = 7.65,
                monthlyTargetKwh = 250L
            )
        )
        assertTrue(connResult.isSuccess)
        val connId = connResult.getOrThrow()

        val billResult = repository.createBill(
            ElectricityBill(
                connectionId = connId,
                providerName = "JVVNL (Jaipur City)",
                consumerNumber = "210112345678",
                nickname = "Home Meter",
                billingMonth = "Nov 2026",
                unitsConsumed = 210.0,
                amount = 1606.5,
                lateFee = 0.0,
                dueDateMillis = System.currentTimeMillis() + 86_400_000L
            )
        )
        assertTrue(billResult.isSuccess)
        val billId = billResult.getOrThrow()

        val payResult = repository.payBill(
            billId = billId,
            paymentMethod = "UPI",
            transactionId = "BP123456ABCD",
            paidAtMillis = 1_700_000_000_000L,
            lateFee = 0.0
        )
        assertTrue(payResult.isSuccess)

        val storedBill = repository.getBill(billId).getOrThrow()
        assertEquals(BillStatus.PAID.name, storedBill?.status)
        assertEquals("UPI", storedBill?.paymentMethod)
        assertEquals("BP123456ABCD", storedBill?.transactionId)
    }

    @Test
    fun reminderAndUsageReadingsPersistLocally() = runTest {
        val reminderId = repository.createReminder(
            BillReminder(
                billId = "b1",
                title = "Home Meter JVVNL Due",
                consumerNumber = "210112345678",
                amount = 1500.0,
                dueDateMillis = System.currentTimeMillis() + 172_800_000L,
                remindAtMillis = System.currentTimeMillis() + 86_400_000L
            )
        ).getOrThrow()

        repository.toggleReminder(reminderId, enabled = false).getOrThrow()
        val reminders = repository.observeReminders().first()
        assertEquals(1, reminders.size)
        assertEquals(false, reminders.first().enabled)

        repository.createUsageReading(
            UsageReading(
                connectionId = "c1",
                providerName = "JVVNL (Jaipur City)",
                monthLabel = "Nov 2026",
                yearMonth = "2026-11",
                previousReading = 1000.0,
                currentReading = 1220.0,
                unitsKwh = 220.0,
                estimatedCost = 1683.0,
                readingDateMillis = System.currentTimeMillis()
            )
        ).getOrThrow()

        val readings = repository.observeUsageReadings().first()
        assertEquals(1, readings.size)
        assertEquals(220.0, readings.first().unitsKwh, 0.01)
    }
}
