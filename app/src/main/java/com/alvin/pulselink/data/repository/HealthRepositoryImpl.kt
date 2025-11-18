package com.alvin.pulselink.data.repository

import com.alvin.pulselink.domain.model.HealthData
import com.alvin.pulselink.domain.repository.HealthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepositoryImpl @Inject constructor() : HealthRepository {
    
    // 模拟数据
    private val mockHealthData = HealthData(
        systolic = 120,
        diastolic = 80,
        timestamp = System.currentTimeMillis()
    )
    
    override suspend fun getLatestHealthData(): Result<HealthData?> {
        return try {
            Result.success(mockHealthData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getHealthHistory(): Result<List<HealthData>> {
        return try {
            // 返回模拟的历史数据
            val history = listOf(
                HealthData(120, 80, System.currentTimeMillis() - 86400000),
                HealthData(118, 78, System.currentTimeMillis() - 172800000),
                HealthData(122, 82, System.currentTimeMillis() - 259200000)
            )
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun saveHealthData(healthData: HealthData): Result<Unit> {
        return try {
            // 这里可以保存到数据库或远程服务器
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
