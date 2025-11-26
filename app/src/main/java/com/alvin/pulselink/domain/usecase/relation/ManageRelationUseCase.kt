package com.alvin.pulselink.domain.usecase.relation

import com.alvin.pulselink.domain.model.CaregiverRelation
import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import javax.inject.Inject

/**
 * 管理 Caregiver-Senior 关系用例 (新架构)
 * 
 * 功能:
 * - 请求添加管理关系
 * - 批准/拒绝关系请求
 * - 更新权限
 * - 移除关系
 */
class ManageRelationUseCase @Inject constructor(
    private val caregiverRelationRepository: CaregiverRelationRepository
) {
    /**
     * 请求添加管理关系
     * 
     * @param caregiverId 请求者 (Caregiver) ID
     * @param seniorProfileId 目标老人资料 ID
     * @param requestedRelationship 请求的关系描述
     * @return 创建的关系（pending 状态）
     */
    suspend fun requestRelation(
        caregiverId: String,
        seniorProfileId: String,
        requestedRelationship: String = "CAREGIVER"
    ): Result<CaregiverRelation> {
        if (caregiverId.isBlank()) {
            return Result.failure(Exception("Caregiver ID 不能为空"))
        }
        
        if (seniorProfileId.isBlank()) {
            return Result.failure(Exception("老人资料 ID 不能为空"))
        }

        // 检查是否已存在关系
        val existingRelation = caregiverRelationRepository
            .getRelation(caregiverId, seniorProfileId)
            .getOrNull()
        
        if (existingRelation != null) {
            return when (existingRelation.status) {
                CaregiverRelation.STATUS_ACTIVE -> Result.failure(Exception("您已经是该老人的监护人"))
                CaregiverRelation.STATUS_PENDING -> Result.failure(Exception("您的申请正在等待审批"))
                CaregiverRelation.STATUS_REJECTED -> {
                    // 被拒绝的可以重新申请 - 需要先删除旧关系再创建新的
                    caregiverRelationRepository.deleteRelation(existingRelation.id)
                    createNewRelation(caregiverId, seniorProfileId, requestedRelationship)
                }
                else -> Result.failure(Exception("关系状态异常"))
            }
        }

        return createNewRelation(caregiverId, seniorProfileId, requestedRelationship)
    }

    private suspend fun createNewRelation(
        caregiverId: String,
        seniorProfileId: String,
        relationship: String
    ): Result<CaregiverRelation> {
        // 创建新的关系请求
        val relation = CaregiverRelation(
            id = CaregiverRelation.generateId(caregiverId, seniorProfileId),
            caregiverId = caregiverId,
            seniorId = seniorProfileId,
            status = CaregiverRelation.STATUS_PENDING,
            relationship = relationship,
            nickname = "",
            caregiverName = "",
            // 默认权限（待批准后生效）
            canViewHealthData = true,
            canEditHealthData = false,
            canViewReminders = true,
            canEditReminders = false,
            canApproveRequests = false,
            createdAt = System.currentTimeMillis(),
            approvedAt = null,
            approvedBy = null
        )

        return caregiverRelationRepository.createRelation(relation)
    }

    /**
     * 批准关系请求
     * 
     * @param relationId 关系 ID
     * @param approverId 批准者 ID (必须有 canApproveRequests 权限)
     * @return 是否成功
     */
    suspend fun approveRelation(
        relationId: String,
        approverId: String
    ): Result<Unit> {
        if (relationId.isBlank()) {
            return Result.failure(Exception("关系 ID 不能为空"))
        }

        return caregiverRelationRepository.approveRelation(relationId, approverId)
    }

    /**
     * 拒绝关系请求
     * 
     * @param relationId 关系 ID
     * @param rejecterId 拒绝者 ID
     * @return 是否成功
     */
    suspend fun rejectRelation(
        relationId: String,
        rejecterId: String
    ): Result<Unit> {
        if (relationId.isBlank()) {
            return Result.failure(Exception("关系 ID 不能为空"))
        }

        return caregiverRelationRepository.rejectRelation(relationId, rejecterId)
    }

    /**
     * 更新权限
     * 
     * @param relationId 关系 ID
     * @param updaterId 更新者 ID (必须有 canApproveRequests 权限)
     * @param permissions 要更新的权限
     * @return 是否成功
     */
    suspend fun updatePermissions(
        relationId: String,
        updaterId: String,
        permissions: RelationPermissions
    ): Result<Unit> {
        if (relationId.isBlank()) {
            return Result.failure(Exception("关系 ID 不能为空"))
        }

        return caregiverRelationRepository.updatePermissions(
            relationId = relationId,
            canViewHealthData = permissions.canViewHealthData,
            canEditHealthData = permissions.canEditHealthData,
            canViewReminders = permissions.canViewReminders,
            canEditReminders = permissions.canEditReminders,
            canApproveRequests = permissions.canApproveRequests
        )
    }

    /**
     * 移除关系
     * 
     * @param relationId 关系 ID
     * @param requesterId 请求者 ID
     * @return 是否成功
     */
    suspend fun removeRelation(
        relationId: String,
        requesterId: String
    ): Result<Unit> {
        if (relationId.isBlank()) {
            return Result.failure(Exception("关系 ID 不能为空"))
        }

        return caregiverRelationRepository.deleteRelation(relationId)
    }

    /**
     * 获取待处理的关系请求
     * 
     * @param seniorProfileId 老人资料 ID
     * @return 待处理的关系列表
     */
    suspend fun getPendingRequests(
        seniorProfileId: String
    ): Result<List<CaregiverRelation>> {
        if (seniorProfileId.isBlank()) {
            return Result.failure(Exception("老人资料 ID 不能为空"))
        }

        return caregiverRelationRepository.getPendingRelationsBySenior(seniorProfileId)
    }

    /**
     * 获取指定老人的所有 Caregiver
     * 
     * @param seniorProfileId 老人资料 ID
     * @return Caregiver 关系列表
     */
    suspend fun getCaregiversForSenior(
        seniorProfileId: String
    ): Result<List<CaregiverRelation>> {
        if (seniorProfileId.isBlank()) {
            return Result.failure(Exception("老人资料 ID 不能为空"))
        }

        return caregiverRelationRepository.getActiveRelationsBySenior(seniorProfileId)
    }
}

/**
 * 关系权限
 */
data class RelationPermissions(
    val canViewHealthData: Boolean = true,
    val canEditHealthData: Boolean = false,
    val canViewReminders: Boolean = true,
    val canEditReminders: Boolean = false,
    val canApproveRequests: Boolean = false
)
