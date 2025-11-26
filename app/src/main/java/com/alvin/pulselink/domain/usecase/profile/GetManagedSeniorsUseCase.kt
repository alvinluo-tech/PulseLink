package com.alvin.pulselink.domain.usecase.profile

import com.alvin.pulselink.domain.model.CaregiverRelation
import com.alvin.pulselink.domain.model.SeniorProfile
import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import javax.inject.Inject

/**
 * 获取 Caregiver 管理的所有老人用例 (新架构)
 * 
 * 优化：
 * - 使用 CaregiverRelation 进行单次查询
 * - 自动过滤已批准的关系
 * - 支持分页
 */
class GetManagedSeniorsUseCase @Inject constructor(
    private val caregiverRelationRepository: CaregiverRelationRepository
) {
    /**
     * 获取 Caregiver 管理的所有老人（已批准状态）
     * 
     * @param caregiverId Caregiver 的用户 ID
     * @return 包含老人资料和关系信息的列表
     */
    suspend operator fun invoke(caregiverId: String): Result<List<ManagedSeniorInfo>> {
        if (caregiverId.isBlank()) {
            return Result.failure(Exception("Caregiver ID cannot be empty"))
        }

        return try {
            val managedSeniors = caregiverRelationRepository
                .getManagedSeniors(caregiverId)
                .getOrThrow()
            
            // 只返回已批准的关系
            val approvedSeniors = managedSeniors.filter { (_, relation) ->
                relation.status == CaregiverRelation.STATUS_ACTIVE
            }
            
            Result.success(approvedSeniors.map { (profile, relation) ->
                ManagedSeniorInfo(
                    profile = profile,
                    relation = relation,
                    canViewHealthData = relation.canViewHealthData,
                    canEditHealthData = relation.canEditHealthData,
                    canViewReminders = relation.canViewReminders,
                    canEditReminders = relation.canEditReminders
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 管理的老人信息（包含权限）
 */
data class ManagedSeniorInfo(
    val profile: SeniorProfile,
    val relation: CaregiverRelation,
    val canViewHealthData: Boolean,
    val canEditHealthData: Boolean,
    val canViewReminders: Boolean,
    val canEditReminders: Boolean
)
