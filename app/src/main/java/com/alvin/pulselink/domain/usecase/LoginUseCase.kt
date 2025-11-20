package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.model.UserRole
import com.alvin.pulselink.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<Unit> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Email and password cannot be empty"))
        }
        
        return authRepository.login(email, password)
    }
}
