package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BillReminder
import com.example.data.model.ElectricityBill
import com.example.data.model.ElectricityConnection
import com.example.data.model.UsageReading
import kotlinx.coroutines.flow.Flow

@Dao
interface BijliDao {
    @Query("SELECT * FROM connections ORDER BY nickname ASC")
    fun observeConnections(): Flow<List<ElectricityConnection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: ElectricityConnection)

    @Query("UPDATE connections SET monthlyTargetKwh = :targetKwh, updatedAtMillis = :updatedAtMillis WHERE id = :connectionId")
    suspend fun updateConnectionTarget(connectionId: String, targetKwh: Long, updatedAtMillis: Long)

    @Query("DELETE FROM connections WHERE id = :connectionId")
    suspend fun deleteConnection(connectionId: String)

    @Query("SELECT * FROM bills ORDER BY dueDateMillis DESC")
    fun observeBills(): Flow<List<ElectricityBill>>

    @Query("SELECT * FROM bills WHERE id = :billId LIMIT 1")
    suspend fun getBillById(billId: String): ElectricityBill?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: ElectricityBill)

    @Query(
        """
        UPDATE bills
        SET status = :status,
            paymentMethod = :paymentMethod,
            transactionId = :transactionId,
            paidAtMillis = :paidAtMillis,
            lateFee = :lateFee,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :billId
        """
    )
    suspend fun markBillPaid(
        billId: String,
        status: String,
        paymentMethod: String,
        transactionId: String,
        paidAtMillis: Long,
        lateFee: Double,
        updatedAtMillis: Long
    )

    @Query("DELETE FROM bills WHERE id = :billId")
    suspend fun deleteBill(billId: String)

    @Query("SELECT * FROM reminders ORDER BY remindAtMillis ASC")
    fun observeReminders(): Flow<List<BillReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: BillReminder)

    @Query("UPDATE reminders SET enabled = :enabled, updatedAtMillis = :updatedAtMillis WHERE id = :reminderId")
    suspend fun toggleReminder(reminderId: String, enabled: Boolean, updatedAtMillis: Long)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminder(reminderId: String)

    @Query("SELECT * FROM usage_readings ORDER BY yearMonth ASC")
    fun observeUsageReadings(): Flow<List<UsageReading>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageReading(reading: UsageReading)

    @Query("DELETE FROM usage_readings WHERE id = :readingId")
    suspend fun deleteUsageReading(readingId: String)
}
