package com.alvin.pulselink.data.repository

import com.alvin.pulselink.domain.model.MedicationLog
import com.alvin.pulselink.domain.model.MedicationLogStatus
import com.alvin.pulselink.domain.model.MedicationReminder
import com.alvin.pulselink.domain.model.ReminderStatus
import com.alvin.pulselink.domain.repository.MedicationReminderRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Medication Reminder Repository Implementation
 * Firestore Collections:
 * - /reminders/{reminderId}
 * - /medication_logs/{logId}
 */
@Singleton
class MedicationReminderRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MedicationReminderRepository {

    companion object {
        private const val REMINDERS_COLLECTION = "reminders"
        private const val LOGS_COLLECTION = "medication_logs"
    }

    // --- Reminder CRUD ---

    override suspend fun createReminder(reminder: MedicationReminder): Result<String> {
        return try {
            // 如果 ID 为空，自动生成新 ID
            val reminderId = if (reminder.id.isBlank()) {
                firestore.collection(REMINDERS_COLLECTION).document().id
            } else {
                reminder.id
            }
            
            val reminderWithId = reminder.copy(id = reminderId)
            firestore.collection(REMINDERS_COLLECTION)
                .document(reminderId)
                .set(reminderWithId)
                .await()
            Result.success(reminderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateReminder(reminder: MedicationReminder): Result<Unit> {
        return try {
            firestore.collection(REMINDERS_COLLECTION)
                .document(reminder.id)
                .set(reminder)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteReminder(reminderId: String): Result<Unit> {
        return try {
            firestore.collection(REMINDERS_COLLECTION)
                .document(reminderId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReminder(reminderId: String): Result<MedicationReminder?> {
        return try {
            val doc = firestore.collection(REMINDERS_COLLECTION)
                .document(reminderId)
                .get()
                .await()
            
            val reminder = doc.toObject(MedicationReminder::class.java)
            Result.success(reminder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getRemindersForSenior(seniorId: String): Flow<List<MedicationReminder>> = callbackFlow {
        val listener = firestore.collection(REMINDERS_COLLECTION)
            .whereEqualTo("seniorId", seniorId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val reminders = snapshot?.documents?.mapNotNull { 
                    it.toObject(MedicationReminder::class.java)
                } ?: emptyList()
                
                trySend(reminders)
            }
        
        awaitClose { listener.remove() }
    }

    override fun getActiveRemindersForSenior(seniorId: String): Flow<List<MedicationReminder>> = callbackFlow {
        val listener = firestore.collection(REMINDERS_COLLECTION)
            .whereEqualTo("seniorId", seniorId)
            .whereEqualTo("status", ReminderStatus.ACTIVE.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val reminders = snapshot?.documents?.mapNotNull { 
                    it.toObject(MedicationReminder::class.java)
                } ?: emptyList()
                
                trySend(reminders)
            }
        
        awaitClose { listener.remove() }
    }

    override suspend fun updateStock(reminderId: String, newStock: Int): Result<Unit> {
        return try {
            firestore.collection(REMINDERS_COLLECTION)
                .document(reminderId)
                .update("currentStock", newStock)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleReminderStatus(reminderId: String, isPaused: Boolean): Result<Unit> {
        return try {
            val newStatus = if (isPaused) ReminderStatus.PAUSED else ReminderStatus.ACTIVE
            firestore.collection(REMINDERS_COLLECTION)
                .document(reminderId)
                .update("status", newStatus.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Medication Logs ---

    override suspend fun createLog(log: MedicationLog): Result<String> {
        return try {
            val docRef = firestore.collection(LOGS_COLLECTION).document(log.id)
            docRef.set(log).await()
            Result.success(log.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsTaken(logId: String, takenTime: Long): Result<Unit> {
        return try {
            // 使用 Transaction 保证原子性：更新 Log + 递减库存
            firestore.runTransaction { transaction ->
                // 1. 读取 Log 获取 reminderId
                val logRef = firestore.collection(LOGS_COLLECTION).document(logId)
                val logSnapshot = transaction.get(logRef)
                val log = logSnapshot.toObject(MedicationLog::class.java)
                    ?: throw Exception("Log not found: $logId")
                
                // 2. 更新 Log 状态
                transaction.update(
                    logRef,
                    mapOf(
                        "takenTime" to takenTime,
                        "status" to MedicationLogStatus.TAKEN.name
                    )
                )
                
                // 3. 原子化递减 Reminder 的 currentStock (仅当 > 0 时)
                val reminderRef = firestore.collection(REMINDERS_COLLECTION).document(log.reminderId)
                val reminderSnapshot = transaction.get(reminderRef)
                val currentStock = reminderSnapshot.getLong("currentStock") ?: 0
                
                if (currentStock > 0) {
                    transaction.update(reminderRef, "currentStock", currentStock - 1)
                }
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsSkipped(logId: String): Result<Unit> {
        return try {
            firestore.collection(LOGS_COLLECTION)
                .document(logId)
                .update("status", MedicationLogStatus.SKIPPED.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLogsForDate(
        seniorId: String,
        startOfDay: Long,
        endOfDay: Long
    ): Result<List<MedicationLog>> {
        return try {
            val snapshot = firestore.collection(LOGS_COLLECTION)
                .whereEqualTo("seniorId", seniorId)
                .whereGreaterThanOrEqualTo("scheduledTime", startOfDay)
                .whereLessThan("scheduledTime", endOfDay)
                .orderBy("scheduledTime", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val logs = snapshot.documents.mapNotNull { 
                it.toObject(MedicationLog::class.java)
            }
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getLogsForReminder(reminderId: String, limit: Int): Flow<List<MedicationLog>> = callbackFlow {
        val listener = firestore.collection(LOGS_COLLECTION)
            .whereEqualTo("reminderId", reminderId)
            .orderBy("scheduledTime", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val logs = snapshot?.documents?.mapNotNull { 
                    it.toObject(MedicationLog::class.java)
                } ?: emptyList()
                
                trySend(logs)
            }
        
        awaitClose { listener.remove() }
    }

    override fun getTodayPendingLogs(seniorId: String): Flow<List<MedicationLog>> = callbackFlow {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        val listener = firestore.collection(LOGS_COLLECTION)
            .whereEqualTo("seniorId", seniorId)
            .whereEqualTo("status", MedicationLogStatus.PENDING.name)
            .whereGreaterThanOrEqualTo("scheduledTime", startOfDay)
            .orderBy("scheduledTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val logs = snapshot?.documents?.mapNotNull { 
                    it.toObject(MedicationLog::class.java)
                } ?: emptyList()
                
                trySend(logs)
            }
        
        awaitClose { listener.remove() }
    }
    
    override fun getTodayAllLogs(seniorId: String): Flow<List<MedicationLog>> = callbackFlow {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        // 获取今天结束时间（23:59:59）
        val endCalendar = java.util.Calendar.getInstance()
        endCalendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        endCalendar.set(java.util.Calendar.MINUTE, 59)
        endCalendar.set(java.util.Calendar.SECOND, 59)
        endCalendar.set(java.util.Calendar.MILLISECOND, 999)
        val endOfDay = endCalendar.timeInMillis
        
        val listener = firestore.collection(LOGS_COLLECTION)
            .whereEqualTo("seniorId", seniorId)
            .whereGreaterThanOrEqualTo("scheduledTime", startOfDay)
            .whereLessThanOrEqualTo("scheduledTime", endOfDay)
            .orderBy("scheduledTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val logs = snapshot?.documents?.mapNotNull { 
                    it.toObject(MedicationLog::class.java)
                } ?: emptyList()
                
                trySend(logs)
            }
        
        awaitClose { listener.remove() }
    }
}
