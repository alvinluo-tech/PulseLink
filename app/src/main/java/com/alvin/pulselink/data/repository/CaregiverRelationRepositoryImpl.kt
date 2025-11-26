package com.alvin.pulselink.data.repository

import android.util.Log
import com.alvin.pulselink.domain.model.CaregiverRelation
import com.alvin.pulselink.domain.model.SeniorProfile
import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CaregiverRelationRepo"
private const val COLLECTION_RELATIONS = "caregiver_relations"
private const val COLLECTION_PROFILES = "senior_profiles"

@Singleton
class CaregiverRelationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CaregiverRelationRepository {
    
    private val relationsCollection = firestore.collection(COLLECTION_RELATIONS)
    private val profilesCollection = firestore.collection(COLLECTION_PROFILES)
    
    // ========== 数据解析 ==========
    
    private fun DocumentSnapshot.toCaregiverRelation(): CaregiverRelation? {
        return try {
            CaregiverRelation(
                id = getString("id") ?: "",
                caregiverId = getString("caregiverId") ?: "",
                seniorId = getString("seniorId") ?: "",
                relationship = getString("relationship") ?: "",
                nickname = getString("nickname") ?: "",
                caregiverName = getString("caregiverName") ?: "",
                status = getString("status") ?: CaregiverRelation.STATUS_PENDING,
                createdAt = getLong("createdAt") ?: 0L,
                approvedAt = getLong("approvedAt"),
                approvedBy = getString("approvedBy"),
                rejectedAt = getLong("rejectedAt"),
                rejectedBy = getString("rejectedBy"),
                message = getString("message") ?: "",
                canViewHealthData = getBoolean("canViewHealthData") ?: true,
                canEditHealthData = getBoolean("canEditHealthData") ?: false,
                canViewReminders = getBoolean("canViewReminders") ?: true,
                canEditReminders = getBoolean("canEditReminders") ?: true,
                canApproveRequests = getBoolean("canApproveRequests") ?: false,
                virtualAccountPassword = getString("virtualAccountPassword")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse relation ${id}", e)
            null
        }
    }
    
    private fun CaregiverRelation.toFirestoreMap(): Map<String, Any?> {
        return hashMapOf(
            "id" to id,
            "caregiverId" to caregiverId,
            "seniorId" to seniorId,
            "relationship" to relationship,
            "nickname" to nickname,
            "caregiverName" to caregiverName,
            "status" to status,
            "createdAt" to createdAt,
            "approvedAt" to approvedAt,
            "approvedBy" to approvedBy,
            "rejectedAt" to rejectedAt,
            "rejectedBy" to rejectedBy,
            "message" to message,
            "canViewHealthData" to canViewHealthData,
            "canEditHealthData" to canEditHealthData,
            "canViewReminders" to canViewReminders,
            "canEditReminders" to canEditReminders,
            "canApproveRequests" to canApproveRequests,
            "virtualAccountPassword" to virtualAccountPassword
        )
    }
    
    private fun DocumentSnapshot.toSeniorProfile(): SeniorProfile? {
        return try {
            SeniorProfile(
                id = getString("id") ?: "",
                userId = getString("userId") ?: "",
                name = getString("name") ?: "",
                age = (getLong("age") ?: 0).toInt(),
                gender = getString("gender") ?: "",
                avatarType = getString("avatarType") ?: "",
                creatorId = getString("creatorId") ?: "",
                createdAt = getLong("createdAt") ?: 0L,
                registrationType = getString("registrationType") ?: SeniorProfile.REGISTRATION_CAREGIVER
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse profile ${id}", e)
            null
        }
    }
    
    // ========== 查询方法 ==========
    
    override suspend fun getRelationsByCaregiver(caregiverId: String): Result<List<CaregiverRelation>> {
        return try {
            Log.d(TAG, "getRelationsByCaregiver - caregiverId: $caregiverId")
            
            val snapshot = relationsCollection
                .whereEqualTo("caregiverId", caregiverId)
                .get().await()
            
            val relations = snapshot.documents.mapNotNull { it.toCaregiverRelation() }
            Log.d(TAG, "Found ${relations.size} relations")
            
            Result.success(relations)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get relations by caregiver", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getActiveRelationsByCaregiver(caregiverId: String): Result<List<CaregiverRelation>> {
        return try {
            val snapshot = relationsCollection
                .whereEqualTo("caregiverId", caregiverId)
                .whereEqualTo("status", CaregiverRelation.STATUS_ACTIVE)
                .get().await()
            
            val relations = snapshot.documents.mapNotNull { it.toCaregiverRelation() }
            Result.success(relations)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active relations by caregiver", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getRelationsBySenior(seniorProfileId: String): Result<List<CaregiverRelation>> {
        return try {
            val snapshot = relationsCollection
                .whereEqualTo("seniorId", seniorProfileId)
                .get().await()
            
            val relations = snapshot.documents.mapNotNull { it.toCaregiverRelation() }
            Result.success(relations)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get relations by senior", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getActiveRelationsBySenior(seniorProfileId: String): Result<List<CaregiverRelation>> {
        return try {
            val snapshot = relationsCollection
                .whereEqualTo("seniorId", seniorProfileId)
                .whereEqualTo("status", CaregiverRelation.STATUS_ACTIVE)
                .get().await()
            
            val relations = snapshot.documents.mapNotNull { it.toCaregiverRelation() }
            Result.success(relations)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active relations by senior", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getPendingRelationsBySenior(seniorProfileId: String): Result<List<CaregiverRelation>> {
        return try {
            val snapshot = relationsCollection
                .whereEqualTo("seniorId", seniorProfileId)
                .whereEqualTo("status", CaregiverRelation.STATUS_PENDING)
                .get().await()
            
            val relations = snapshot.documents.mapNotNull { it.toCaregiverRelation() }
            Result.success(relations)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get pending relations by senior", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getRelation(caregiverId: String, seniorProfileId: String): Result<CaregiverRelation?> {
        return try {
            val relationId = CaregiverRelation.generateId(caregiverId, seniorProfileId)
            val doc = relationsCollection.document(relationId).get().await()
            
            val relation = if (doc.exists()) doc.toCaregiverRelation() else null
            Result.success(relation)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get relation", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getRelationById(relationId: String): Result<CaregiverRelation> {
        return try {
            val doc = relationsCollection.document(relationId).get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Relation not found: $relationId"))
            }
            
            val relation = doc.toCaregiverRelation()
                ?: return Result.failure(Exception("Failed to parse relation"))
            
            Result.success(relation)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get relation by id", e)
            Result.failure(e)
        }
    }
    
    override suspend fun hasActiveRelation(caregiverId: String, seniorProfileId: String): Result<Boolean> {
        return try {
            val relationId = CaregiverRelation.generateId(caregiverId, seniorProfileId)
            val doc = relationsCollection.document(relationId).get().await()
            
            if (!doc.exists()) {
                return Result.success(false)
            }
            
            val status = doc.getString("status")
            Result.success(status == CaregiverRelation.STATUS_ACTIVE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check active relation", e)
            Result.failure(e)
        }
    }
    
    // ========== 创建和更新方法 ==========
    
    override suspend fun createRelation(relation: CaregiverRelation): Result<CaregiverRelation> {
        return try {
            val relationId = relation.id.ifEmpty { 
                CaregiverRelation.generateId(relation.caregiverId, relation.seniorId)
            }
            val relationWithId = relation.copy(id = relationId)
            
            relationsCollection.document(relationId).set(relationWithId.toFirestoreMap()).await()
            Log.d(TAG, "Created relation: $relationId")
            
            Result.success(relationWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create relation", e)
            Result.failure(e)
        }
    }
    
    override suspend fun approveRelation(relationId: String, approvedBy: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            relationsCollection.document(relationId).update(
                mapOf(
                    "status" to CaregiverRelation.STATUS_ACTIVE,
                    "approvedAt" to now,
                    "approvedBy" to approvedBy
                )
            ).await()
            
            Log.d(TAG, "Approved relation: $relationId by $approvedBy")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to approve relation", e)
            Result.failure(e)
        }
    }
    
    override suspend fun rejectRelation(relationId: String, rejectedBy: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            relationsCollection.document(relationId).update(
                mapOf(
                    "status" to CaregiverRelation.STATUS_REJECTED,
                    "rejectedAt" to now,
                    "rejectedBy" to rejectedBy
                )
            ).await()
            
            Log.d(TAG, "Rejected relation: $relationId by $rejectedBy")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reject relation", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updatePermissions(
        relationId: String,
        canViewHealthData: Boolean,
        canEditHealthData: Boolean,
        canViewReminders: Boolean,
        canEditReminders: Boolean,
        canApproveRequests: Boolean
    ): Result<Unit> {
        return try {
            relationsCollection.document(relationId).update(
                mapOf(
                    "canViewHealthData" to canViewHealthData,
                    "canEditHealthData" to canEditHealthData,
                    "canViewReminders" to canViewReminders,
                    "canEditReminders" to canEditReminders,
                    "canApproveRequests" to canApproveRequests
                )
            ).await()
            
            Log.d(TAG, "Updated permissions for relation: $relationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update permissions", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateRelationInfo(
        relationId: String,
        relationship: String?,
        nickname: String?
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()
            relationship?.let { updates["relationship"] = it }
            nickname?.let { updates["nickname"] = it }
            
            if (updates.isNotEmpty()) {
                relationsCollection.document(relationId).update(updates).await()
                Log.d(TAG, "Updated relation info: $relationId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update relation info", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteRelation(relationId: String): Result<Unit> {
        return try {
            relationsCollection.document(relationId).delete().await()
            Log.d(TAG, "Deleted relation: $relationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete relation", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteRelation(caregiverId: String, seniorProfileId: String): Result<Unit> {
        val relationId = CaregiverRelation.generateId(caregiverId, seniorProfileId)
        return deleteRelation(relationId)
    }
    
    // ========== 批量查询方法 ==========
    
    override suspend fun getSeniorProfilesByRelations(relations: List<CaregiverRelation>): Result<List<SeniorProfile>> {
        return try {
            if (relations.isEmpty()) {
                return Result.success(emptyList())
            }
            
            val profileIds = relations.map { it.seniorId }.distinct()
            
            // Firestore whereIn 限制最多 30 个
            val profiles = mutableListOf<SeniorProfile>()
            profileIds.chunked(30).forEach { chunk ->
                val snapshot = profilesCollection
                    .whereIn("id", chunk)
                    .get().await()
                
                profiles.addAll(snapshot.documents.mapNotNull { it.toSeniorProfile() })
            }
            
            Result.success(profiles)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get profiles by relations", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getManagedSeniors(caregiverId: String): Result<List<Pair<SeniorProfile, CaregiverRelation>>> {
        return try {
            Log.d(TAG, "getManagedSeniors - caregiverId: $caregiverId")
            
            // 1. 获取所有活跃关系
            val relationsResult = getActiveRelationsByCaregiver(caregiverId)
            if (relationsResult.isFailure) {
                return Result.failure(relationsResult.exceptionOrNull()!!)
            }
            val relations = relationsResult.getOrNull() ?: emptyList()
            
            // 2. 获取创建的老人（创建者也能管理）
            val createdProfilesSnapshot = profilesCollection
                .whereEqualTo("creatorId", caregiverId)
                .get().await()
            val createdProfiles = createdProfilesSnapshot.documents.mapNotNull { it.toSeniorProfile() }
            
            // 3. 获取关系对应的老人档案
            val profilesResult = getSeniorProfilesByRelations(relations)
            if (profilesResult.isFailure) {
                return Result.failure(profilesResult.exceptionOrNull()!!)
            }
            val relationProfiles = profilesResult.getOrNull() ?: emptyList()
            
            // 4. 合并结果
            val result = mutableListOf<Pair<SeniorProfile, CaregiverRelation>>()
            
            // 添加通过关系管理的老人
            relations.forEach { relation ->
                val profile = relationProfiles.find { it.id == relation.seniorId }
                if (profile != null) {
                    result.add(Pair(profile, relation))
                }
            }
            
            // 添加创建的老人（如果没有关系，创建一个虚拟的创建者关系）
            createdProfiles.forEach { profile ->
                if (result.none { it.first.id == profile.id }) {
                    val creatorRelation = CaregiverRelation(
                        id = CaregiverRelation.generateId(caregiverId, profile.id),
                        caregiverId = caregiverId,
                        seniorId = profile.id,
                        relationship = "Creator",
                        caregiverName = "",
                        status = CaregiverRelation.STATUS_ACTIVE,
                        createdAt = profile.createdAt,
                        canViewHealthData = true,
                        canEditHealthData = true,
                        canViewReminders = true,
                        canEditReminders = true,
                        canApproveRequests = true
                    )
                    result.add(Pair(profile, creatorRelation))
                }
            }
            
            Log.d(TAG, "getManagedSeniors - Found ${result.size} seniors")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get managed seniors", e)
            Result.failure(e)
        }
    }
}
