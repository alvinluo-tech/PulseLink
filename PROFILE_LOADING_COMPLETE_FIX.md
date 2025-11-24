# Profile Loading 完整修复方案

## 🐛 问题根源

Profile 页面一直 loading，报错：`PERMISSION_DENIED: Missing or insufficient permissions`

经过排查，发现**两个问题**：

### 问题 1: Firestore Rules 权限逻辑错误 ✅ 已修复
**位置**: `firestore.rules`

**错误逻辑**:
```javascript
function isSeniorSelf() {
  return seniorId == resource.data.id;  // ❌ 永远不会匹配
}
```

**修复后**:
```javascript
function isSeniorSelf() {
  return isAuthenticated() 
         && exists(/databases/$(database)/documents/users/$(request.auth.uid))
         && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.seniorId == seniorId;
}
```

### 问题 2: 登录时未正确保存 seniorId ✅ 已修复
**位置**: `AuthRepositoryImpl.kt` - `login()` 方法

**错误逻辑**:
```kotlin
// ❌ 对于 senior 用户，保存的是 Firebase Auth UID，而不是 seniorId
localDataSource.saveUser(
    id = user.uid,  // 例如: "abc123xyz"
    username = username,
    role = role.lowercase()
)
```

**修复后**:
```kotlin
// ✅ 从 Firestore users 文档读取 seniorId，保存正确的 ID
var userId = user.uid
if (userRole == "SENIOR") {
    val seniorId = userDoc.getString("seniorId")
    if (!seniorId.isNullOrBlank()) {
        userId = seniorId  // 例如: "SNR-ABCD1234"
    }
}

localDataSource.saveUser(
    id = userId,  // Senior: "SNR-ABCD1234", Caregiver: "abc123xyz"
    username = finalUsername,
    role = finalRole
)
```

## 📊 数据流程对比

### 修复前（错误）❌
```
Senior 登录
  ↓
Firebase Auth UID = "abc123xyz"
  ↓
LocalDataSource.saveUser(id="abc123xyz", ...)  ❌ 保存了错误的 ID
  ↓
ProfileViewModel 读取: seniorId = "abc123xyz"
  ↓
SeniorRepository.getSeniorById("abc123xyz")  ❌ 查询错误的文档
  ↓
Firestore Rules: 
  - request.auth.uid = "abc123xyz"
  - users/abc123xyz.seniorId = "SNR-ABCD1234"
  - 请求访问 seniors/abc123xyz  ❌ 文档不存在
  ↓
PERMISSION_DENIED
```

### 修复后（正确）✅
```
Senior 登录
  ↓
Firebase Auth UID = "abc123xyz"
  ↓
读取 users/abc123xyz 文档
  ↓
获取 seniorId = "SNR-ABCD1234"
  ↓
LocalDataSource.saveUser(id="SNR-ABCD1234", ...)  ✅ 保存正确的 seniorId
  ↓
ProfileViewModel 读取: seniorId = "SNR-ABCD1234"
  ↓
SeniorRepository.getSeniorById("SNR-ABCD1234")  ✅ 查询正确的文档
  ↓
Firestore Rules:
  - request.auth.uid = "abc123xyz"
  - users/abc123xyz.seniorId = "SNR-ABCD1234"
  - 请求访问 seniors/SNR-ABCD1234  ✅ 文档存在
  - seniorId 匹配：users 中的 "SNR-ABCD1234" == 路径中的 "SNR-ABCD1234"
  ↓
允许访问 ✅
  ↓
Profile 数据加载成功
```

## 🔧 修复的文件

### 1. `firestore.rules` ✅
- 修复了 `isSeniorSelf()` 函数
- 通过 `users` 集合查询 `seniorId`
- 已部署：`firebase deploy --only firestore:rules`

### 2. `AuthRepositoryImpl.kt` ✅
- 修复了 `login()` 方法
- 对于 senior 用户，从 Firestore 读取 `seniorId` 并保存
- 对于 caregiver 用户，仍然保存 auth UID

### 3. `ProfileViewModel.kt` ✅
- 增强了日志记录
- 改用 `getHealthHistory()` 获取健康数据
- 添加了详细的错误处理

## 🧪 测试步骤

### 1. 重新登录（必须！）
由于登录逻辑已修改，**必须重新登录**才能让 `seniorId` 正确保存到本地缓存。

1. 退出当前账户
2. 重新登录老人账户
3. 查看 Logcat 确认日志：
   ```
   AuthRepo: Login success: id=SNR-ABCD1234, username=张三, role=senior
   ```

### 2. 查看 Profile 页面
打开 Profile 页面，应该看到：

**Logcat 日志**:
```
✅ ProfileViewModel: Cached user: id=SNR-ABCD1234, name=张三, role=senior
✅ ProfileViewModel: Loading profile for senior: SNR-ABCD1234
✅ ProfileViewModel: Senior data loaded: name=张三, age=75, gender=Male
👤 ProfileViewModel: Avatar emoji: 👴
📊 ProfileViewModel: Health history size: X
✅ ProfileViewModel: Profile loaded successfully
```

**UI 显示**:
- 头像：👴/👵（根据年龄和性别）
- 姓名：张三
- 年龄和使用天数：Age 75 · Used 12 days
- 血压：128/82（如果有数据）
- 心率：78（如果有数据）

### 3. 如果仍然失败
在 Logcat 中过滤 `ProfileViewModel` 和 `AuthRepo`，查找错误信息。

## 📋 数据结构验证

### Firestore users 集合
```
users/{firebaseAuthUid}/
  - uid: "abc123xyz"
  - email: "senior_SNR-ABCD1234@pulselink.app"
  - username: "张三"
  - role: "SENIOR"
  - seniorId: "SNR-ABCD1234"  ⭐ 关键字段
  - createdAt: 1732435200000
```

### Firestore seniors 集合
```
seniors/SNR-ABCD1234/
  - id: "SNR-ABCD1234"
  - name: "张三"
  - age: 75
  - gender: "Male"
  - avatarType: "ELDERLY_MALE"
  - caregiverIds: [...]
  - createdAt: 1732435200000
```

### LocalDataSource 缓存
```kotlin
Triple(
    "SNR-ABCD1234",  // ⭐ seniorId（不是 auth UID）
    "张三",          // username
    "senior"         // role
)
```

## ✅ 完成清单

- [x] 修复 Firestore Rules 的 `isSeniorSelf()` 函数
- [x] 部署 Firestore Rules
- [x] 修复 `AuthRepositoryImpl.login()` 保存 seniorId
- [x] 增强 `ProfileViewModel` 日志
- [x] 创建调试文档

## 🎯 关键要点

1. **Senior 用户的 ID 是 `seniorId`，不是 Firebase Auth UID**
2. **登录时必须从 Firestore `users` 文档读取 `seniorId`**
3. **Firestore Rules 需要通过 `users` 集合查询 `seniorId`**
4. **修改后必须重新登录才能生效**

## 📚 相关文档

- `FIRESTORE_RULES_FIX.md` - Firestore 权限修复详解
- `DEBUG_PROFILE_LOADING.md` - Profile 页面调试指南
- `firestore.rules` - Firestore 安全规则
- `AuthRepositoryImpl.kt` - 认证仓库实现
- `ProfileViewModel.kt` - Profile 视图模型
