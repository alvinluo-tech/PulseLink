# Firestore Rules 权限修复说明

## 🐛 问题
老人端 Profile 页面报错：
```
PERMISSION_DENIED: Missing or insufficient permissions.
```

## 🔍 原因分析

### 旧的权限规则（有问题）
```javascript
function isSeniorSelf() {
  // ❌ 错误：seniorId 是路径参数（文档ID），不是用户的 Auth UID
  return seniorId == resource.data.id;
}
```

**问题**：
- 老人登录后，`request.auth.uid` 是 Firebase Auth UID（例如：`abc123xyz`）
- `seniorId` 是文档路径参数（例如：`SNR-ABCD1234`）
- 这两者永远不会相等，所以老人无法读取自己的数据

### 新的权限规则（已修复）✅
```javascript
function isSeniorSelf() {
  // ✅ 正确：通过 users 集合查找当前用户的 seniorId
  return isAuthenticated() 
         && exists(/databases/$(database)/documents/users/$(request.auth.uid))
         && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.seniorId == seniorId;
}
```

**逻辑流程**：
1. 检查用户已登录 (`isAuthenticated()`)
2. 检查 `users/{auth.uid}` 文档是否存在
3. 从 `users/{auth.uid}` 读取 `seniorId` 字段
4. 将读取的 `seniorId` 与当前路径的 `seniorId` 进行比较

## 📊 数据结构

### users 集合
```
users/{firebaseAuthUid}/
  - uid: "abc123xyz"              // Firebase Auth UID
  - email: "senior_SNR-ABCD1234@pulselink.app"
  - username: "张三"
  - role: "SENIOR"
  - seniorId: "SNR-ABCD1234"      // ⭐ 关键字段
  - createdAt: 1732435200000
```

### seniors 集合
```
seniors/{seniorId}/               // 例如: SNR-ABCD1234
  - id: "SNR-ABCD1234"
  - name: "张三"
  - age: 75
  - gender: "Male"
  - avatarType: "ELDERLY_MALE"
  - caregiverIds: ["caregiver_uid_1", "caregiver_uid_2"]
  - creatorId: "caregiver_uid_1"
  - createdAt: 1732435200000
  - ...
```

## 🔐 权限矩阵

| 角色 | 操作 | 条件 | 说明 |
|------|------|------|------|
| 创建者 (Caregiver) | Read | `creatorId == auth.uid` | 创建者可以读取自己创建的老人数据 |
| 绑定的护理者 | Read | `auth.uid in caregiverIds` | 已绑定的护理者可以读取 |
| **老人自己** | **Read** | **`users/{auth.uid}.seniorId == seniorId`** | **⭐ 老人可以读取自己的数据** |
| 创建者 | Create | `creatorId == auth.uid` | 只有创建者可以创建 |
| 创建者 | Update | `creatorId == auth.uid` | 创建者可以更新（包括审批链接请求） |
| 创建者 | Delete | `creatorId == auth.uid` | 只有创建者可以删除 |

## ✅ 部署状态

```bash
firebase deploy --only firestore:rules
```

**结果**：
```
✅ cloud.firestore: rules file firestore.rules compiled successfully
✅ firestore: released rules firestore.rules to cloud.firestore
✅ Deploy complete!
```

## 🧪 测试验证

### 测试场景 1：老人读取自己的数据
```kotlin
// 老人登录后
val seniorId = localDataSource.getUser()?.first // "SNR-ABCD1234"
val result = seniorRepository.getSeniorById(seniorId)

// ✅ 应该成功
// request.auth.uid = "abc123xyz"
// users/abc123xyz.seniorId = "SNR-ABCD1234"
// seniorId (路径) = "SNR-ABCD1234"
// 匹配成功！
```

### 测试场景 2：护理者读取绑定的老人数据
```kotlin
// Caregiver 登录后
val result = seniorRepository.getSeniorById("SNR-ABCD1234")

// ✅ 应该成功（如果该 caregiver 的 UID 在 caregiverIds 中）
// request.auth.uid in seniors/SNR-ABCD1234.caregiverIds
```

### 测试场景 3：未授权用户尝试读取
```kotlin
// 一个未关联的用户尝试读取
val result = seniorRepository.getSeniorById("SNR-ABCD1234")

// ❌ 应该失败（PERMISSION_DENIED）
// 不是创建者，不在 caregiverIds，也不是老人自己
```

## 🎯 修复效果

### 修复前
```
❌ ProfileViewModel: Failed to load senior data: PERMISSION_DENIED
```

### 修复后
```
✅ ProfileViewModel: Senior data loaded: name=张三, age=75, gender=Male
✅ ProfileViewModel: Avatar emoji: 👴
✅ ProfileViewModel: Profile loaded successfully
```

## 📝 相关文件

- `firestore.rules` - Firestore 安全规则
- `ProfileViewModel.kt` - Profile 页面逻辑
- `SeniorRepositoryImpl.kt` - Senior 数据访问
- `LocalDataSource.kt` - 本地缓存

## 💡 注意事项

1. **Firestore Rules 更改后立即生效**，无需重启应用
2. 老人登录时，`users` 集合中必须有正确的 `seniorId` 字段
3. 如果老人账户是通过 Cloud Function `createSeniorAccount` 创建的，`seniorId` 会自动设置
4. 如果遇到权限问题，首先检查：
   - 用户是否已登录（`request.auth != null`）
   - `users/{uid}` 文档是否存在
   - `users/{uid}.seniorId` 是否正确设置

## 🔗 相关文档

- [Firebase Security Rules 文档](https://firebase.google.com/docs/firestore/security/get-started)
- [Firestore Rules 函数](https://firebase.google.com/docs/firestore/security/rules-conditions#functions)
- `DEBUG_PROFILE_LOADING.md` - Profile 页面调试指南
