package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * 修改密码用例
 */
class ChangePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(newPassword: String): Result<Unit> {
        if (newPassword.isBlank()) {
            return Result.failure(Exception("Password cannot be empty"))
        }
        
        if (newPassword.length < 8) {
            return Result.failure(Exception("Password must be at least 8 characters"))
        }
        
        return authRepository.changePassword(newPassword)
    }
}
