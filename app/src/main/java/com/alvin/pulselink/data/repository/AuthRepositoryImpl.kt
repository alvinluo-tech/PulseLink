package com.alvin.pulselink.data.repository

import com.alvin.pulselink.core.constants.AuthConstants
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
     * 登录（支持邮箱或 Senior ID）
     */
    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            // 自动识别输入类型：如果是 SNR-ID 格式，转换为虚拟邮箱
            val loginEmail = if (email.matches(AuthConstants.SNR_ID_REGEX)) {
                // Senior ID 格式 -> 转换为虚拟邮箱
                AuthConstants.generateVirtualEmail(email)
            } else {
                // 普通邮箱格式
                email
            }
            
            android.util.Log.d("AuthRepo", "Login with: input=$email, converted=$loginEmail")
            
            // 登录阶段设置总体超时，避免网络问题导致长时间卡住
            val authResult = withTimeout(15_000) {
                firebaseAuth.signInWithEmailAndPassword(loginEmail, password).await()
            }
            val user = authResult.user ?: throw Exception("Login failed")
            
            // 从 displayName 中解析用户名和角色
            val displayName = user.displayName ?: "User|SENIOR"
            val parts = displayName.split("|")
            val username = parts.getOrNull(0) ?: "User"
            val role = parts.getOrNull(1) ?: "SENIOR"
            
            // 🔍 从 Firestore users 文档读取完整用户信息（包括 seniorId）
            var userId = user.uid
            var finalUsername = username
            var finalRole = role.lowercase()
            
            runCatching {
                val userDoc = withTimeout(8_000) {
                    firestore.collection("users").document(user.uid).get().await()
                }
                if (userDoc.exists()) {
                    // 如果是 senior 用户，使用 seniorId 作为 ID
                    val userRole = userDoc.getString("role") ?: role
                    if (userRole == "SENIOR") {
                        val seniorId = userDoc.getString("seniorId")
                        if (!seniorId.isNullOrBlank()) {
                            userId = seniorId  // ⭐ 使用 seniorId 而不是 auth UID
                        }
                    }
                    // 更新用户名和角色
                    finalUsername = userDoc.getString("username") ?: username
                    finalRole = userRole.lowercase()
                } else {
                    // 创建用户文档（如果不存在）
                    // ⭐ 如果是 senior，需要提取 seniorId 从邮箱或从 seniors 集合
                    var seniorIdForDoc: String? = null
                    if (role == "SENIOR") {
                        // 从邮箱中提取 SNR-ID
                        seniorIdForDoc = AuthConstants.extractSeniorIdFromEmail(loginEmail)
                        if (seniorIdForDoc != null) {
                            userId = seniorIdForDoc  // ⭐ 使用 seniorId
                        }
                    }
                    
                    val newUserDoc = hashMapOf(
                        "uid" to user.uid,
                        "email" to user.email,
                        "username" to username,
                        "role" to role,
                        "createdAt" to System.currentTimeMillis(),
                        "emailVerified" to user.isEmailVerified
                    )
                    
                    // ⭐ 为 senior 添加 seniorId 字段
                    if (seniorIdForDoc != null) {
                        newUserDoc["seniorId"] = seniorIdForDoc
                    }
                    
                    withTimeout(8_000) {
                        firestore.collection("users")
                            .document(user.uid)
                            .set(newUserDoc)
                            .await()
                    }
                }
            }.onFailure { e ->
                android.util.Log.w("AuthRepo", "Failed to read/create user document: ${e.message}")
            }
            
            // 保存到本地 DataStore
            localDataSource.saveUser(
                id = userId,  // Senior: seniorId, Caregiver: auth UID
                username = finalUsername,
                role = finalRole
            )

            android.util.Log.d("AuthRepo", "Login success: id=$userId, username=$finalUsername, role=$finalRole")

            // ⭐ 绑定/迁移：确保 senior_profiles 中的 userId 绑定当前登录 UID
            if (finalRole == "senior") {
                runCatching {
                    withTimeout(8_000) {
                        val profileRef = firestore.collection("senior_profiles").document(userId)
                        val profileDoc = profileRef.get().await()
                        if (profileDoc.exists()) {
                            val boundUid = profileDoc.getString("userId")
                            if (boundUid == null || boundUid != user.uid) {
                                android.util.Log.d("AuthRepo", "Binding senior_profiles.userId → ${user.uid} for profile ${userId}")
                                profileRef.update("userId", user.uid).await()
                            }
                        } else {
                            // 档案不存在，尝试从 legacy seniors 迁移到 senior_profiles
                            android.util.Log.w("AuthRepo", "senior_profiles/${userId} not found, trying migrate from seniors/${userId}")
                            val legacy = firestore.collection("seniors").document(userId).get().await()
                            if (legacy.exists()) {
                                val name = legacy.getString("name") ?: finalUsername
                                val age = (legacy.getLong("age") ?: 0L).toInt()
                                val gender = legacy.getString("gender") ?: ""
                                val avatarType = legacy.getString("avatarType") ?: determineAvatarType(age, gender)
                                val creatorId = legacy.getString("creatorId") ?: user.uid
                                val createdAt = legacy.getLong("createdAt") ?: System.currentTimeMillis()
                                val registrationType = "SELF_REGISTERED"

                                val newProfile = hashMapOf(
                                    "id" to userId,
                                    "userId" to user.uid,
                                    "name" to name,
                                    "age" to age,
                                    "gender" to gender,
                                    "avatarType" to avatarType,
                                    "creatorId" to creatorId,
                                    "createdAt" to createdAt,
                                    "registrationType" to registrationType
                                )
                                android.util.Log.d("AuthRepo", "Migrating seniors/${userId} → senior_profiles/${userId}")
                                profileRef.set(newProfile).await()
                            } else {
                                android.util.Log.w("AuthRepo", "No legacy seniors/${userId} found; skip creating profile")
                            }
                        }
                    }
                }.onFailure { e ->
                    android.util.Log.w("AuthRepo", "Bind/migrate senior profile failed: ${e.message}", e)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 注册（自动发送验证邮件）- Caregiver 注册
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
     * 老人自主注册（需要额外的年龄和性别信息）
     */
    override suspend fun registerSenior(
        email: String,
        password: String,
        name: String,
        age: Int,
        gender: String
    ): Result<Unit> {
        return try {
            // 1. 创建 Firebase Auth 账号
            val authResult = withTimeout(15_000) {
                firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            }
            val user = authResult.user ?: throw Exception("User creation failed")
            
            // 2. 生成唯一的 seniorId (SNR-XXXXXXXXXXXX)
            val seniorId = generateSeniorId()
            
            // 3. 发送验证邮件
            runCatching {
                withTimeout(8_000) { user.sendEmailVerification().await() }
            }
            
            // 4. 更新 Firebase User Profile
            runCatching {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName("$name|SENIOR")
                    .build()
                withTimeout(8_000) { user.updateProfile(profileUpdates).await() }
            }
            
            // 5. 写入 Firestore users 文档
            val userDoc = hashMapOf(
                "uid" to user.uid,
                "email" to email,
                "username" to name,
                "role" to "SENIOR",
                "seniorId" to seniorId,  // ⭐ 关键字段
                "createdAt" to System.currentTimeMillis(),
                "emailVerified" to false
            )
            withTimeout(10_000) {
                firestore.collection("users")
                    .document(user.uid)
                    .set(userDoc)
                    .await()
            }
            
            // 6. 写入 Firestore senior_profiles 文档（新架构）
            val avatarType = determineAvatarType(age, gender)
            val profileDoc = hashMapOf(
                "id" to seniorId,
                "userId" to user.uid,              // 绑定当前 Auth UID
                "name" to name,
                "age" to age,
                "gender" to gender,
                "avatarType" to avatarType,
                "creatorId" to user.uid,           // 自己是创建者
                "createdAt" to System.currentTimeMillis(),
                "registrationType" to "SELF_REGISTERED"
            )
            withTimeout(10_000) {
                firestore.collection("senior_profiles")
                    .document(seniorId)
                    .set(profileDoc)
                    .await()
            }

            // 7. 写入密码到独立集合 senior_passwords（与规则一致）
            val passwordDoc = hashMapOf(
                "profileId" to seniorId,
                "password" to password,
                "createdAt" to System.currentTimeMillis()
            )
            runCatching {
                withTimeout(8_000) {
                    firestore.collection("senior_passwords")
                        .document(seniorId)
                        .set(passwordDoc)
                        .await()
                }
            }.onFailure { e ->
                android.util.Log.w("AuthRepo", "Write senior_passwords failed: ${e.message}")
            }
            
            android.util.Log.d("AuthRepo", "Senior registered: seniorId=$seniorId, name=$name, age=$age")
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepo", "Senior registration failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 生成唯一的 Senior ID (SNR-XXXXXXXXXXXX)
     * 使用时间戳 + 随机数保证唯一性
     * 
     * 格式说明：
     * - 时间戳转36进制（压缩表示，字符集：0-9A-Z）
     * - 4位随机大写字母
     * - 总长度：SNR- + 8位（时间戳部分取后缀） + 4位随机 = 12-16字符
     * 
     * 示例：SNR-KXM2VQW7ABCD
     */
    private fun generateSeniorId(): String {
        // 时间戳转36进制并转大写（36进制：0-9 + A-Z）
        val timestamp = System.currentTimeMillis().toString(36).uppercase()
        
        // 4位随机大写字母
        val random = (1..4).map { ('A'..'Z').random() }.joinToString("")
        
        // 拼接并取后12位（确保长度一致性）
        // 格式：SNR-{时间戳8位}{随机4位}
        val combined = timestamp + random
        return "SNR-${combined.takeLast(12)}"
    }
    
    /**
     * 根据年龄和性别确定头像类型
     */
    private fun determineAvatarType(age: Int, gender: String): String {
        return when {
            age >= 60 && gender.equals("Male", ignoreCase = true) -> "ELDERLY_MALE"
            age >= 60 && gender.equals("Female", ignoreCase = true) -> "ELDERLY_FEMALE"
            gender.equals("Male", ignoreCase = true) -> "ADULT_MALE"
            gender.equals("Female", ignoreCase = true) -> "ADULT_FEMALE"
            else -> "ELDERLY_MALE"
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
                val role = document.getString("role") ?: "SENIOR"
                // ⭐ 对于 Senior 用户，使用 seniorId 作为 ID
                val userId = if (role == "SENIOR") {
                    document.getString("seniorId") ?: uid
                } else {
                    uid
                }
                
                User(
                    id = userId,
                    email = document.getString("email") ?: firebaseUser.email ?: "",
                    name = document.getString("username") ?: "",
                    username = document.getString("username") ?: "",
                    role = UserRole.valueOf(role)
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
