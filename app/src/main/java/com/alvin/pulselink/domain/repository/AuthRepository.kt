package com.alvin.pulselink.domain.repository

import com.alvin.pulselink.domain.model.User
import com.alvin.pulselink.domain.model.UserRole

interface AuthRepository {
    suspend fun login(username: String, password: String, role: UserRole): Result<User>
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
    suspend fun getCurrentUser(): User?
}
