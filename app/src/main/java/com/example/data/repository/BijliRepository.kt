package com.example.data.repository

import android.content.Context
import com.example.data.local.BijliDao
import com.example.data.local.BijliDatabase
import com.example.data.model.BillReminder
import com.example.data.model.BillStatus
import com.example.data.model.ElectricityBill
import com.example.data.model.ElectricityConnection
import com.example.data.model.UsageReading
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class BijliRepository(private val dao: BijliDao) {

    constructor(context: Context) : this(
        BijliDatabase.getInstance(context).bijliDao()
    )

    fun observeConnections(): Flow<List<ElectricityConnection>> = dao.observeConnections()

    fun observeBills(): Flow<List<ElectricityBill>> = dao.observeBills()

    fun observeReminders(): Flow<List<BillReminder>> = dao.observeReminders()

    fun observeUsageReadings(): Flow<List<UsageReading>> = dao.observeUsageReadings()

    suspend fun createConnection(connection: ElectricityConnection): Result<String> {
        val id = connection.id.ifBlank { UUID.randomUUID().toString() }
        val now = System.currentTimeMillis()
        val entity = connection.copy(id = id, createdAtMillis = now, updatedAtMillis = now)
        return try {
            dao.insertConnection(entity)
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateConnectionTarget(connection: ElectricityConnection, newTargetKwh: Long): Result<Unit> {
        require(connection.id.isNotBlank()) { "Connection ID must not be blank" }
        return try {
            dao.updateConnectionTarget(connection.id, newTargetKwh, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteConnection(connectionId: String): Result<Unit> {
        return try {
            dao.deleteConnection(connectionId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createBill(bill: ElectricityBill): Result<String> {
        val id = bill.id.ifBlank { UUID.randomUUID().toString() }
        val now = System.currentTimeMillis()
        val entity = bill.copy(id = id, createdAtMillis = now, updatedAtMillis = now)
        return try {
            dao.insertBill(entity)
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBill(billId: String): Result<ElectricityBill?> {
        return try {
            Result.success(dao.getBillById(billId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun payBill(
        billId: String,
        paymentMethod: String,
        transactionId: String,
        paidAtMillis: Long,
        lateFee: Double
    ): Result<Unit> {
        return try {
            dao.markBillPaid(
                billId = billId,
                status = BillStatus.PAID.name,
                paymentMethod = paymentMethod,
                transactionId = transactionId,
                paidAtMillis = paidAtMillis,
                lateFee = lateFee,
                updatedAtMillis = System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBill(billId: String): Result<Unit> {
        return try {
            dao.deleteBill(billId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createReminder(reminder: BillReminder): Result<String> {
        val id = reminder.id.ifBlank { UUID.randomUUID().toString() }
        val now = System.currentTimeMillis()
        val entity = reminder.copy(id = id, createdAtMillis = now, updatedAtMillis = now)
        return try {
            dao.insertReminder(entity)
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleReminder(reminderId: String, enabled: Boolean): Result<Unit> {
        return try {
            dao.toggleReminder(reminderId, enabled, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(reminderId: String): Result<Unit> {
        return try {
            dao.deleteReminder(reminderId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUsageReading(reading: UsageReading): Result<String> {
        val id = reading.id.ifBlank { UUID.randomUUID().toString() }
        val now = System.currentTimeMillis()
        val entity = reading.copy(id = id, createdAtMillis = now, updatedAtMillis = now)
        return try {
            dao.insertUsageReading(entity)
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUsageReading(readingId: String): Result<Unit> {
        return try {
            dao.deleteUsageReading(readingId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
