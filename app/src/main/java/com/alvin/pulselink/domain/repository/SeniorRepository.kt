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
}
