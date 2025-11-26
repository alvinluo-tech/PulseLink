package com.alvin.pulselink.domain.usecase.profile

import android.util.Log
import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import com.alvin.pulselink.domain.repository.HealthRecordRepository
import com.alvin.pulselink.domain.repository.SeniorProfileRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 删除老人资料用例 (新架构 - 原子性删除)
 * 
 * 所有删除操作都在 Cloud Function 中原子性执行：
 * 1. 验证权限（必须是创建者）
 * 2. 批量删除所有 health_records
 * 3. 批量删除所有 caregiver_relations
 * 4. 删除 senior_profile
 * 5. 删除 Firebase Auth 账户
 */
class DeleteSeniorProfileUseCase @Inject constructor(
    private val caregiverRelationRepository: CaregiverRelationRepository,
    private val seniorProfileRepository: SeniorProfileRepository,
    private val functions: FirebaseFunctions
) {
    companion object {
        private const val TAG = "DeleteSeniorProfileUseCase"
    }

    /**
     * 检查是否可以删除老人账号
     * 
     * @return DeletionCheckResult 检查结果
     */
    suspend fun checkDeletionAllowed(
        seniorProfileId: String,
        requesterId: String
    ): Result<DeletionCheckResult> {
        return try {
            // 1. 验证权限（必须是创建者）
            val profile = seniorProfileRepository.getProfileById(seniorProfileId).getOrNull()
            if (profile == null) {
                return Result.failure(Exception("老人资料不存在"))
            }
            
            if (profile.creatorId != requesterId) {
                return Result.failure(Exception("只有创建者才能删除账号"))
            }
            
            // 2. 查询所有关联关系
            val allRelations = caregiverRelationRepository
                .getRelationsBySenior(seniorProfileId)
                .getOrNull() ?: emptyList()
            
            // 3. 过滤出活跃的关系
            val activeRelations = allRelations.filter { it.isActive }
            
            // 4. 判断是否只有创建者自己
            val hasOtherCaregivers = activeRelations.any { it.caregiverId != requesterId }
            
            // 5. 获取其他护理者信息
            val otherCaregiversInfo = if (hasOtherCaregivers) {
                activeRelations
                    .filter { it.caregiverId != requesterId }
                    .map { 
                        val relationshipText = when (it.relationship) {
                            "Son" -> "儿子"
                            "Daughter" -> "女儿"
                            "Son-in-law" -> "女婿"
                            "Daughter-in-law" -> "儿媳"
                            else -> it.relationship
                        }
                        relationshipText
                    }
            } else {
                emptyList()
            }
            
            Result.success(
                DeletionCheckResult(
                    canDelete = !hasOtherCaregivers,
                    hasOtherCaregivers = hasOtherCaregivers,
                    totalActiveRelations = activeRelations.size,
                    otherCaregiversInfo = otherCaregiversInfo,
                    seniorName = profile.name
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check deletion allowed", e)
            Result.failure(e)
        }
    }
    
    /**
     * 删除老人资料及所有关联数据
     * 
     * 所有删除操作都在 Cloud Function 中原子性执行，Android 端只负责发起请求
     * 
     * @param seniorProfileId 老人资料 ID
     * @param requesterId 请求者 ID (必须是创建者)
     * @return 删除结果
     */
    suspend operator fun invoke(
        seniorProfileId: String,
        requesterId: String
    ): Result<DeleteResult> {
        if (seniorProfileId.isBlank()) {
            return Result.failure(Exception("老人资料 ID 不能为空"))
        }
        
        if (requesterId.isBlank()) {
            return Result.failure(Exception("请求者 ID 不能为空"))
        }

        return try {
            Log.d(TAG, "Deleting senior profile: $seniorProfileId by $requesterId")
            
            // 调用 Cloud Function 执行所有删除操作（原子性）
            val data = hashMapOf(
                "seniorId" to seniorProfileId,
                "requesterId" to requesterId
            )
            
            val result = functions
                .getHttpsCallable("deleteSeniorAccount")
                .call(data)
                .await()
            
            val response = result.getData() as? Map<*, *>
            
            if (response?.get("success") == true) {
                Log.d(TAG, "Successfully deleted senior account via Cloud Function")
                
                Result.success(
                    DeleteResult(
                        seniorProfileId = seniorProfileId,
                        deletedProfile = response["deletedProfile"] as? Boolean ?: false,
                        deletedHealthRecords = (response["deletedHealthRecords"] as? Number)?.toInt() ?: 0,
                        deletedRelations = (response["deletedRelations"] as? Number)?.toInt() ?: 0,
                        deletedAuth = response["deletedAuth"] as? Boolean ?: false
                    )
                )
            } else {
                Result.failure(Exception("删除失败：${response?.get("message")}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete senior profile", e)
            Result.failure(e)
        }
    }
}

/**
 * 删除结果
 */
data class DeleteResult(
    val seniorProfileId: String,
    val deletedProfile: Boolean,
    val deletedHealthRecords: Int,
    val deletedRelations: Int,
    val deletedAuth: Boolean
)

/**
 * 删除检查结果
 */
data class DeletionCheckResult(
    val canDelete: Boolean,              // 是否可以删除
    val hasOtherCaregivers: Boolean,     // 是否有其他护理者
    val totalActiveRelations: Int,       // 总共有多少个活跃关系
    val otherCaregiversInfo: List<String>, // 其他护理者的关系信息（如"女儿", "儿子"）
    val seniorName: String               // 老人姓名
)
