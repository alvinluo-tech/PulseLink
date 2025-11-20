package com.alvin.pulselink.data.repository

import com.alvin.pulselink.domain.model.User
import com.alvin.pulselink.domain.model.UserRole
import com.alvin.pulselink.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {
    
    /**
     * 登录
     */
    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Login failed")
            
            // 检查 Firestore 中是否已有用户文档，如果没有则创建
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            
            if (!userDoc.exists()) {
                // 从 User Profile 中解析用户名和角色
                val displayName = user.displayName ?: "User|SENIOR"
                val parts = displayName.split("|")
                val username = parts.getOrNull(0) ?: "User"
                val role = parts.getOrNull(1) ?: "SENIOR"
                
                // 创建用户文档
                val newUserDoc = hashMapOf(
                    "uid" to user.uid,
                    "email" to user.email,
                    "username" to username,
                    "role" to role,
                    "createdAt" to System.currentTimeMillis(),
                    "emailVerified" to user.isEmailVerified
                )
                
                firestore.collection("users")
                    .document(user.uid)
                    .set(newUserDoc)
                    .await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 注册（自动发送验证邮件）
     */
    override suspend fun register(
        email: String,
        password: String,
        username: String,
        role: UserRole
    ): Result<Unit> {
        return try {
            // 1. 创建 Firebase 账号
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("User creation failed")
            
            // 2. 立即发送验证邮件
            user.sendEmailVerification().await()
            
            // 3. 临时保存用户信息到本地（等验证后再同步到 Firestore）
            // 将 username 和 role 保存到 Firebase User Profile
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName("$username|$role")  // 格式: "用户名|角色"
                .build()
            user.updateProfile(profileUpdates).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 忘记密码
     */
    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 检查邮箱验证状态
     */
    override suspend fun isEmailVerified(): Boolean {
        return try {
            // 刷新用户状态
            firebaseAuth.currentUser?.reload()?.await()
            firebaseAuth.currentUser?.isEmailVerified ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 重新发送验证邮件
     */
    override suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser ?: throw Exception("No user logged in")
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取当前用户 UID
     */
    override suspend fun getCurrentUid(): String? {
        return firebaseAuth.currentUser?.uid
    }
    
    /**
     * 获取当前用户信息
     */
    override suspend fun getCurrentUser(): User? {
        return try {
            val firebaseUser = firebaseAuth.currentUser ?: return null
            val uid = firebaseUser.uid
            
            // 先尝试从 Firestore 获取
            val document = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            
            if (document.exists()) {
                User(
                    id = uid,
                    username = document.getString("username") ?: "",
                    role = UserRole.valueOf(document.getString("role") ?: "SENIOR")
                )
            } else {
                // 如果 Firestore 中没有，从 User Profile 解析
                val displayName = firebaseUser.displayName ?: "User|SENIOR"
                val parts = displayName.split("|")
                User(
                    id = uid,
                    username = parts.getOrNull(0) ?: "User",
                    role = UserRole.valueOf(parts.getOrNull(1) ?: "SENIOR")
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 登出
     */
    override suspend fun logout() {
        firebaseAuth.signOut()
    }
    
    /**
     * 检查登录状态
     */
    override suspend fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}
