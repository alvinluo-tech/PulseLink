package com.alvin.pulselink.domain.repository

import com.alvin.pulselink.domain.model.HealthData

interface HealthRepository {
    suspend fun getLatestHealthData(): Result<HealthData?>
    suspend fun getHealthHistory(): Result<List<HealthData>>
    suspend fun saveHealthData(healthData: HealthData): Result<Unit>
    
    /**
     * 根据老人的 UID 获取最新的健康数据
     * 用于 Caregiver 查看老人的健康状态
     */
    suspend fun getLatestHealthDataBySeniorUid(seniorUid: String): Result<HealthData?>
    
    // 测试 Firestore 连接
    suspend fun testConnection(): Result<Unit>
}
