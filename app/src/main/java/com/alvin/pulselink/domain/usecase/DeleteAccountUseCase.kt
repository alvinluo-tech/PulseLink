package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * 删除账户用例
 */
class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.deleteAccount()
    }
}
