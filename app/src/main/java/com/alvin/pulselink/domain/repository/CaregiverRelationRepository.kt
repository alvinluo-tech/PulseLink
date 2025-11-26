package com.alvin.pulselink.domain.repository

import com.alvin.pulselink.domain.model.CaregiverRelation
import com.alvin.pulselink.domain.model.SeniorProfile

/**
 * 护理者关系数据仓库接口
 * 
 * 方案C重构：独立管理护理者与老人的关系
 * Collection: caregiver_relations/{relationId}
 */
interface CaregiverRelationRepository {
    
    // ========== 查询方法 ==========
    
    /**
     * 获取护理者的所有关系（包括 pending）
     */
    suspend fun getRelationsByCaregiver(caregiverId: String): Result<List<CaregiverRelation>>
    
    /**
     * 获取护理者的活跃关系
     */
    suspend fun getActiveRelationsByCaregiver(caregiverId: String): Result<List<CaregiverRelation>>
    
    /**
     * 获取老人的所有关系
     */
    suspend fun getRelationsBySenior(seniorProfileId: String): Result<List<CaregiverRelation>>
    
    /**
     * 获取老人的活跃护理者关系
     */
    suspend fun getActiveRelationsBySenior(seniorProfileId: String): Result<List<CaregiverRelation>>
    
    /**
     * 获取老人的待审核关系请求
     */
    suspend fun getPendingRelationsBySenior(seniorProfileId: String): Result<List<CaregiverRelation>>
    
    /**
     * 获取特定的护理者-老人关系
     */
    suspend fun getRelation(caregiverId: String, seniorProfileId: String): Result<CaregiverRelation?>
    
    /**
     * 根据 ID 获取关系
     */
    suspend fun getRelationById(relationId: String): Result<CaregiverRelation>
    
    /**
     * 检查护理者是否有权访问老人
     */
    suspend fun hasActiveRelation(caregiverId: String, seniorProfileId: String): Result<Boolean>
    
    // ========== 创建和更新方法 ==========
    
    /**
     * 创建关系请求
     * 状态默认为 pending
     */
    suspend fun createRelation(relation: CaregiverRelation): Result<CaregiverRelation>
    
    /**
     * 批准关系请求
     */
    suspend fun approveRelation(
        relationId: String, 
        approvedBy: String
    ): Result<Unit>
    
    /**
     * 拒绝关系请求
     */
    suspend fun rejectRelation(
        relationId: String, 
        rejectedBy: String
    ): Result<Unit>
    
    /**
     * 更新关系权限
     */
    suspend fun updatePermissions(
        relationId: String,
        canViewHealthData: Boolean,
        canEditHealthData: Boolean,
        canViewReminders: Boolean,
        canEditReminders: Boolean,
        canApproveRequests: Boolean
    ): Result<Unit>
    
    /**
     * 更新关系信息（昵称、称谓等）
     */
    suspend fun updateRelationInfo(
        relationId: String,
        relationship: String? = null,
        nickname: String? = null
    ): Result<Unit>
    
    /**
     * 删除关系
     */
    suspend fun deleteRelation(relationId: String): Result<Unit>
    
    /**
     * 删除护理者与老人之间的关系
     */
    suspend fun deleteRelation(caregiverId: String, seniorProfileId: String): Result<Unit>
    
    // ========== 批量查询方法（优化性能）==========
    
    /**
     * 根据关系列表获取老人档案
     * 优化：使用 whereIn 批量查询
     */
    suspend fun getSeniorProfilesByRelations(relations: List<CaregiverRelation>): Result<List<SeniorProfile>>
    
    /**
     * 获取护理者管理的所有老人档案（一步查询）
     * 优化后的核心方法，替代原来的 getSeniorsByCaregiver
     */
    suspend fun getManagedSeniors(caregiverId: String): Result<List<Pair<SeniorProfile, CaregiverRelation>>>
}
