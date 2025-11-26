package com.alvin.pulselink.domain.usecase.profile

import android.util.Log
import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import com.alvin.pulselink.domain.repository.HealthRecordRepository
import com.alvin.pulselink.domain.repository.SeniorProfileRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 删除老人资料用例 (新架构)
 * 
 * 流程:
 * 1. 删除所有 health_records
 * 2. 删除所有 caregiver_relations
 * 3. 删除 senior_profile
 * 4. 调用 Cloud Function 删除 Firebase Auth 账户
 */
class DeleteSeniorProfileUseCase @Inject constructor(
    private val seniorProfileRepository: SeniorProfileRepository,
    private val caregiverRelationRepository: CaregiverRelationRepository,
    private val healthRecordRepository: HealthRecordRepository,
    private val functions: FirebaseFunctions
) {
    companion object {
        private const val TAG = "DeleteSeniorProfileUseCase"
    }

    /**
     * 删除老人资料及所有关联数据
     * 
     * @param seniorProfileId 老人资料 ID
     * @param requesterId 请求者 ID (必须是创建者或有权限的 Caregiver)
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
            
            // Step 1: 验证权限（必须是创建者）
            val profile = seniorProfileRepository.getProfileById(seniorProfileId).getOrNull()
            if (profile == null) {
                return Result.failure(Exception("老人资料不存在"))
            }
            
            if (profile.creatorId != requesterId) {
                // 检查是否有 canApproveRequests 权限
                val relation = caregiverRelationRepository
                    .getRelation(requesterId, seniorProfileId)
                    .getOrNull()
                
                if (relation == null || !relation.canApproveRequests) {
                    return Result.failure(Exception("没有删除权限"))
                }
            }
            
            var deletedHealthRecords = 0
            var deletedRelations = 0
            var deletedProfile = false
            var deletedAuth = false
            
            // Step 2: 删除所有健康记录
            // 这里简化处理，实际应该使用批量删除
            try {
                // TODO: 添加批量删除健康记录的方法
                deletedHealthRecords = 0 // 暂时跳过
                Log.d(TAG, "Health records deletion skipped (to be implemented)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete health records", e)
            }
            
            // Step 3: 删除所有关系
            try {
                // TODO: 添加批量删除关系的方法
                deletedRelations = 0 // 暂时跳过
                Log.d(TAG, "Relations deletion skipped (to be implemented)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete relations", e)
            }
            
            // Step 4: 删除 Profile
            seniorProfileRepository.deleteProfile(seniorProfileId).getOrThrow()
            deletedProfile = true
            Log.d(TAG, "Profile deleted")
            
            // Step 5: 调用 Cloud Function 删除 Firebase Auth 账户
            try {
                val data = hashMapOf("seniorId" to seniorProfileId)
                val result = functions
                    .getHttpsCallable("deleteSeniorAccount")
                    .call(data)
                    .await()
                
                val response = result.getData() as? Map<*, *>
                deletedAuth = response?.get("success") == true
                Log.d(TAG, "Auth account deleted: $deletedAuth")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete Auth account", e)
            }
            
            Result.success(
                DeleteResult(
                    seniorProfileId = seniorProfileId,
                    deletedProfile = deletedProfile,
                    deletedHealthRecords = deletedHealthRecords,
                    deletedRelations = deletedRelations,
                    deletedAuth = deletedAuth
                )
            )
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
