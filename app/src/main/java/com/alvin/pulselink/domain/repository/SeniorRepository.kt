package com.alvin.pulselink.domain.repository

import com.alvin.pulselink.domain.model.Senior

/**
 * 老人账户数据仓库接口
 */
interface SeniorRepository {
    /**
     * 创建老人账户
     */
    suspend fun createSenior(senior: Senior): Result<Senior>
    
    /**
     * 获取caregiver管理的所有老人
     */
    suspend fun getSeniorsByCaregiver(caregiverId: String): Result<List<Senior>>
    
    /**
     * 获取由某个caregiver创建的所有老人
     */
    suspend fun getSeniorsByCreator(creatorId: String): Result<List<Senior>>
    
    /**
     * 根据ID获取老人信息
     */
    suspend fun getSeniorById(seniorId: String): Result<Senior>
    
    /**
     * 更新老人信息
     */
    suspend fun updateSenior(senior: Senior): Result<Unit>
    
    /**
     * 删除老人账户
     */
    suspend fun deleteSenior(seniorId: String): Result<Unit>
    
    /**
     * 获取创建者待审核的链接请求
     * 返回所有包含 pending 状态 caregiverRelationships 的老人账户
     */
    suspend fun getPendingLinkRequests(creatorId: String): Result<List<Senior>>
    
    /**
     * 获取与用户相关的所有老人（包括pending状态的请求）
     * 查询条件：caregiverRelationships中包含该用户ID的所有老人
     */
    suspend fun getSeniorsWithPendingByCaregiver(caregiverId: String): Result<List<Senior>>
    
    /**
     * 获取用户发送的pending请求
     * 查询条件：pendingCaregiversIds数组中包含该用户ID的老人
     */
    suspend fun getPendingRequestsByCaregiver(caregiverId: String): Result<List<Senior>>
    
    /**
     * 根据 seniorId 获取对应的 Firebase Auth UID
     * 用于查询健康数据
     */
    suspend fun getSeniorAuthUid(seniorId: String): Result<String?>
}
