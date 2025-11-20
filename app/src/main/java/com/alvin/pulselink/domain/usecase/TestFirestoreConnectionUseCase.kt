package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.repository.HealthRepository
import javax.inject.Inject

class TestFirestoreConnectionUseCase @Inject constructor(
    private val healthRepository: HealthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return healthRepository.testConnection()
    }
}
