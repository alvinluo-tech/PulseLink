package com.alvin.pulselink.data.repository

import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.domain.model.User
import com.alvin.pulselink.domain.model.UserRole
import com.alvin.pulselink.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : AuthRepository {
    
    companion object {
        private const val TEST_USERNAME = "alvin"
        private const val TEST_PASSWORD = "123456"
    }
    
    override suspend fun login(
        username: String,
        password: String,
        role: UserRole
    ): Result<User> {
        return try {
            // 简单的测试验证
            if (username == TEST_USERNAME && password == TEST_PASSWORD) {
                val user = User(
                    id = "1",
                    username = username,
                    role = role
                )
                
                // 保存用户信息到本地
                localDataSource.saveUser(
                    id = user.id,
                    username = user.username,
                    role = user.role.name
                )
                
                Result.success(user)
            } else {
                Result.failure(Exception("Invalid username or password"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun logout() {
        localDataSource.clearUser()
    }
    
    override suspend fun isLoggedIn(): Boolean {
        return localDataSource.getUser() != null
    }
    
    override suspend fun getCurrentUser(): User? {
        val userData = localDataSource.getUser() ?: return null
        val (id, username, roleString) = userData
        
        return User(
            id = id ?: return null,
            username = username ?: return null,
            role = UserRole.valueOf(roleString ?: return null)
        )
    }
}
