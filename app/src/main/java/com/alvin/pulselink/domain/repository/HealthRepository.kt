package com.alvin.pulselink.domain.repository

import com.alvin.pulselink.domain.model.HealthData

interface HealthRepository {
    suspend fun getLatestHealthData(): Result<HealthData?>
    suspend fun getHealthHistory(): Result<List<HealthData>>
    suspend fun saveHealthData(healthData: HealthData): Result<Unit>
}
