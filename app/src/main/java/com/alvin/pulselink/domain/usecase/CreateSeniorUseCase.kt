package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.model.Senior
import com.alvin.pulselink.domain.repository.SeniorRepository
import javax.inject.Inject

/**
 * 创建老人账户用例
 */
class CreateSeniorUseCase @Inject constructor(
    private val seniorRepository: SeniorRepository
) {
    suspend operator fun invoke(senior: Senior): Result<Senior> {
        // 验证输入
        if (senior.name.isBlank()) {
            return Result.failure(Exception("Senior name cannot be empty"))
        }
        
        if (senior.age <= 0 || senior.age > 150) {
            return Result.failure(Exception("Invalid age"))
        }
        // 至少需要一个创建者或caregiver
        if (senior.creatorId.isBlank() && senior.caregiverIds.isEmpty()) {
            return Result.failure(Exception("Creator ID or caregiver list is required"))
        }
        
        return seniorRepository.createSenior(senior)
    }
}
