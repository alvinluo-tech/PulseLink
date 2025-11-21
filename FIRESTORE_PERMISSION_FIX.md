# 🔧 解决注册时 Firestore 权限问题

## 问题原因

注册时遇到 `PERMISSION_DENIED` 错误，因为：
1. 用户刚创建账号，还未完全认证
2. Firestore 规则要求 `request.auth != null`
3. 在发送验证邮件前尝试写入 Firestore 会被拒绝

## ✅ 解决方案

### 策略：延迟 Firestore 写入

**注册时：**
- ✅ 创建 Firebase Auth 账号
- ✅ 发送验证邮件
- ✅ 将用户名和角色保存到 Firebase User Profile（不需要 Firestore 权限）
- ❌ **不再**写入 Firestore

**首次登录时：**
- ✅ 检查 Firestore 中是否已有用户文档
- ✅ 如果没有，从 User Profile 解析信息并创建文档
- ✅ 此时用户已认证，有权限写入

### 代码修改

#### 1. 注册流程（AuthRepositoryImpl.kt）

```kotlin
override suspend fun register(...): Result<Unit> {
    // 1. 创建账号
    val user = firebaseAuth.createUserWithEmailAndPassword(email, password).await().user
    
    // 2. 发送验证邮件
    user?.sendEmailVerification()?.await()
    
    // 3. 保存到 User Profile（格式: "用户名|角色"）
    val profileUpdates = UserProfileChangeRequest.Builder()
        .setDisplayName("$username|$role")
        .build()
    user?.updateProfile(profileUpdates)?.await()
    
    // ✅ 不写入 Firestore
}
```

#### 2. 登录流程（AuthRepositoryImpl.kt）

```kotlin
override suspend fun login(email: String, password: String): Result<Unit> {
    val user = firebaseAuth.signInWithEmailAndPassword(email, password).await().user
    
    // 检查 Firestore 是否已有文档
    val userDoc = firestore.collection("users").document(user.uid).get().await()
    
    if (!userDoc.exists()) {
        // 首次登录，从 User Profile 创建文档
        val displayName = user.displayName ?: "User|SENIOR"
        val parts = displayName.split("|")
        
        firestore.collection("users").document(user.uid).set(
            hashMapOf(
                "uid" to user.uid,
                "email" to user.email,
                "username" to parts[0],
                "role" to parts[1],
                "createdAt" to System.currentTimeMillis(),
                "emailVerified" to user.isEmailVerified
            )
        ).await()
    }
}
```

## 📋 Firestore 规则配置

将以下规则复制到 Firebase Console → Firestore Database → 规则：

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // 用户文档 - 只能在登录后访问
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // 测试日志 - 开发测试用（生产环境请限制）
    match /test_logs/{document=**} {
      allow read, write: if true;
    }
    
    // 其他数据集合
    match /health_data/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    // 老人账户 seniors（支持多护理者 + 创建者）
    match /seniors/{seniorId} {
      function isAuthenticated() {
        return request.auth != null;
      }

      function isCreator() {
        return isAuthenticated() && resource.data.creatorId == request.auth.uid;
      }

      function isCaregiverBound() {
        return isAuthenticated() && (request.auth.uid in resource.data.caregiverIds);
      }

      // 创建：必须由创建者执行，且创建者在 caregiverIds 中
      allow create: if isAuthenticated()
                    && request.resource.data.creatorId == request.auth.uid
                    && (request.auth.uid in request.resource.data.caregiverIds)
                    && request.resource.data.caregiverIds.size() >= 1
                    && request.resource.data.keys().hasOnly([
                      'name','age','gender','healthHistory','caregiverIds','creatorId','createdAt'
                    ]);

      // 读取：创建者或已绑定护理者
      allow read: if isCreator() || isCaregiverBound();

      // 删除：仅创建者
      allow delete: if isCreator();

      // 更新：
      // 1) 创建者无条件；
      // 2) 未绑定护理者仅允许自我绑定（只新增自身到 caregiverIds，其他字段不变）
      allow update: if isCreator()
                    || (
                      isAuthenticated()
                      && !(request.auth.uid in resource.data.caregiverIds)
                      && (request.auth.uid in request.resource.data.caregiverIds)
                      && request.resource.data.caregiverIds.size() == resource.data.caregiverIds.size() + 1
                      && request.resource.data.creatorId == resource.data.creatorId
                      && request.resource.data.diff(resource.data).changedKeys().hasOnly(['caregiverIds'])
                      && request.resource.data.caregiverIds.hasAll(resource.data.caregiverIds)
                    )
                    || isCaregiverBound();
    }
  }
}
```

**发布规则后生效！**

## 🧪 测试流程

### 1. 注册新用户
```
输入邮箱、密码、用户名 → 点击注册
→ ✅ 创建 Firebase Auth 账号
→ ✅ 发送验证邮件
→ ✅ 保存到 User Profile
→ ✅ 显示成功消息
```

### 2. 验证邮箱
```
打开邮箱 → 点击验证链接 → 邮箱验证成功
```

### 3. 首次登录
```
输入邮箱、密码 → 点击登录
→ ✅ Firebase Auth 认证
→ ✅ 检查邮箱已验证
→ ✅ 首次登录，创建 Firestore 文档
→ ✅ 跳转主页
```

### 4. 后续登录
```
输入邮箱、密码 → 点击登录
→ ✅ Firebase Auth 认证
→ ✅ Firestore 文档已存在，跳过创建
→ ✅ 跳转主页
```

## 🎯 优势

1. **✅ 无需修改 Firestore 规则** - 保持安全性
2. **✅ 注册流程更快** - 不需要等待 Firestore 写入
3. **✅ 离线注册** - User Profile 是本地操作
4. **✅ 自动同步** - 首次登录时自动创建 Firestore 文档

## ⚠️ 注意事项

1. **User Profile 格式：** `username|role`（使用 `|` 分隔）
2. **首次登录必须联网：** 需要创建 Firestore 文档
3. **Firestore 规则必须配置：** 否则测试连接会失败（尤其是 `seniors` 集合的多护理者绑定规则）

## 📊 数据流

```
注册：
Firebase Auth ← ✅ 创建账号
      ↓
User Profile ← ✅ 保存 "username|role"
      ↓
Email ← ✅ 发送验证邮件

首次登录：
Firebase Auth ← ✅ 认证
      ↓
Firestore ← ✅ 创建用户文档（从 User Profile 解析）
      ↓
主页 ← ✅ 跳转

后续登录：
Firebase Auth ← ✅ 认证
      ↓
Firestore ← ✅ 读取用户文档
      ↓
主页 ← ✅ 跳转
```

---

现在重新构建应用，注册流程应该可以正常工作了！🎉
