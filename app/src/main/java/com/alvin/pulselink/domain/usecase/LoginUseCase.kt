package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.model.User
import com.alvin.pulselink.domain.model.UserRole
import com.alvin.pulselink.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        username: String,
        password: String,
        role: UserRole
    ): Result<User> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Username and password cannot be empty"))
        }
        
        return authRepository.login(username, password, role)
    }
}
