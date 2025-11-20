package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.model.UserRole
import com.alvin.pulselink.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        username: String,
        role: UserRole
    ): Result<Unit> {
        if (email.isBlank() || password.isBlank() || username.isBlank()) {
            return Result.failure(Exception("All fields are required"))
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(Exception("Invalid email format"))
        }
        
        if (password.length < 6) {
            return Result.failure(Exception("Password must be at least 6 characters"))
        }
        
        return authRepository.register(email, password, username, role)
    }
}
