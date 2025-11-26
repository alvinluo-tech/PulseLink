package com.alvin.pulselink.data.repository

import android.util.Log
import com.alvin.pulselink.domain.model.SeniorProfile
import com.alvin.pulselink.domain.repository.SeniorProfileRepository
import com.alvin.pulselink.util.SnrIdGenerator
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SeniorProfileRepo"
private const val COLLECTION_PROFILES = "senior_profiles"
private const val COLLECTION_USERS = "users"
private const val COLLECTION_PASSWORDS = "senior_passwords"  // 密码单独存储

@Singleton
class SeniorProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : SeniorProfileRepository {
    
    private val profilesCollection = firestore.collection(COLLECTION_PROFILES)
    private val usersCollection = firestore.collection(COLLECTION_USERS)
    private val passwordsCollection = firestore.collection(COLLECTION_PASSWORDS)
    
    // ========== 数据解析 ==========
    
    private fun DocumentSnapshot.toSeniorProfile(): SeniorProfile? {
        return try {
            SeniorProfile(
                id = getString("id") ?: "",
                userId = getString("userId"),  // 可为 null
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
    
    private fun SeniorProfile.toFirestoreMap(): Map<String, Any?> {
        return hashMapOf(
            "id" to id,
            "userId" to userId,
            "name" to name,
            "age" to age,
            "gender" to gender,
            "avatarType" to avatarType,
            "creatorId" to creatorId,
            "createdAt" to createdAt,
            "registrationType" to registrationType
        )
    }
    
    // ========== Repository 实现 ==========
    
    override suspend fun createProfile(profile: SeniorProfile, password: String?): Result<SeniorProfile> {
        return try {
            val profileId = profile.id.ifEmpty { SnrIdGenerator.generate() }
            val profileWithId = profile.copy(id = profileId)
            
            // 创建档案
            profilesCollection.document(profileId).set(profileWithId.toFirestoreMap()).await()
            
            // 单独存储密码（加密或哈希处理应在这里）
            // 如果密码为空，则不存储（Cloud Function 会生成）
            if (!password.isNullOrBlank()) {
                passwordsCollection.document(profileId).set(
                    hashMapOf(
                        "profileId" to profileId,
                        "password" to password,  // TODO: 应该加密存储
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            }
            
            Log.d(TAG, "Created profile: $profileId")
            Result.success(profileWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create profile", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getProfileById(profileId: String): Result<SeniorProfile> {
        return try {
            val doc = profilesCollection.document(profileId).get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Profile not found: $profileId"))
            }
            
            val profile = doc.toSeniorProfile()
                ?: return Result.failure(Exception("Failed to parse profile"))
            
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get profile by id: $profileId", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getProfileByUserId(userId: String): Result<SeniorProfile?> {
        return try {
            val snapshot = profilesCollection
                .whereEqualTo("userId", userId)
                .limit(1)
                .get().await()
            
            val profile = snapshot.documents.firstOrNull()?.toSeniorProfile()
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get profile by userId: $userId", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getProfilesByCreator(creatorId: String): Result<List<SeniorProfile>> {
        return try {
            Log.d(TAG, "getProfilesByCreator - creatorId: $creatorId")
            
            val snapshot = profilesCollection
                .whereEqualTo("creatorId", creatorId)
                .get().await()
            
            val profiles = snapshot.documents.mapNotNull { it.toSeniorProfile() }
            Log.d(TAG, "getProfilesByCreator - Found ${profiles.size} profiles")
            
            Result.success(profiles)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get profiles by creator", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateProfile(profile: SeniorProfile): Result<Unit> {
        return try {
            val updateData = profile.toFirestoreMap().filterKeys { 
                it != "id" && it != "creatorId" && it != "createdAt" && it != "registrationType" 
            }
            
            profilesCollection.document(profile.id).update(updateData).await()
            Log.d(TAG, "Updated profile: ${profile.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile: ${profile.id}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteProfile(profileId: String): Result<Unit> {
        return try {
            // 只删除 Firestore 文档，Cloud Function 由 UseCase 层调用
            // 删除档案
            profilesCollection.document(profileId).delete().await()
            
            Log.d(TAG, "Deleted profile: $profileId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete profile: $profileId", e)
            Result.failure(e)
        }
    }
    
    override suspend fun bindUserId(profileId: String, userId: String): Result<Unit> {
        return try {
            profilesCollection.document(profileId).update("userId", userId).await()
            Log.d(TAG, "Bound userId $userId to profile $profileId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind userId", e)
            Result.failure(e)
        }
    }
    
    override suspend fun verifyPassword(profileId: String, password: String): Result<Boolean> {
        return try {
            val doc = passwordsCollection.document(profileId).get().await()
            
            if (!doc.exists()) {
                return Result.success(false)
            }
            
            val storedPassword = doc.getString("password") ?: ""
            Result.success(storedPassword == password)  // TODO: 应该使用哈希比较
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify password", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updatePassword(profileId: String, newPassword: String): Result<Unit> {
        return try {
            passwordsCollection.document(profileId).update(
                mapOf(
                    "password" to newPassword,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            
            Log.d(TAG, "Updated password for profile: $profileId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update password", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getAuthUid(profileId: String): Result<String?> {
        return try {
            val snapshot = usersCollection
                .whereEqualTo("seniorId", profileId)
                .limit(1)
                .get().await()
            
            val uid = snapshot.documents.firstOrNull()?.id
            Log.d(TAG, if (uid != null) "Found UID: $uid" else "No UID found for profile: $profileId")
            
            Result.success(uid)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get auth UID", e)
            Result.failure(e)
        }
    }
}
