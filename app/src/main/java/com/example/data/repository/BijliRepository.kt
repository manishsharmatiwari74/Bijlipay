package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BillReminder
import com.example.data.model.BillStatus
import com.example.data.model.ElectricityBill
import com.example.data.model.ElectricityConnection
import com.example.data.model.UsageReading
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BijliRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun connectionsCollection(userId: String) =
        db.collection("users").document(userId).collection("connections")

    private fun billsCollection(userId: String) =
        db.collection("users").document(userId).collection("bills")

    private fun remindersCollection(userId: String) =
        db.collection("users").document(userId).collection("reminders")

    private fun usageReadingsCollection(userId: String) =
        db.collection("users").document(userId).collection("usageReadings")

    private fun <T> observeUserCollection(
        userId: String,
        path: String,
        query: com.google.firebase.firestore.Query,
        mapper: (com.google.firebase.firestore.QuerySnapshot) -> List<T>
    ): Flow<List<T>> = flow {
        emitAll(
            query.snapshots()
                .map(mapper)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeConnections(userId: String): Flow<List<ElectricityConnection>> =
        observeUserCollection(
            userId = userId,
            path = "users/$userId/connections",
            query = connectionsCollection(userId),
            mapper = { snapshot ->
                snapshot.toObjects(
                    ElectricityConnection::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
        )

    fun observeBills(userId: String): Flow<List<ElectricityBill>> =
        observeUserCollection(
            userId = userId,
            path = "users/$userId/bills",
            query = billsCollection(userId),
            mapper = { snapshot ->
                snapshot.toObjects(
                    ElectricityBill::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
        )

    fun observeReminders(userId: String): Flow<List<BillReminder>> =
        observeUserCollection(
            userId = userId,
            path = "users/$userId/reminders",
            query = remindersCollection(userId),
            mapper = { snapshot ->
                snapshot.toObjects(
                    BillReminder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
        )

    fun observeUsageReadings(userId: String): Flow<List<UsageReading>> =
        observeUserCollection(
            userId = userId,
            path = "users/$userId/usageReadings",
            query = usageReadingsCollection(userId),
            mapper = { snapshot ->
                snapshot.toObjects(
                    UsageReading::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
        )

    suspend fun createConnection(connection: ElectricityConnection): Result<String> {
        val uid = requireUserId()
        val docRef = connectionsCollection(uid).document()
        val payload = connection.copy(userId = uid).toCreateMap()
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateConnectionTarget(connection: ElectricityConnection, newTargetKwh: Long): Result<Unit> {
        val uid = requireUserId()
        require(connection.id.isNotBlank()) { "Connection ID must not be blank" }
        val docRef = connectionsCollection(uid).document(connection.id)
        return try {
            docRef.update(
                mapOf(
                    "monthlyTargetKwh" to newTargetKwh,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteConnection(connectionId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = connectionsCollection(uid).document(connectionId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createBill(bill: ElectricityBill): Result<String> {
        val uid = requireUserId()
        val docRef = billsCollection(uid).document()
        val payload = bill.copy(userId = uid).toCreateMap()
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getBill(billId: String): Result<ElectricityBill?> {
        val uid = requireUserId()
        val docRef = billsCollection(uid).document(billId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    ElectricityBill::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
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
        val uid = requireUserId()
        val docRef = billsCollection(uid).document(billId)
        return try {
            docRef.update(
                mapOf(
                    "status" to BillStatus.PAID.name,
                    "paymentMethod" to paymentMethod,
                    "transactionId" to transactionId,
                    "paidAtMillis" to paidAtMillis,
                    "lateFee" to lateFee,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteBill(billId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = billsCollection(uid).document(billId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createReminder(reminder: BillReminder): Result<String> {
        val uid = requireUserId()
        val docRef = remindersCollection(uid).document()
        val payload = reminder.copy(userId = uid).toCreateMap()
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleReminder(reminderId: String, enabled: Boolean): Result<Unit> {
        val uid = requireUserId()
        val docRef = remindersCollection(uid).document(reminderId)
        return try {
            docRef.update(
                mapOf(
                    "enabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(reminderId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = remindersCollection(uid).document(reminderId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createUsageReading(reading: UsageReading): Result<String> {
        val uid = requireUserId()
        val docRef = usageReadingsCollection(uid).document()
        val payload = reading.copy(userId = uid).toCreateMap()
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteUsageReading(readingId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = usageReadingsCollection(uid).document(readingId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
