package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.model.Senior
import com.alvin.pulselink.domain.repository.SeniorRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 创建老人账户用例
 * 
 * 流程:
 * 1. 调用 Cloud Function 在 Firebase Auth 中创建账户
 * 2. 在 Firestore 的 seniors 集合中创建老人信息
 */
class CreateSeniorUseCase @Inject constructor(
    private val seniorRepository: SeniorRepository,
    private val functions: FirebaseFunctions
) {
    /**
     * 创建老人账户
     * 
     * @param senior 老人信息（id 将由 Cloud Function 验证，可以预先生成或留空）
     * @param customPassword 自定义密码（可选，不提供则自动生成）
     * @return Result<SeniorAccountResult> 包含创建的老人信息、邮箱、密码等
     */
    suspend operator fun invoke(
        senior: Senior,
        customPassword: String? = null
    ): Result<SeniorAccountResult> {
        // 验证输入
        if (senior.name.isBlank()) {
            return Result.failure(Exception("Senior name cannot be empty"))
        }
        
        if (senior.age <= 0 || senior.age > 150) {
            return Result.failure(Exception("Invalid age"))
        }
        
        // 至少需要一个创建者或caregiver
        if (senior.creatorId.isBlank() && senior.caregiverIds.isEmpty()) {
            return Result.failure(Exception("Creator ID or caregiver list is required"))
        }

        return try {
            // Step 1: 先创建 Firestore 中的 senior 文档（获取自动生成的 ID）
            val createdSenior = seniorRepository.createSenior(senior).getOrThrow()
            
            // Step 2: 调用 Cloud Function 创建 Firebase Auth 账户
            val data = hashMapOf(
                "seniorId" to createdSenior.id,
                "name" to createdSenior.name,
                "password" to (customPassword ?: "")
            )
            
            val result = functions
                .getHttpsCallable("createSeniorAccount")
                .call(data)
                .await()
            
            val response = result.getData() as? Map<*, *>
                ?: return Result.failure(Exception("Invalid response from cloud function"))
            
            if (response["success"] != true) {
                return Result.failure(Exception("Failed to create Firebase Auth account"))
            }
            
            val generatedPassword = response["password"] as? String ?: ""
            
            // Step 3: 更新 Firestore 中的 senior 文档，存储密码
            val seniorWithPassword = createdSenior.copy(password = generatedPassword)
            seniorRepository.updateSenior(seniorWithPassword).getOrThrow()
            
            // Step 4: 返回完整结果
            Result.success(
                SeniorAccountResult(
                    senior = seniorWithPassword,
                    email = response["email"] as? String ?: "",
                    password = generatedPassword,
                    uid = response["uid"] as? String ?: "",
                    qrCodeData = generateQRCodeData(
                        seniorId = createdSenior.id,
                        password = generatedPassword
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 生成二维码数据（JSON 字符串）
     * 
     * 注意：现在二维码数据中存储的是 seniorId，而不是完整邮箱
     */
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
 * 创建老人账户的结果
 */
data class SeniorAccountResult(
    val senior: Senior,
    val email: String,
    val password: String,
    val uid: String,
    val qrCodeData: String
)
