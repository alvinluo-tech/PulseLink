package com.alvin.pulselink.data.repository

import android.util.Log
import com.alvin.pulselink.domain.model.HealthRecord
import com.alvin.pulselink.domain.model.HealthSummary
import com.alvin.pulselink.domain.repository.HealthRecordRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HealthRecordRepo"
private const val COLLECTION_RECORDS = "health_records"

@Singleton
class HealthRecordRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : HealthRecordRepository {
    
    private val recordsCollection = firestore.collection(COLLECTION_RECORDS)
    
    // ========== 数据解析 ==========
    
    private fun DocumentSnapshot.toHealthRecord(): HealthRecord? {
        return try {
            HealthRecord(
                id = getString("id") ?: "",
                seniorId = getString("seniorId") ?: "",
                type = getString("type") ?: HealthRecord.TYPE_BLOOD_PRESSURE,
                recordedAt = getLong("recordedAt") ?: 0L,
                recordedBy = getString("recordedBy") ?: "",
                systolic = getLong("systolic")?.toInt(),
                diastolic = getLong("diastolic")?.toInt(),
                heartRate = getLong("heartRate")?.toInt(),
                bloodSugar = getDouble("bloodSugar"),
                weight = getDouble("weight"),
                notes = getString("notes") ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse record ${id}", e)
            null
        }
    }
    
    private fun HealthRecord.toFirestoreMap(): Map<String, Any?> {
        return hashMapOf(
            "id" to id,
            "seniorId" to seniorId,
            "type" to type,
            "recordedAt" to recordedAt,
            "recordedBy" to recordedBy,
            "systolic" to systolic,
            "diastolic" to diastolic,
            "heartRate" to heartRate,
            "bloodSugar" to bloodSugar,
            "weight" to weight,
            "notes" to notes
        )
    }
    
    // ========== 创建记录 ==========
    
    override suspend fun createRecord(record: HealthRecord): Result<HealthRecord> {
        return try {
            val recordId = record.id.ifEmpty { UUID.randomUUID().toString() }
            val recordWithId = record.copy(id = recordId)
            
            recordsCollection.document(recordId).set(recordWithId.toFirestoreMap()).await()
            Log.d(TAG, "Created record: $recordId")
            
            Result.success(recordWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create record", e)
            Result.failure(e)
        }
    }
    
    override suspend fun createRecords(records: List<HealthRecord>): Result<List<HealthRecord>> {
        return try {
            val batch = firestore.batch()
            val recordsWithIds = records.map { record ->
                val recordId = record.id.ifEmpty { UUID.randomUUID().toString() }
                val recordWithId = record.copy(id = recordId)
                batch.set(recordsCollection.document(recordId), recordWithId.toFirestoreMap())
                recordWithId
            }
            
            batch.commit().await()
            Log.d(TAG, "Created ${records.size} records")
            
            Result.success(recordsWithIds)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create records", e)
            Result.failure(e)
        }
    }
    
    // ========== 查询记录 ==========
    
    override suspend fun getRecordsBySenior(
        seniorProfileId: String,
        limit: Int,
        startAfter: Long?
    ): Result<List<HealthRecord>> {
        return try {
            var query = recordsCollection
                .whereEqualTo("seniorId", seniorProfileId)
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
            
            if (startAfter != null) {
                query = query.startAfter(startAfter)
            }
            
            val snapshot = query.get().await()
            val records = snapshot.documents.mapNotNull { it.toHealthRecord() }
            
            Result.success(records)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get records by senior", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getRecordsByType(
        seniorProfileId: String,
        type: String,
        limit: Int,
        startAfter: Long?
    ): Result<List<HealthRecord>> {
        return try {
            var query = recordsCollection
                .whereEqualTo("seniorId", seniorProfileId)
                .whereEqualTo("type", type)
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
            
            if (startAfter != null) {
                query = query.startAfter(startAfter)
            }
            
            val snapshot = query.get().await()
            val records = snapshot.documents.mapNotNull { it.toHealthRecord() }
            
            Result.success(records)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get records by type", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getLatestRecords(seniorProfileId: String): Result<List<HealthRecord>> {
        return try {
            Log.d(TAG, "getLatestRecords for seniorId: $seniorProfileId")
            
            val types = listOf(
                HealthRecord.TYPE_BLOOD_PRESSURE,
                HealthRecord.TYPE_HEART_RATE,
                HealthRecord.TYPE_BLOOD_SUGAR,
                HealthRecord.TYPE_WEIGHT
            )
            
            val latestRecords = mutableListOf<HealthRecord>()
            
            for (type in types) {
                val snapshot = recordsCollection
                    .whereEqualTo("seniorId", seniorProfileId)
                    .whereEqualTo("type", type)
                    .orderBy("recordedAt", Query.Direction.DESCENDING)
                    .limit(1)
                    .get().await()
                
                snapshot.documents.firstOrNull()?.toHealthRecord()?.let {
                    Log.d(TAG, "  Found $type record: systolic=${it.systolic}, diastolic=${it.diastolic}, heartRate=${it.heartRate}")
                    latestRecords.add(it)
                }
            }
            
            Log.d(TAG, "Total latest records found: ${latestRecords.size}")
            Result.success(latestRecords)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get latest records for $seniorProfileId: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getHealthSummary(seniorProfileId: String): Result<HealthSummary> {
        return try {
            Log.d(TAG, "getHealthSummary for seniorId: $seniorProfileId")
            
            val latestRecordsResult = getLatestRecords(seniorProfileId)
            if (latestRecordsResult.isFailure) {
                Log.e(TAG, "getLatestRecords failed: ${latestRecordsResult.exceptionOrNull()?.message}")
                return Result.failure(latestRecordsResult.exceptionOrNull()!!)
            }
            
            val latestRecords = latestRecordsResult.getOrNull() ?: emptyList()
            Log.d(TAG, "getHealthSummary - Got ${latestRecords.size} latest records")
            
            val bpRecord = latestRecords.find { it.type == HealthRecord.TYPE_BLOOD_PRESSURE }
            val hrRecord = latestRecords.find { it.type == HealthRecord.TYPE_HEART_RATE }
            
            Log.d(TAG, "getHealthSummary - BP record: $bpRecord")
            Log.d(TAG, "getHealthSummary - HR record: $hrRecord")
            
            val summary = HealthSummary(
                seniorId = seniorProfileId,
                latestBloodPressure = bpRecord,
                latestHeartRate = hrRecord,
                latestBloodSugar = latestRecords.find { it.type == HealthRecord.TYPE_BLOOD_SUGAR }
            )
            
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get health summary for $seniorProfileId: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getRecordsInRange(
        seniorProfileId: String,
        type: String,
        startTime: Long,
        endTime: Long
    ): Result<List<HealthRecord>> {
        return try {
            val snapshot = recordsCollection
                .whereEqualTo("seniorId", seniorProfileId)
                .whereEqualTo("type", type)
                .whereGreaterThanOrEqualTo("recordedAt", startTime)
                .whereLessThanOrEqualTo("recordedAt", endTime)
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .get().await()
            
            val records = snapshot.documents.mapNotNull { it.toHealthRecord() }
            Result.success(records)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get records in range", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getRecordById(recordId: String): Result<HealthRecord> {
        return try {
            val doc = recordsCollection.document(recordId).get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Record not found: $recordId"))
            }
            
            val record = doc.toHealthRecord()
                ?: return Result.failure(Exception("Failed to parse record"))
            
            Result.success(record)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get record by id", e)
            Result.failure(e)
        }
    }
    
    // ========== 更新和删除 ==========
    
    override suspend fun updateRecord(record: HealthRecord): Result<Unit> {
        return try {
            recordsCollection.document(record.id).set(record.toFirestoreMap()).await()
            Log.d(TAG, "Updated record: ${record.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update record", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteRecord(recordId: String): Result<Unit> {
        return try {
            recordsCollection.document(recordId).delete().await()
            Log.d(TAG, "Deleted record: $recordId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete record", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteAllRecords(seniorProfileId: String): Result<Unit> {
        return try {
            val snapshot = recordsCollection
                .whereEqualTo("seniorId", seniorProfileId)
                .get().await()
            
            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            
            Log.d(TAG, "Deleted all records for profile: $seniorProfileId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all records", e)
            Result.failure(e)
        }
    }
    
    // ========== 统计方法 ==========
    
    override suspend fun getAverageBloodPressure(
        seniorProfileId: String,
        startTime: Long,
        endTime: Long
    ): Result<Pair<Double, Double>?> {
        return try {
            val recordsResult = getRecordsInRange(
                seniorProfileId,
                HealthRecord.TYPE_BLOOD_PRESSURE,
                startTime,
                endTime
            )
            
            if (recordsResult.isFailure) {
                return Result.failure(recordsResult.exceptionOrNull()!!)
            }
            
            val records = recordsResult.getOrNull() ?: emptyList()
            
            if (records.isEmpty()) {
                return Result.success(null)
            }
            
            val avgSystolic = records.mapNotNull { it.systolic }.average()
            val avgDiastolic = records.mapNotNull { it.diastolic }.average()
            
            Result.success(Pair(avgSystolic, avgDiastolic))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get average blood pressure", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getAverageHeartRate(
        seniorProfileId: String,
        startTime: Long,
        endTime: Long
    ): Result<Double?> {
        return try {
            val recordsResult = getRecordsInRange(
                seniorProfileId,
                HealthRecord.TYPE_HEART_RATE,
                startTime,
                endTime
            )
            
            if (recordsResult.isFailure) {
                return Result.failure(recordsResult.exceptionOrNull()!!)
            }
            
            val records = recordsResult.getOrNull() ?: emptyList()
            
            if (records.isEmpty()) {
                return Result.success(null)
            }
            
            val avg = records.mapNotNull { it.heartRate }.average()
            Result.success(avg)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get average heart rate", e)
            Result.failure(e)
        }
    }
}
