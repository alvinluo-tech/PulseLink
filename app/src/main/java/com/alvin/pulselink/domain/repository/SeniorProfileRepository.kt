package com.alvin.pulselink.domain.repository

import com.alvin.pulselink.domain.model.SeniorProfile

/**
 * 老人档案数据仓库接口
 * 
 * 方案C重构：简化的老人档案管理
 * - 关系管理移至 CaregiverRelationRepository
 * - 健康数据移至 HealthRecordRepository
 */
interface SeniorProfileRepository {
    
    /**
     * 创建老人档案
     * @param profile 老人档案数据
     * @param password 登录密码（单独存储，不在 profile 中，可为 null 则自动生成）
     */
    suspend fun createProfile(profile: SeniorProfile, password: String?): Result<SeniorProfile>
    
    /**
     * 根据 ID 获取老人档案
     */
    suspend fun getProfileById(profileId: String): Result<SeniorProfile>
    
    /**
     * 根据 Firebase Auth UID 获取老人档案
     */
    suspend fun getProfileByUserId(userId: String): Result<SeniorProfile?>
    
    /**
     * 获取创建者创建的所有老人档案
     */
    suspend fun getProfilesByCreator(creatorId: String): Result<List<SeniorProfile>>
    
    /**
     * 更新老人档案
     */
    suspend fun updateProfile(profile: SeniorProfile): Result<Unit>
    
    /**
     * 删除老人档案
     */
    suspend fun deleteProfile(profileId: String): Result<Unit>
    
    /**
     * 绑定 Firebase Auth UID 到老人档案
     * 用于老人首次登录时绑定账户
     */
    suspend fun bindUserId(profileId: String, userId: String): Result<Unit>
    
    /**
     * 验证老人密码
     */
    suspend fun verifyPassword(profileId: String, password: String): Result<Boolean>
    
    /**
     * 更新老人密码
     */
    suspend fun updatePassword(profileId: String, newPassword: String): Result<Unit>
    
    /**
     * 获取老人的 Firebase Auth UID
     */
    suspend fun getAuthUid(profileId: String): Result<String?>
}
