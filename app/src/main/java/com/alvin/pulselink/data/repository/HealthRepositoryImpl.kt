package com.alvin.pulselink.data.repository

import com.alvin.pulselink.domain.model.HealthData
import com.alvin.pulselink.domain.repository.HealthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : HealthRepository {
    
    override suspend fun getLatestHealthData(): Result<HealthData?> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                // 未登录则无数据库信息，返回空以触发默认显示
                return Result.success(null)
            }

            val snapshot = firestore
                .collection("health_data")
                .document(uid)
                .collection("records")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull()
            if (doc == null) {
                Result.success(null)
            } else {
                val systolic = (doc.getLong("systolic") ?: doc.getDouble("systolic")?.toLong())?.toInt()
                val diastolic = (doc.getLong("diastolic") ?: doc.getDouble("diastolic")?.toLong())?.toInt()
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                if (systolic == null || diastolic == null) {
                    Result.success(null)
                } else {
                    Result.success(HealthData(systolic, diastolic, timestamp))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getHealthHistory(): Result<List<HealthData>> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                // 未登录则返回空列表，界面回落到默认/空数据
                return Result.success(emptyList())
            }

            val snapshot = firestore
                .collection("health_data")
                .document(uid)
                .collection("records")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val list = snapshot.documents.mapNotNull { doc ->
                val systolic = (doc.getLong("systolic") ?: doc.getDouble("systolic")?.toLong())?.toInt()
                val diastolic = (doc.getLong("diastolic") ?: doc.getDouble("diastolic")?.toLong())?.toInt()
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                if (systolic == null || diastolic == null) null
                else HealthData(systolic, diastolic, timestamp)
            }

            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun saveHealthData(healthData: HealthData): Result<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val data = hashMapOf(
                "systolic" to healthData.systolic,
                "diastolic" to healthData.diastolic,
                "timestamp" to (healthData.timestamp)
                // heartRate 字段暂未纳入领域模型，如需要可在此扩展
            )

            firestore
                .collection("health_data")
                .document(uid)
                .collection("records")
                .add(data)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
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
