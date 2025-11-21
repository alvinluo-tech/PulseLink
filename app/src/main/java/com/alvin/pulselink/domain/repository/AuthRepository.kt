package com.alvin.pulselink.domain.repository

import com.alvin.pulselink.domain.model.User
import com.alvin.pulselink.domain.model.UserRole

interface AuthRepository {
    // 登录
    suspend fun login(email: String, password: String): Result<Unit>
    
    // 注册（自动发送验证邮件）
    suspend fun register(email: String, password: String, username: String, role: UserRole): Result<Unit>
    
    // 忘记密码
    suspend fun resetPassword(email: String): Result<Unit>
    
    // 检查邮箱验证状态
    suspend fun isEmailVerified(): Boolean
    
    // 重新发送验证邮件
    suspend fun sendEmailVerification(): Result<Unit>
    
    // 获取当前用户 UID
    suspend fun getCurrentUid(): String?
    
    // 获取当前用户信息
    suspend fun getCurrentUser(): User?
    
    // 登出
    suspend fun logout()
    
    // 检查登录状态
    suspend fun isLoggedIn(): Boolean
    
    // 修改密码
    suspend fun changePassword(newPassword: String): Result<Unit>
    
    // 删除账户
    suspend fun deleteAccount(): Result<Unit>
}
