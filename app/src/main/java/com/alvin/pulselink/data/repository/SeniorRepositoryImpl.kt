package com.alvin.pulselink.data.repository

import com.alvin.pulselink.domain.model.BloodPressureRecord
import com.alvin.pulselink.domain.model.CaregiverRelationship
import com.alvin.pulselink.domain.model.HealthHistory
import com.alvin.pulselink.domain.model.Senior
import com.alvin.pulselink.domain.repository.SeniorRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeniorRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : SeniorRepository {
    
    private val seniorsCollection = firestore.collection("seniors")
    
    override suspend fun createSenior(senior: Senior): Result<Senior> {
        return try {
            // 生成唯一ID
            val seniorId = senior.id.ifEmpty { 
                "SNR-${UUID.randomUUID().toString().substring(0, 8).uppercase()}" 
            }
            
            // 默认将创建者加�?caregiverIds，并记录 creatorId
            val creatorId = if (senior.creatorId.isNotBlank()) senior.creatorId else senior.caregiverIds.firstOrNull() ?: ""
            val caregiverIds = if (senior.caregiverIds.isNotEmpty()) senior.caregiverIds else listOfNotNull(creatorId)

            // 转换 caregiverRelationships Map �?Firestore 格式
            val relationshipsMap = senior.caregiverRelationships.mapValues { (_, relationship) ->
                hashMapOf(
                    "relationship" to relationship.relationship,
                    "nickname" to relationship.nickname,
                    "linkedAt" to relationship.linkedAt,
                    "status" to relationship.status
                )
            }

            val seniorData = hashMapOf(
                "id" to seniorId,
                "name" to senior.name,
                "age" to senior.age,
                "gender" to senior.gender,
                "avatarType" to senior.avatarType,
                "caregiverIds" to caregiverIds,
                "pendingCaregiversIds" to senior.pendingCaregiversIds,
                "caregiverRelationships" to relationshipsMap,
                "creatorId" to creatorId,
                "createdAt" to senior.createdAt,
                "password" to senior.password, // 存储密码（用于生成二维码）
                "healthHistory" to hashMapOf(
                    "bloodPressure" to senior.healthHistory.bloodPressure?.let {
                        hashMapOf(
                            "systolic" to it.systolic,
                            "diastolic" to it.diastolic,
                            "recordedAt" to it.recordedAt
                        )
                    },
                    "heartRate" to senior.healthHistory.heartRate,
                    "bloodSugar" to senior.healthHistory.bloodSugar,
                    "medicalConditions" to senior.healthHistory.medicalConditions,
                    "medications" to senior.healthHistory.medications,
                    "allergies" to senior.healthHistory.allergies
                )
            )
            
            seniorsCollection.document(seniorId).set(seniorData).await()
            Result.success(senior.copy(id = seniorId, caregiverIds = caregiverIds, creatorId = creatorId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getSeniorsByCaregiver(caregiverId: String): Result<List<Senior>> {
        return try {
            android.util.Log.d("SeniorRepo", "getSeniorsByCaregiver - Querying for caregiverId: $caregiverId")
            val snapshot = seniorsCollection
                .whereArrayContains("caregiverIds", caregiverId)
                .get()
                .await()
            
            android.util.Log.d("SeniorRepo", "getSeniorsByCaregiver - Found ${snapshot.size()} documents")
            snapshot.documents.forEach { doc ->
                android.util.Log.d("SeniorRepo", "  - Document ${doc.id}: caregiverIds=${doc.get("caregiverIds")}")
            }
            
            val seniors = snapshot.documents.mapNotNull { doc ->
                try {
                    val healthHistoryMap = doc.get("healthHistory") as? Map<*, *>
                    val bloodPressureMap = healthHistoryMap?.get("bloodPressure") as? Map<*, *>
                    
                    // 读取 caregiverRelationships Map
                    val relationshipsMap = doc.get("caregiverRelationships") as? Map<*, *>
                    val caregiverRelationships = relationshipsMap?.mapNotNull { (key, value) ->
                        val caregiverId = key as? String ?: return@mapNotNull null
                        val relMap = value as? Map<*, *> ?: return@mapNotNull null
                        caregiverId to CaregiverRelationship(
                            relationship = relMap["relationship"] as? String ?: "",
                            nickname = relMap["nickname"] as? String ?: "",
                            linkedAt = relMap["linkedAt"] as? Long ?: System.currentTimeMillis(),
                            status = relMap["status"] as? String ?: "active",
                            message = relMap["message"] as? String ?: ""
                        )
                    }?.toMap() ?: emptyMap()
                    
                    Senior(
                        id = doc.getString("id") ?: "",
                        name = doc.getString("name") ?: "",
                        age = (doc.getLong("age") ?: 0).toInt(),
                        gender = doc.getString("gender") ?: "",
                        avatarType = doc.getString("avatarType") ?: "",
                        caregiverIds = (doc.get("caregiverIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        caregiverRelationships = caregiverRelationships,
                        creatorId = doc.getString("creatorId") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        password = doc.getString("password") ?: "", // 读取密码
                        healthHistory = HealthHistory(
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
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(seniors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getSeniorsByCreator(creatorId: String): Result<List<Senior>> {
        return try {
            android.util.Log.d("SeniorRepo", "getSeniorsByCreator - Querying for creatorId: $creatorId")
            val snapshot = seniorsCollection
                .whereEqualTo("creatorId", creatorId)
                .get()
                .await()
            
            android.util.Log.d("SeniorRepo", "getSeniorsByCreator - Found ${snapshot.size()} documents")
            snapshot.documents.forEach { doc ->
                android.util.Log.d("SeniorRepo", "  - Document ${doc.id}: name=${doc.getString("name")}, creatorId=${doc.get("creatorId")}")
            }
            
            val seniors = snapshot.documents.mapNotNull { doc ->
                try {
                    val healthHistoryMap = doc.get("healthHistory") as? Map<*, *>
                    val bloodPressureMap = healthHistoryMap?.get("bloodPressure") as? Map<*, *>
                    
                    // 读取 caregiverRelationships Map
                    val relationshipsMap = doc.get("caregiverRelationships") as? Map<*, *>
                    val caregiverRelationships = relationshipsMap?.mapNotNull { (key, value) ->
                        val caregiverId = key as? String ?: return@mapNotNull null
                        val relMap = value as? Map<*, *> ?: return@mapNotNull null
                        caregiverId to CaregiverRelationship(
                            relationship = relMap["relationship"] as? String ?: "",
                            nickname = relMap["nickname"] as? String ?: "",
                            linkedAt = relMap["linkedAt"] as? Long ?: System.currentTimeMillis(),
                            status = relMap["status"] as? String ?: "active",
                            message = relMap["message"] as? String ?: ""
                        )
                    }?.toMap() ?: emptyMap()
                    
                    Senior(
                        id = doc.getString("id") ?: "",
                        name = doc.getString("name") ?: "",
                        age = (doc.getLong("age") ?: 0).toInt(),
                        gender = doc.getString("gender") ?: "",
                        avatarType = doc.getString("avatarType") ?: "",
                        caregiverIds = (doc.get("caregiverIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        caregiverRelationships = caregiverRelationships,
                        creatorId = doc.getString("creatorId") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        password = doc.getString("password") ?: "", // 读取密码
                        healthHistory = HealthHistory(
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
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(seniors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getSeniorById(seniorId: String): Result<Senior> {
        return try {
            val doc = seniorsCollection.document(seniorId).get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Senior not found"))
            }
            
            val healthHistoryMap = doc.get("healthHistory") as? Map<*, *>
            val bloodPressureMap = healthHistoryMap?.get("bloodPressure") as? Map<*, *>
            
            // 读取 caregiverRelationships Map
            val relationshipsMap = doc.get("caregiverRelationships") as? Map<*, *>
            val caregiverRelationships = relationshipsMap?.mapNotNull { (key, value) ->
                val caregiverId = key as? String ?: return@mapNotNull null
                val relMap = value as? Map<*, *> ?: return@mapNotNull null
                caregiverId to CaregiverRelationship(
                    relationship = relMap["relationship"] as? String ?: "",
                    nickname = relMap["nickname"] as? String ?: "",
                    linkedAt = relMap["linkedAt"] as? Long ?: System.currentTimeMillis(),
                    status = relMap["status"] as? String ?: "active",
                            message = relMap["message"] as? String ?: ""
                )
            }?.toMap() ?: emptyMap()
            
            val senior = Senior(
                id = doc.getString("id") ?: "",
                name = doc.getString("name") ?: "",
                age = (doc.getLong("age") ?: 0).toInt(),
                gender = doc.getString("gender") ?: "",
                avatarType = doc.getString("avatarType") ?: "",
                caregiverIds = (doc.get("caregiverIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                caregiverRelationships = caregiverRelationships,
                creatorId = doc.getString("creatorId") ?: "",
                createdAt = doc.getLong("createdAt") ?: 0L,
                password = doc.getString("password") ?: "", // 读取密码
                healthHistory = HealthHistory(
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
            )
            
            Result.success(senior)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateSenior(senior: Senior): Result<Unit> {
        return try {
            // 转换 caregiverRelationships Map 为 Firestore 格式
            val relationshipsMap = senior.caregiverRelationships.mapValues { (_, relationship) ->
                hashMapOf(
                    "relationship" to relationship.relationship,
                    "nickname" to relationship.nickname,
                    "linkedAt" to relationship.linkedAt,
                    "status" to relationship.status,
                    "message" to relationship.message
                )
            }
            
            val seniorData = hashMapOf(
                "name" to senior.name,
                "age" to senior.age,
                "gender" to senior.gender,
                "avatarType" to senior.avatarType,
                "caregiverIds" to senior.caregiverIds,
                "caregiverRelationships" to relationshipsMap,
                "password" to senior.password, // 更新密码
                "healthHistory" to hashMapOf(
                    "bloodPressure" to senior.healthHistory.bloodPressure?.let {
                        hashMapOf(
                            "systolic" to it.systolic,
                            "diastolic" to it.diastolic,
                            "recordedAt" to it.recordedAt
                        )
                    },
                    "heartRate" to senior.healthHistory.heartRate,
                    "bloodSugar" to senior.healthHistory.bloodSugar,
                    "medicalConditions" to senior.healthHistory.medicalConditions,
                    "medications" to senior.healthHistory.medications,
                    "allergies" to senior.healthHistory.allergies
                )
            )
            
            seniorsCollection.document(senior.id).update(seniorData as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteSenior(seniorId: String): Result<Unit> {
        return try {
            // Step 1: 调用 Cloud Function 删除 Firebase Auth 账户�?users 集合中的文档
            val data = hashMapOf(
                "seniorId" to seniorId
            )
            
            functions
                .getHttpsCallable("deleteSeniorAccount")
                .call(data)
                .await()
            
            // Step 2: 删除 Firestore seniors 集合中的文档
            seniorsCollection.document(seniorId).delete().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getPendingLinkRequests(creatorId: String): Result<List<Senior>> {
        return try {
            // 获取创建者创建的所有老人账户
            val snapshot = seniorsCollection
                .whereEqualTo("creatorId", creatorId)
                .get()
                .await()
            
            val seniors = snapshot.documents.mapNotNull { doc ->
                try {
                    val healthHistoryMap = doc.get("healthHistory") as? Map<*, *>
                    val bloodPressureMap = healthHistoryMap?.get("bloodPressure") as? Map<*, *>
                    
                    // 读取 caregiverRelationships Map
                    val relationshipsMap = doc.get("caregiverRelationships") as? Map<*, *>
                    val caregiverRelationships = relationshipsMap?.mapNotNull { (key, value) ->
                        val caregiverId = key as? String ?: return@mapNotNull null
                        val relMap = value as? Map<*, *> ?: return@mapNotNull null
                        caregiverId to CaregiverRelationship(
                            relationship = relMap["relationship"] as? String ?: "",
                            nickname = relMap["nickname"] as? String ?: "",
                            linkedAt = relMap["linkedAt"] as? Long ?: System.currentTimeMillis(),
                            status = relMap["status"] as? String ?: "active",
                            message = relMap["message"] as? String ?: ""
                        )
                    }?.toMap() ?: emptyMap()
                    
                    // 只返回有 pending 状态请求的老人
                    if (caregiverRelationships.any { it.value.status == "pending" }) {
                        Senior(
                            id = doc.getString("id") ?: "",
                            name = doc.getString("name") ?: "",
                            age = (doc.getLong("age") ?: 0).toInt(),
                            gender = doc.getString("gender") ?: "",
                            avatarType = doc.getString("avatarType") ?: "",
                            caregiverIds = (doc.get("caregiverIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            caregiverRelationships = caregiverRelationships,
                            creatorId = doc.getString("creatorId") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            password = doc.getString("password") ?: "",
                            healthHistory = HealthHistory(
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
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(seniors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getSeniorsWithPendingByCaregiver(caregiverId: String): Result<List<Senior>> {
        return try {
            // 由于Firestore不支持直接查询Map的key，我们需要获取所有seniors然后过滤
            // 更高效的方法是维护一个反向索引，但这里为了简单起见先用这个方法
            val snapshot = seniorsCollection.get().await()
            
            val seniors = snapshot.documents.mapNotNull { doc ->
                try {
                    val healthHistoryMap = doc.get("healthHistory") as? Map<*, *>
                    val bloodPressureMap = healthHistoryMap?.get("bloodPressure") as? Map<*, *>
                    
                    // 读取 caregiverRelationships Map
                    val relationshipsMap = doc.get("caregiverRelationships") as? Map<*, *>
                    val caregiverRelationships = relationshipsMap?.mapNotNull { (key, value) ->
                        val cId = key as? String ?: return@mapNotNull null
                        val relMap = value as? Map<*, *> ?: return@mapNotNull null
                        cId to CaregiverRelationship(
                            relationship = relMap["relationship"] as? String ?: "",
                            nickname = relMap["nickname"] as? String ?: "",
                            linkedAt = relMap["linkedAt"] as? Long ?: System.currentTimeMillis(),
                            status = relMap["status"] as? String ?: "active",
                            message = relMap["message"] as? String ?: ""
                        )
                    }?.toMap() ?: emptyMap()
                    
                    // 只返回与该caregiver相关的老人（在caregiverRelationships中有记录）
                    if (caregiverRelationships.containsKey(caregiverId)) {
                        Senior(
                            id = doc.getString("id") ?: "",
                            name = doc.getString("name") ?: "",
                            age = (doc.getLong("age") ?: 0).toInt(),
                            gender = doc.getString("gender") ?: "",
                            avatarType = doc.getString("avatarType") ?: "",
                            caregiverIds = (doc.get("caregiverIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            caregiverRelationships = caregiverRelationships,
                            creatorId = doc.getString("creatorId") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            password = doc.getString("password") ?: "",
                            healthHistory = HealthHistory(
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
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(seniors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getPendingRequestsByCaregiver(caregiverId: String): Result<List<Senior>> {
        // 这个方法现在已经不需要了，因为我们使用独立的 linkRequests collection
        // 返回空列表即可
        return Result.success(emptyList())
    }
}
