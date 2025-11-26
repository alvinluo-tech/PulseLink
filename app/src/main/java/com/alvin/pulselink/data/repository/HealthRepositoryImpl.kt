package com.alvin.pulselink.data.repository

import android.util.Log
import com.alvin.pulselink.domain.model.HealthData
import com.alvin.pulselink.domain.repository.HealthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HealthRepo"

@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : HealthRepository {
    
    /**
     * 获取当前用户的 seniorId
     * 1. 先尝试从 users 集合获取 seniorId 字段
     * 2. 如果没有，查找 senior_profiles 中 userId 匹配的文档
     */
    private suspend fun getCurrentSeniorId(): String? {
        val uid = firebaseAuth.currentUser?.uid ?: return null
        return try {
            Log.d(TAG, "getCurrentSeniorId - Looking for seniorId for UID: $uid")
            
            // 方法1: 从 users 文档获取 seniorId
            val userDoc = firestore.collection("users").document(uid).get().await()
            Log.d(TAG, "getCurrentSeniorId - User doc exists: ${userDoc.exists()}")
            
            val seniorIdFromUser = userDoc.getString("seniorId")
            Log.d(TAG, "getCurrentSeniorId - seniorId from users: $seniorIdFromUser")
            
            if (seniorIdFromUser != null) {
                // 验证这个 senior_profiles 文档是否存在且 userId 匹配
                val profileDoc = firestore.collection("senior_profiles").document(seniorIdFromUser).get().await()
                Log.d(TAG, "getCurrentSeniorId - Profile doc exists: ${profileDoc.exists()}, userId: ${profileDoc.getString("userId")}")
                return seniorIdFromUser
            }
            
            // 方法2: 从 senior_profiles 查找 userId 匹配的文档
            Log.d(TAG, "getCurrentSeniorId - Searching senior_profiles where userId=$uid")
            val profileSnapshot = firestore.collection("senior_profiles")
                .whereEqualTo("userId", uid)
                .limit(1)
                .get()
                .await()
            
            val profileId = profileSnapshot.documents.firstOrNull()?.id
            if (profileId != null) {
                Log.d(TAG, "getCurrentSeniorId - Found seniorId from profile: $profileId")
                return profileId
            }
            
            Log.w(TAG, "getCurrentSeniorId - No seniorId found for user $uid")
            null
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentSeniorId - Failed to get seniorId for user $uid: ${e.message}", e)
            null
        }
    }
    
    override suspend fun getLatestHealthData(): Result<HealthData?> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
            Log.d(TAG, "getLatestHealthData - Current UID: $uid")
            
            val seniorId = getCurrentSeniorId()
            Log.d(TAG, "getLatestHealthData - Got seniorId: $seniorId")
            
            if (seniorId == null) {
                Log.d(TAG, "No seniorId found for current user")
                return Result.success(null)
            }

            Log.d(TAG, "Querying health_records with seniorId=$seniorId, type=BLOOD_PRESSURE")
            
            val snapshot = firestore
                .collection("health_records")
                .whereEqualTo("seniorId", seniorId)
                .whereEqualTo("type", "BLOOD_PRESSURE")
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            Log.d(TAG, "Query returned ${snapshot.documents.size} documents")
            
            val doc = snapshot.documents.firstOrNull()
            if (doc == null) {
                Log.d(TAG, "No documents found")
                Result.success(null)
            } else {
                Log.d(TAG, "Found document: ${doc.id}, seniorId=${doc.getString("seniorId")}")
                val systolic = doc.getLong("systolic")?.toInt()
                val diastolic = doc.getLong("diastolic")?.toInt()
                val heartRate = doc.getLong("heartRate")?.toInt() ?: 0
                val timestamp = doc.getLong("recordedAt") ?: System.currentTimeMillis()

                if (systolic == null || diastolic == null) {
                    Log.w(TAG, "Invalid data: systolic=$systolic, diastolic=$diastolic")
                    Result.success(null)
                } else {
                    Log.d(TAG, "Successfully parsed: BP=$systolic/$diastolic, HR=$heartRate")
                    Result.success(HealthData(systolic, diastolic, heartRate, timestamp))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get latest health data: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getHealthHistory(): Result<List<HealthData>> {
        return try {
            val seniorId = getCurrentSeniorId()
            if (seniorId == null) {
                Log.d(TAG, "No seniorId found for current user")
                return Result.success(emptyList())
            }

            val snapshot = firestore
                .collection("health_records")
                .whereEqualTo("seniorId", seniorId)
                .whereEqualTo("type", "BLOOD_PRESSURE")
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val list = snapshot.documents.mapNotNull { doc ->
                val systolic = doc.getLong("systolic")?.toInt()
                val diastolic = doc.getLong("diastolic")?.toInt()
                val heartRate = doc.getLong("heartRate")?.toInt() ?: 0
                val timestamp = doc.getLong("recordedAt") ?: System.currentTimeMillis()
                if (systolic == null || diastolic == null) null
                else HealthData(systolic, diastolic, heartRate, timestamp)
            }

            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get health history", e)
            Result.failure(e)
        }
    }
    
    override suspend fun saveHealthData(healthData: HealthData): Result<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
            Log.d(TAG, "saveHealthData - Current UID: $uid")
            
            if (uid == null) {
                Log.e(TAG, "saveHealthData - Not logged in")
                return Result.failure(Exception("Not logged in"))
            }
            
            val seniorId = getCurrentSeniorId()
            Log.d(TAG, "saveHealthData - Got seniorId: $seniorId")
            
            if (seniorId == null) {
                Log.e(TAG, "saveHealthData - No seniorId found")
                return Result.failure(Exception("No seniorId found"))
            }
            
            // 使用 Firestore 自动生成的 ID，不需要后续 update
            val docRef = firestore.collection("health_records").document()
            val documentId = docRef.id
            
            val data = hashMapOf(
                "id" to documentId,  // 使用生成的文档 ID
                "seniorId" to seniorId,
                "type" to "BLOOD_PRESSURE",
                "systolic" to healthData.systolic,
                "diastolic" to healthData.diastolic,
                "heartRate" to healthData.heartRate,
                "bloodSugar" to null,
                "weight" to null,
                "recordedAt" to healthData.timestamp,
                "recordedBy" to uid,
                "notes" to ""
            )

            Log.d(TAG, "saveHealthData - Creating document: $documentId with seniorId=$seniorId")
            docRef.set(data).await()
            Log.d(TAG, "saveHealthData - Successfully created health record")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveHealthData - Failed: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 根据老人的 Profile ID 获取最新的健康数据
     * 用于 Caregiver 查看老人的健康状态
     */
    override suspend fun getLatestHealthDataBySeniorUid(seniorId: String): Result<HealthData?> {
        return try {
            Log.d(TAG, "Getting latest health data for seniorId: $seniorId")
            
            val snapshot = firestore
                .collection("health_records")
                .whereEqualTo("seniorId", seniorId)
                .whereEqualTo("type", "blood_pressure")
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull()
            if (doc == null) {
                Log.d(TAG, "No health data found for seniorId: $seniorId")
                Result.success(null)
            } else {
                val systolic = doc.getLong("systolic")?.toInt()
                val diastolic = doc.getLong("diastolic")?.toInt()
                val heartRate = doc.getLong("heartRate")?.toInt() ?: 0
                val timestamp = doc.getLong("recordedAt") ?: System.currentTimeMillis()

                if (systolic == null || diastolic == null) {
                    Log.w(TAG, "Invalid health data for seniorId: $seniorId")
                    Result.success(null)
                } else {
                    Log.d(TAG, "Found health data: BP=$systolic/$diastolic, HR=$heartRate")
                    Result.success(HealthData(systolic, diastolic, heartRate, timestamp))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting health data for seniorId $seniorId: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 测试 Firestore 连接
     * 向 test_logs 集合写入测试数据
     */
    override suspend fun testConnection(): Result<Unit> {
        return try {
            val testData = hashMapOf(
                "timestamp" to System.currentTimeMillis(),
                "status" to "connected",
                "message" to "Firestore connection test successful"
            )
            
            firestore.collection("test_logs")
                .add(testData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
