package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.model.Senior
import com.alvin.pulselink.domain.repository.SeniorRepository
import javax.inject.Inject

/**
 * 获取caregiver管理的所有老人用例
 */
class GetSeniorsUseCase @Inject constructor(
    private val seniorRepository: SeniorRepository
) {
    suspend operator fun invoke(caregiverId: String): Result<List<Senior>> {
        if (caregiverId.isBlank()) {
            return Result.failure(Exception("Caregiver ID cannot be empty"))
        }
        
        return seniorRepository.getSeniorsByCaregiver(caregiverId)
    }
}
