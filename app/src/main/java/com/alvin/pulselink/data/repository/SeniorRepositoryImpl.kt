package com.alvin.pulselink.data.repository

import com.alvin.pulselink.domain.model.BloodPressureRecord
import com.alvin.pulselink.domain.model.HealthHistory
import com.alvin.pulselink.domain.model.Senior
import com.alvin.pulselink.domain.repository.SeniorRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeniorRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SeniorRepository {
    
    private val seniorsCollection = firestore.collection("seniors")
    
    override suspend fun createSenior(senior: Senior): Result<Senior> {
        return try {
            // 生成唯一ID
            val seniorId = senior.id.ifEmpty { 
                "SNR-${UUID.randomUUID().toString().substring(0, 8).uppercase()}" 
            }
            
            // 默认将创建者加入 caregiverIds，并记录 creatorId
            val creatorId = if (senior.creatorId.isNotBlank()) senior.creatorId else senior.caregiverIds.firstOrNull() ?: ""
            val caregiverIds = if (senior.caregiverIds.isNotEmpty()) senior.caregiverIds else listOfNotNull(creatorId)

            val seniorData = hashMapOf(
                "id" to seniorId,
                "name" to senior.name,
                "age" to senior.age,
                "gender" to senior.gender,
                "caregiverIds" to caregiverIds,
                "creatorId" to creatorId,
                "createdAt" to senior.createdAt,
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
            val snapshot = seniorsCollection
                .whereArrayContains("caregiverIds", caregiverId)
                .get()
                .await()
            
            val seniors = snapshot.documents.mapNotNull { doc ->
                try {
                    val healthHistoryMap = doc.get("healthHistory") as? Map<*, *>
                    val bloodPressureMap = healthHistoryMap?.get("bloodPressure") as? Map<*, *>
                    
                    Senior(
                        id = doc.getString("id") ?: "",
                        name = doc.getString("name") ?: "",
                        age = (doc.getLong("age") ?: 0).toInt(),
                        gender = doc.getString("gender") ?: "",
                        caregiverIds = (doc.get("caregiverIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        creatorId = doc.getString("creatorId") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
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
            val snapshot = seniorsCollection
                .whereEqualTo("creatorId", creatorId)
                .get()
                .await()
            
            val seniors = snapshot.documents.mapNotNull { doc ->
                try {
                    val healthHistoryMap = doc.get("healthHistory") as? Map<*, *>
                    val bloodPressureMap = healthHistoryMap?.get("bloodPressure") as? Map<*, *>
                    
                    Senior(
                        id = doc.getString("id") ?: "",
                        name = doc.getString("name") ?: "",
                        age = (doc.getLong("age") ?: 0).toInt(),
                        gender = doc.getString("gender") ?: "",
                        caregiverIds = (doc.get("caregiverIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        creatorId = doc.getString("creatorId") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
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
            
            val senior = Senior(
                id = doc.getString("id") ?: "",
                name = doc.getString("name") ?: "",
                age = (doc.getLong("age") ?: 0).toInt(),
                gender = doc.getString("gender") ?: "",
                caregiverIds = (doc.get("caregiverIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                creatorId = doc.getString("creatorId") ?: "",
                createdAt = doc.getLong("createdAt") ?: 0L,
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
            val seniorData = hashMapOf(
                "name" to senior.name,
                "age" to senior.age,
                "gender" to senior.gender,
                "caregiverIds" to senior.caregiverIds,
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
            seniorsCollection.document(seniorId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
