package com.alvin.pulselink.domain.usecase.profile

import com.alvin.pulselink.domain.model.SeniorProfile
import com.alvin.pulselink.domain.repository.SeniorProfileRepository
import javax.inject.Inject

/**
 * 获取由 Caregiver 创建的所有老人资料用例 (新架构)
 */
class GetCreatedProfilesUseCase @Inject constructor(
    private val seniorProfileRepository: SeniorProfileRepository
) {
    /**
     * 获取由指定用户创建的所有老人资料
     * 
     * @param creatorId 创建者的用户 ID
     * @return 老人资料列表
     */
    suspend operator fun invoke(creatorId: String): Result<List<SeniorProfile>> {
        if (creatorId.isBlank()) {
            return Result.failure(Exception("Creator ID cannot be empty"))
        }

        return seniorProfileRepository.getProfilesByCreator(creatorId)
    }
}
