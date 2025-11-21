package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.model.Senior
import com.alvin.pulselink.domain.repository.SeniorRepository
import javax.inject.Inject

/**
 * 获取由caregiver创建的所有老人列表用例
 */
class GetCreatedSeniorsUseCase @Inject constructor(
    private val seniorRepository: SeniorRepository
) {
    suspend operator fun invoke(creatorId: String): Result<List<Senior>> {
        if (creatorId.isBlank()) {
            return Result.failure(Exception("Creator ID cannot be empty"))
        }
        return seniorRepository.getSeniorsByCreator(creatorId)
    }
}