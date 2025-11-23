package com.alvin.pulselink.data.repository

import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.domain.model.User
import com.alvin.pulselink.domain.model.UserRole
import com.alvin.pulselink.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val localDataSource: LocalDataSource
) : AuthRepository {
    
    /**
     * 登录
     */
    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            // 登录阶段设置总体超时，避免网络问题导致长时间卡住
            val authResult = withTimeout(15_000) {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
            }
            val user = authResult.user ?: throw Exception("Login failed")
            
            // 从 displayName 中解析用户名和角色
            val displayName = user.displayName ?: "User|SENIOR"
            val parts = displayName.split("|")
            val username = parts.getOrNull(0) ?: "User"
            val role = parts.getOrNull(1) ?: "SENIOR"
            
            // 保存到本地 DataStore
            localDataSource.saveUser(
                id = user.uid,
                username = username,
                role = role.lowercase()  // "SENIOR" -> "senior"
            )
            
            // 尽量确保 Firestore 中有用户文档，但失败不影响登录成功
            runCatching {
                val userDoc = withTimeout(8_000) {
                    firestore.collection("users").document(user.uid).get().await()
                }
                if (!userDoc.exists()) {
                    // 创建用户文档
                    val newUserDoc = hashMapOf(
                        "uid" to user.uid,
                        "email" to user.email,
                        "username" to username,
                        "role" to role,
                        "createdAt" to System.currentTimeMillis(),
                        "emailVerified" to user.isEmailVerified
                    )
                    withTimeout(8_000) {
                        firestore.collection("users")
                            .document(user.uid)
                            .set(newUserDoc)
                            .await()
                    }
                }
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
            // 1. 创建 Firebase 账号（超时保护）
            val authResult = withTimeout(15_000) {
                firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            }
            val user = authResult.user ?: throw Exception("User creation failed")
            
            // 2. 立即发送验证邮件（超时保护）
            runCatching {
                withTimeout(8_000) { user.sendEmailVerification().await() }
            }
            
            // 3. 临时保存用户信息到本地（等验证后再同步到 Firestore）
            // 将 username 和 role 保存到 Firebase User Profile
            runCatching {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName("$username|$role")  // 格式: "用户名|角色"
                    .build()
                withTimeout(8_000) { user.updateProfile(profileUpdates).await() }
            }
            
            // 4. 提前写入 users 文档，减少首次登录的额外网络交互成本（失败仅记录，不影响注册成功）
            runCatching {
                val newUserDoc = hashMapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "username" to username,
                    "role" to role.name,
                    "createdAt" to System.currentTimeMillis(),
                    "emailVerified" to false
                )
                withTimeout(8_000) {
                    firestore.collection("users")
                        .document(user.uid)
                        .set(newUserDoc)
                        .await()
                }
            }
            
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
                    email = document.getString("email") ?: firebaseUser.email ?: "",
                    name = document.getString("username") ?: "",
                    username = document.getString("username") ?: "",
                    role = UserRole.valueOf(document.getString("role") ?: "SENIOR")
                )
            } else {
                // 如果 Firestore 中没有，从 User Profile 解析
                val displayName = firebaseUser.displayName ?: "User|SENIOR"
                val parts = displayName.split("|")
                User(
                    id = uid,
                    email = firebaseUser.email ?: "",
                    name = parts.getOrNull(0) ?: "User",
                    username = parts.getOrNull(0) ?: "User",
                    role = UserRole.valueOf(parts.getOrNull(1) ?: "SENIOR")
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 根据用户ID获取用户信息
     */
    override suspend fun getUserById(userId: String): Result<User> {
        return try {
            android.util.Log.d("AuthRepository", "Getting user by ID: $userId")
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            android.util.Log.d("AuthRepository", "Document exists: ${document.exists()}")
            
            if (document.exists()) {
                val email = document.getString("email") ?: ""
                val username = document.getString("username") ?: ""
                val role = document.getString("role") ?: "SENIOR"
                
                android.util.Log.d("AuthRepository", "User data - email: $email, username: $username, role: $role")
                
                val user = User(
                    id = userId,
                    email = email,
                    name = username,
                    username = username,
                    role = UserRole.valueOf(role)
                )
                Result.success(user)
            } else {
                android.util.Log.e("AuthRepository", "User document not found for ID: $userId")
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error getting user by ID: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 登出
     */
    override suspend fun logout() {
        // 清除本地缓存
        localDataSource.clearUser()
        // 登出 Firebase
        firebaseAuth.signOut()
    }
    
    /**
     * 检查登录状态
     */
    override suspend fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
    
    /**
     * 修改密码
     */
    override suspend fun changePassword(newPassword: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser 
                ?: throw Exception("No user logged in")
            
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 删除账户
     */
    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser 
                ?: throw Exception("No user logged in")
            
            val uid = user.uid
            
            // 1. 删除 Firestore 中的用户数据
            firestore.collection("users")
                .document(uid)
                .delete()
                .await()
            
            // 2. 删除 Firebase Authentication 账户
            user.delete().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
