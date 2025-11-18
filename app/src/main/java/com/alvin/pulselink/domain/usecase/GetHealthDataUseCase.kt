package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.model.HealthData
import com.alvin.pulselink.domain.repository.HealthRepository
import javax.inject.Inject

class GetHealthDataUseCase @Inject constructor(
    private val healthRepository: HealthRepository
) {
    suspend operator fun invoke(): Result<HealthData?> {
        return healthRepository.getLatestHealthData()
    }
}
