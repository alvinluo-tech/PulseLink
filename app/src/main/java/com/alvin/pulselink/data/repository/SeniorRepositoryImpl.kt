package com.alvin.pulselink.data.repository

import android.util.Log
import com.alvin.pulselink.domain.model.BloodPressureRecord
import com.alvin.pulselink.domain.model.CaregiverPermissions
import com.alvin.pulselink.domain.model.CaregiverRelationship
import com.alvin.pulselink.domain.model.HealthHistory
import com.alvin.pulselink.domain.model.Senior
import com.alvin.pulselink.domain.repository.SeniorRepository
import com.alvin.pulselink.util.SnrIdGenerator
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SeniorRepo"

@Singleton
class SeniorRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : SeniorRepository {
    
    private val seniorsCollection = firestore.collection("seniors")
    
    // ========== 统一的数据解析函数 ==========
    
    /**
     * 从 Firestore DocumentSnapshot 解析 Senior 对象
     * 统一解析逻辑，避免重复代码
     */
    private fun DocumentSnapshot.toSenior(): Senior? {
        return try {
            val healthHistoryMap = get("healthHistory") as? Map<*, *>
            val bloodPressureMap = healthHistoryMap?.get("bloodPressure") as? Map<*, *>
            
            // 解析 caregiverRelationships Map
            val relationshipsMap = get("caregiverRelationships") as? Map<*, *>
            val caregiverRelationships = parseRelationships(relationshipsMap)
            
            Senior(
                id = getString("id") ?: "",
                name = getString("name") ?: "",
                age = (getLong("age") ?: 0).toInt(),
                gender = getString("gender") ?: "",
                avatarType = getString("avatarType") ?: "",
                caregiverIds = (get("caregiverIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                caregiverRelationships = caregiverRelationships,
                creatorId = getString("creatorId") ?: "",
                createdAt = getLong("createdAt") ?: 0L,
                password = getString("password") ?: "",
                registrationType = getString("registrationType") ?: "CAREGIVER_CREATED",
                healthHistory = parseHealthHistory(healthHistoryMap, bloodPressureMap)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse senior document ${id}", e)
            null
        }
    }
    
    /**
     * 解析 caregiverRelationships Map
     */
    private fun parseRelationships(relationshipsMap: Map<*, *>?): Map<String, CaregiverRelationship> {
        return relationshipsMap?.mapNotNull { (key, value) ->
            val caregiverId = key as? String ?: return@mapNotNull null
            val relMap = value as? Map<*, *> ?: return@mapNotNull null
            
            val permMap = relMap["permissions"] as? Map<*, *>
            val permissions = CaregiverPermissions(
                canViewHealthData = permMap?.get("canViewHealthData") as? Boolean ?: true,
                canViewReminders = permMap?.get("canViewReminders") as? Boolean ?: true,
                canEditReminders = permMap?.get("canEditReminders") as? Boolean ?: true,
                canApproveLinkRequests = permMap?.get("canApproveLinkRequests") as? Boolean ?: false
            )
            
            caregiverId to CaregiverRelationship(
                relationship = relMap["relationship"] as? String ?: "",
                nickname = relMap["nickname"] as? String ?: "",
                linkedAt = relMap["linkedAt"] as? Long ?: System.currentTimeMillis(),
                status = relMap["status"] as? String ?: "active",
                message = relMap["message"] as? String ?: "",
                approvedBy = relMap["approvedBy"] as? String ?: "",
                permissions = permissions
            )
        }?.toMap() ?: emptyMap()
    }
    
    /**
     * 解析 healthHistory
     */
    private fun parseHealthHistory(healthHistoryMap: Map<*, *>?, bloodPressureMap: Map<*, *>?): HealthHistory {
        return HealthHistory(
            bloodPressure = bloodPressureMap?.let {
                BloodPressureRecord(
                    systolic = (it["systolic"] as? Long)?.toInt() ?: 0,
                    diastolic = (it["diastolic"] as? Long)?.toInt() ?: 0,
                    recordedAt = it["recordedAt"] as? Long ?: 0L
                )
            },
            heartRate = (healthHistoryMap?.get("heartRate") as? Long)?.toInt(),
            bloodSugar = healthHistoryMap?.get("bloodSugar") as? Double,
            medicalConditions = (healthHistoryMap?.get("medicalConditions") as? List<*>)
                ?.mapNotNull { it as? String } ?: emptyList(),
            medications = (healthHistoryMap?.get("medications") as? List<*>)
                ?.mapNotNull { it as? String } ?: emptyList(),
            allergies = (healthHistoryMap?.get("allergies") as? List<*>)
                ?.mapNotNull { it as? String } ?: emptyList()
        )
    }
    
    /**
     * 将 Senior 转换为 Firestore 数据格式
     */
    private fun Senior.toFirestoreMap(): Map<String, Any?> {
        val relationshipsMap = caregiverRelationships.mapValues { (_, relationship) ->
            hashMapOf(
                "relationship" to relationship.relationship,
                "nickname" to relationship.nickname,
                "linkedAt" to relationship.linkedAt,
                "status" to relationship.status,
                "message" to relationship.message,
                "approvedBy" to relationship.approvedBy,
                "permissions" to hashMapOf(
                    "canViewHealthData" to relationship.permissions.canViewHealthData,
                    "canViewReminders" to relationship.permissions.canViewReminders,
                    "canEditReminders" to relationship.permissions.canEditReminders,
                    "canApproveLinkRequests" to relationship.permissions.canApproveLinkRequests
                )
            )
        }
        
        return hashMapOf(
            "id" to id,
            "name" to name,
            "age" to age,
            "gender" to gender,
            "avatarType" to avatarType,
            "caregiverIds" to caregiverIds,
            "caregiverRelationships" to relationshipsMap,
            "creatorId" to creatorId,
            "createdAt" to createdAt,
            "password" to password,
            "registrationType" to registrationType,
            "healthHistory" to hashMapOf(
                "bloodPressure" to healthHistory.bloodPressure?.let {
                    hashMapOf(
                        "systolic" to it.systolic,
                        "diastolic" to it.diastolic,
                        "recordedAt" to it.recordedAt
                    )
                },
                "heartRate" to healthHistory.heartRate,
                "bloodSugar" to healthHistory.bloodSugar,
                "medicalConditions" to healthHistory.medicalConditions,
                "medications" to healthHistory.medications,
                "allergies" to healthHistory.allergies
            )
        )
    }
    
    // ========== Repository 方法实现 ==========
    
    override suspend fun createSenior(senior: Senior): Result<Senior> {
        return try {
            val seniorId = senior.id.ifEmpty { SnrIdGenerator.generate() }
            val seniorWithId = senior.copy(id = seniorId)
            
            seniorsCollection.document(seniorId).set(seniorWithId.toFirestoreMap()).await()
            Result.success(seniorWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create senior", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getSeniorsByCaregiver(caregiverId: String): Result<List<Senior>> {
        return try {
            Log.d(TAG, "getSeniorsByCaregiver - caregiverId: $caregiverId")
            
            // 查询两种情况并合并
            val snapshot1 = seniorsCollection
                .whereArrayContains("caregiverIds", caregiverId)
                .get().await()
            
            val snapshot2 = seniorsCollection
                .whereEqualTo("creatorId", caregiverId)
                .get().await()
            
            val allDocs = (snapshot1.documents + snapshot2.documents).distinctBy { it.id }
            Log.d(TAG, "getSeniorsByCaregiver - Found ${allDocs.size} documents")
            
            val seniors = allDocs.mapNotNull { it.toSenior() }
            Result.success(seniors)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get seniors by caregiver", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getSeniorsByCreator(creatorId: String): Result<List<Senior>> {
        return try {
            Log.d(TAG, "getSeniorsByCreator - creatorId: $creatorId")
            
            val snapshot = seniorsCollection
                .whereEqualTo("creatorId", creatorId)
                .get().await()
            
            Log.d(TAG, "getSeniorsByCreator - Found ${snapshot.size()} documents")
            
            val seniors = snapshot.documents.mapNotNull { it.toSenior() }
            Result.success(seniors)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get seniors by creator", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getSeniorById(seniorId: String): Result<Senior> {
        return try {
            val doc = seniorsCollection.document(seniorId).get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Senior not found"))
            }
            
            val senior = doc.toSenior() ?: return Result.failure(Exception("Failed to parse senior"))
            Result.success(senior)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get senior by id", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateSenior(senior: Senior): Result<Unit> {
        return try {
            val updateData = senior.toFirestoreMap().filterKeys { 
                it != "id" && it != "creatorId" && it != "createdAt" && it != "registrationType" 
            }
            
            seniorsCollection.document(senior.id).update(updateData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update senior", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteSenior(seniorId: String): Result<Unit> {
        return try {
            functions.getHttpsCallable("deleteSeniorAccount")
                .call(hashMapOf("seniorId" to seniorId))
                .await()
            
            seniorsCollection.document(seniorId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete senior", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getPendingLinkRequests(creatorId: String): Result<List<Senior>> {
        return try {
            val snapshot = seniorsCollection
                .whereEqualTo("creatorId", creatorId)
                .get().await()
            
            val seniors = snapshot.documents
                .mapNotNull { it.toSenior() }
                .filter { senior -> 
                    senior.caregiverRelationships.any { it.value.status == "pending" }
                }
            
            Result.success(seniors)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get pending link requests", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getSeniorsWithPendingByCaregiver(caregiverId: String): Result<List<Senior>> {
        return try {
            // 由于 Firestore 不支持直接查询 Map 的 key，需要获取所有后过滤
            val snapshot = seniorsCollection.get().await()
            
            val seniors = snapshot.documents
                .mapNotNull { it.toSenior() }
                .filter { it.caregiverRelationships.containsKey(caregiverId) }
            
            Result.success(seniors)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get seniors with pending", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getPendingRequestsByCaregiver(caregiverId: String): Result<List<Senior>> {
        // 使用独立的 linkRequests collection，返回空列表
        return Result.success(emptyList())
    }
    
    override suspend fun getSeniorAuthUid(seniorId: String): Result<String?> {
        return try {
            Log.d(TAG, "Getting Auth UID for seniorId: $seniorId")
            
            val snapshot = firestore.collection("users")
                .whereEqualTo("seniorId", seniorId)
                .limit(1)
                .get().await()
            
            val uid = snapshot.documents.firstOrNull()?.id
            Log.d(TAG, if (uid != null) "Found UID: $uid" else "No UID found for seniorId: $seniorId")
            
            Result.success(uid)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting UID for seniorId $seniorId", e)
            Result.failure(e)
        }
    }
}
