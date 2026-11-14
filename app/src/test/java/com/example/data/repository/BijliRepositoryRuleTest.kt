package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BillReminder
import com.example.data.model.BillStatus
import com.example.data.model.ElectricityBill
import com.example.data.model.ElectricityConnection
import com.example.data.model.MeterType
import com.example.data.model.UsageReading
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class BijliRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun fullUserFlow_createConnectionBillPayReminderAndUsage_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = BijliRepository(firestore)
        val uniqueSuffix = UUID.randomUUID().toString().take(6)

        val connResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createConnection(
                ElectricityConnection(
                    userId = uid,
                    providerName = "Tata Power",
                    consumerNumber = "TP-$uniqueSuffix",
                    nickname = "Home",
                    meterType = MeterType.SMART_METER.name,
                    tariffRate = 7.5,
                    monthlyTargetKwh = 320L,
                    billingCycleDay = 5L
                )
            )
        }
        assertTrue("Connection create failed: ${connResult.exceptionOrNull()}", connResult.isSuccess)
        val connId = connResult.getOrThrow()

        val billResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createBill(
                ElectricityBill(
                    userId = uid,
                    connectionId = connId,
                    providerName = "Tata Power",
                    consumerNumber = "TP-$uniqueSuffix",
                    nickname = "Home",
                    billingMonth = "Nov 2026",
                    unitsConsumed = 240.0,
                    amount = 1800.0,
                    lateFee = 0.0,
                    dueDateMillis = System.currentTimeMillis() + 5 * 86_400_000L,
                    status = BillStatus.PENDING.name
                )
            )
        }
        assertTrue("Bill create failed: ${billResult.exceptionOrNull()}", billResult.isSuccess)
        val billId = billResult.getOrThrow()

        val payResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.payBill(
                billId = billId,
                paymentMethod = "UPI",
                transactionId = "TXN-$uniqueSuffix",
                paidAtMillis = System.currentTimeMillis(),
                lateFee = 0.0
            )
        }
        assertTrue("Pay bill failed: ${payResult.exceptionOrNull()}", payResult.isSuccess)

        val paidBill = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getBill(billId).getOrThrow()
        }
        assertNotNull(paidBill)
        assertEquals(BillStatus.PAID.name, paidBill?.status)

        // Terminal lock: cannot pay or mutate an already PAID bill
        val secondPayResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.payBill(
                billId = billId,
                paymentMethod = "Card",
                transactionId = "TXN-SECOND-$uniqueSuffix",
                paidAtMillis = System.currentTimeMillis(),
                lateFee = 0.0
            )
        }
        assertTrue("Expected terminal PAID bill update to fail", secondPayResult.isFailure)

        val reminderResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createReminder(
                BillReminder(
                    userId = uid,
                    billId = billId,
                    title = "Home Bill Due",
                    consumerNumber = "TP-$uniqueSuffix",
                    amount = 1800.0,
                    dueDateMillis = System.currentTimeMillis() + 5 * 86_400_000L,
                    remindAtMillis = System.currentTimeMillis() + 2 * 86_400_000L,
                    daysBeforeDue = 3L,
                    enabled = true,
                    notified = false,
                    note = "Pay via UPI"
                )
            )
        }
        assertTrue("Reminder create failed: ${reminderResult.exceptionOrNull()}", reminderResult.isSuccess)

        val readingResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createUsageReading(
                UsageReading(
                    userId = uid,
                    connectionId = connId,
                    providerName = "Tata Power",
                    monthLabel = "Nov 26",
                    yearMonth = "2026-11",
                    previousReading = 1000.0,
                    currentReading = 1240.0,
                    unitsKwh = 240.0,
                    estimatedCost = 1800.0,
                    readingDateMillis = System.currentTimeMillis(),
                    notes = "Normal usage"
                )
            )
        }
        assertTrue("Usage reading create failed: ${readingResult.exceptionOrNull()}", readingResult.isSuccess)

        val observedBills = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeBills(uid).first { list -> list.any { it.id == billId } }
        }
        assertTrue(observedBills.any { it.id == billId && it.status == BillStatus.PAID.name })
    }

    @Test
    fun crossUserAccess_readingAlicesCollectionAsBob_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = BijliRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createConnection(
                ElectricityConnection(
                    userId = aliceUid,
                    providerName = "BESCOM",
                    consumerNumber = "BC-778899",
                    nickname = "Office",
                    meterType = MeterType.THREE_PHASE.name,
                    tariffRate = 8.0,
                    monthlyTargetKwh = 500L,
                    billingCycleDay = 10L
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = BijliRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeConnections(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when Bob observes Alice's connections")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsError)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
    }

    @Test
    fun unauthenticatedCaller_observeBills_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = BijliRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeBills("some_uid_123").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated caller")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsError)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_bijli@test.com"
        const val BOB_EMAIL = "bob_bijli@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
