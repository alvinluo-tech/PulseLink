package com.alvin.pulselink.domain.usecase.profile

import android.util.Log
import com.alvin.pulselink.domain.model.CaregiverRelation
import com.alvin.pulselink.domain.model.SeniorProfile
import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import com.alvin.pulselink.domain.repository.SeniorProfileRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 创建老人资料用例 (新架构)
 * 
 * 流程:
 * 1. 创建 senior_profile 文档
 * 2. 调用 Cloud Function 创建 Firebase Auth 账户
 * 3. 创建 caregiver_relation 关系
 */
class CreateSeniorProfileUseCase @Inject constructor(
    private val seniorProfileRepository: SeniorProfileRepository,
    private val caregiverRelationRepository: CaregiverRelationRepository,
    private val functions: FirebaseFunctions
) {
    companion object {
        private const val TAG = "CreateSeniorProfileUseCase"
    }

    /**
     * 创建老人资料和账户
     * 
     * @param name 老人姓名
     * @param age 年龄
     * @param gender 性别
     * @param avatarType 头像类型
     * @param creatorId 创建者 ID (Caregiver)
     * @param customPassword 自定义密码（可选）
     * @param relationship Caregiver's relationship to senior (e.g., "Son", "Daughter")
     * @param nickname Caregiver's nickname for senior (e.g., "Dad", "Mom")
     * @return 创建结果
     */
    suspend operator fun invoke(
        name: String,
        age: Int,
        gender: String,
        avatarType: String = "GRANDFATHER",
        creatorId: String,
        customPassword: String? = null,
        relationship: String = "Son",
        nickname: String = ""
    ): Result<SeniorProfileResult> {
        // 验证输入
        if (name.isBlank()) {
            return Result.failure(Exception("姓名不能为空"))
        }
        
        if (age <= 0 || age > 150) {
            return Result.failure(Exception("年龄无效"))
        }
        
        if (creatorId.isBlank()) {
            return Result.failure(Exception("创建者 ID 不能为空"))
        }

        return try {
            Log.d(TAG, "Creating senior profile: name=$name, creatorId=$creatorId")
            
            // Step 1: 创建 SeniorProfile
            val profile = SeniorProfile(
                id = "", // 由 Repository 生成
                userId = null,
                name = name,
                age = age,
                gender = gender,
                avatarType = avatarType,
                creatorId = creatorId,
                createdAt = System.currentTimeMillis(),
                registrationType = "CAREGIVER_CREATED"
            )
            
            val createdProfile = seniorProfileRepository
                .createProfile(profile, customPassword)
                .getOrThrow()
            
            Log.d(TAG, "Profile created: id=${createdProfile.id}")
            
            // Step 2: 调用 Cloud Function 创建 Firebase Auth 账户
            val data = hashMapOf(
                "seniorId" to createdProfile.id,
                "name" to name,
                "password" to (customPassword ?: "")
            )
            
            Log.d(TAG, "Calling createSeniorAccount Cloud Function...")
            val result = functions
                .getHttpsCallable("createSeniorAccount")
                .call(data)
                .await()
            
            val response = result.getData() as? Map<*, *>
                ?: return Result.failure(Exception("Cloud Function 返回无效"))
            
            if (response["success"] != true) {
                return Result.failure(Exception("创建 Firebase Auth 账户失败"))
            }
            
            val generatedPassword = response["password"] as? String ?: ""
            val email = response["email"] as? String ?: ""
            val uid = response["uid"] as? String ?: ""
            
            Log.d(TAG, "Auth account created: email=$email")
            
            // Step 3: 创建 Caregiver-Senior 关系（自动批准，并存储密码）
            val relation = CaregiverRelation(
                id = CaregiverRelation.generateId(creatorId, createdProfile.id),
                caregiverId = creatorId,
                seniorId = createdProfile.id,
                status = CaregiverRelation.STATUS_ACTIVE,
                relationship = relationship,  // Use provided relationship
                nickname = nickname,  // Use provided nickname
                canViewHealthData = true,
                canEditHealthData = true,
                canViewReminders = true,
                canEditReminders = true,
                canApproveRequests = true,
                createdAt = System.currentTimeMillis(),
                approvedAt = System.currentTimeMillis(),
                approvedBy = creatorId,
                virtualAccountPassword = generatedPassword  // 存储密码在关系中
            )
            
            caregiverRelationRepository.createRelation(relation).getOrThrow()
            Log.d(TAG, "Relation created: ${relation.id}")
            
            // Step 4: 返回结果
            Result.success(
                SeniorProfileResult(
                    profile = createdProfile,
                    email = email,
                    password = generatedPassword,
                    uid = uid,
                    relation = relation,
                    qrCodeData = generateQRCodeData(createdProfile.id, generatedPassword)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create senior profile", e)
            Result.failure(e)
        }
    }
    
    private fun generateQRCodeData(seniorId: String, password: String): String {
        return """
            {
              "type": "pulselink_login",
              "id": "$seniorId",
              "password": "$password"
            }
        """.trimIndent()
    }
}

/**
 * 创建老人资料的结果
 */
data class SeniorProfileResult(
    val profile: SeniorProfile,
    val email: String,
    val password: String,
    val uid: String,
    val relation: CaregiverRelation,
    val qrCodeData: String
)
