package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.model.HealthData
import com.alvin.pulselink.domain.repository.HealthRepository
import javax.inject.Inject

class SaveHealthDataUseCase @Inject constructor(
    private val healthRepository: HealthRepository
) {
    suspend operator fun invoke(
        systolic: Int,
        diastolic: Int,
        heartRate: Int
    ): Result<Unit> {
        // Validate input
        if (systolic <= 0 || systolic > 300) {
            return Result.failure(Exception("收缩压应在 1-300 之间"))
        }
        
        if (diastolic <= 0 || diastolic > 200) {
            return Result.failure(Exception("舒张压应在 1-200 之间"))
        }
        
        if (heartRate <= 0 || heartRate > 250) {
            return Result.failure(Exception("心率应在 1-250 之间"))
        }
        
        // Additional validation: systolic should be greater than diastolic
        if (systolic <= diastolic) {
            return Result.failure(Exception("收缩压应大于舒张压"))
        }
        
        val healthData = HealthData(
            systolic = systolic,
            diastolic = diastolic,
            heartRate = heartRate,
            timestamp = System.currentTimeMillis()
        )
        
        return healthRepository.saveHealthData(healthData)
    }
}
