# Firebase Integration Guide

## ✅ 已完成的集成

### 1. 核心功能 (Auth)

#### AuthRepository 接口
位置：`domain/repository/AuthRepository.kt`

功能：
- ✅ `login(email, password)` - 登录
- ✅ `register(email, password, username, role)` - 注册并自动发送验证邮件
- ✅ `resetPassword(email)` - 发送密码重置邮件
- ✅ `isEmailVerified()` - 检查邮箱验证状态
- ✅ `sendEmailVerification()` - 重新发送验证邮件
- ✅ `getCurrentUid()` - 获取当前用户 UID
- ✅ `getCurrentUser()` - 获取当前用户信息
- ✅ `logout()` - 登出
- ✅ `isLoggedIn()` - 检查登录状态

#### AuthRepositoryImpl 实现
位置：`data/repository/AuthRepositoryImpl.kt`

特性：
- 使用 `kotlinx-coroutines-play-services` 的 `.await()` 扩展
- 所有操作都包裹在 `try-catch` 中
- 返回标准的 `Result<T>` 类型
- 注册成功后自动发送验证邮件
- 用户信息存储在 Firestore `users` 集合中

### 2. 数据库测试 (Firestore)

#### HealthRepository 接口
位置：`domain/repository/HealthRepository.kt`

新增功能：
- ✅ `testConnection()` - 测试 Firestore 连接

#### HealthRepositoryImpl 实现
位置：`data/repository/HealthRepositoryImpl.kt`

特性：
- 向 `test_logs` 集合写入测试数据
- 包含时间戳和状态信息

### 3. Use Cases

#### LoginUseCase
- 验证邮箱和密码格式
- 调用 `AuthRepository.login()`

#### RegisterUseCase
位置：`domain/usecase/RegisterUseCase.kt`
- 验证所有字段（邮箱、密码、用户名）
- 邮箱格式验证
- 密码长度验证（至少 6 位）
- 调用 `AuthRepository.register()`

#### ResetPasswordUseCase
位置：`domain/usecase/ResetPasswordUseCase.kt`
- 验证邮箱格式
- 调用 `AuthRepository.resetPassword()`

#### TestFirestoreConnectionUseCase
位置：`domain/usecase/TestFirestoreConnectionUseCase.kt`
- 测试 Firestore 数据库连接

### 4. 依赖注入 (Hilt)

#### AppModule
位置：`di/AppModule.kt`

提供的实例：
- ✅ `FirebaseAuth` - Firebase 认证实例
- ✅ `FirebaseFirestore` - Firestore 数据库实例
- ✅ `AuthRepository` - 认证仓库
- ✅ `HealthRepository` - 健康数据仓库
- ✅ `LoginUseCase` - 登录用例
- ✅ `RegisterUseCase` - 注册用例
- ✅ `ResetPasswordUseCase` - 重置密码用例
- ✅ `TestFirestoreConnectionUseCase` - 测试连接用例

### 5. ViewModel 更新

#### LoginViewModel
- ✅ 使用邮箱登录（而非用户名）
- ✅ 登录后检查邮箱验证状态
- ✅ 未验证邮箱会提示错误

#### RegisterViewModel
- ✅ 集成 `RegisterUseCase`
- ✅ 注册成功后提示检查邮箱

#### ForgotPasswordViewModel
- ✅ 集成 `ResetPasswordUseCase`
- ✅ 发送成功后显示确认消息

### 6. 测试页面

#### FirebaseTestScreen
位置：`presentation/test/FirebaseTestScreen.kt`
- 提供 UI 界面测试 Firestore 连接
- 显示成功/失败状态

## 📋 使用指南

### 测试 Firestore 连接

1. 在导航中添加测试页面路由：
```kotlin
// Screen.kt
object FirebaseTest : Screen("firebase_test")

// NavGraph.kt
composable(route = Screen.FirebaseTest.route) {
    FirebaseTestScreen()
}
```

2. 运行应用，导航到测试页面
3. 点击"Test Firestore Connection"按钮
4. 检查 Firebase Console 的 Firestore 数据库，应该能看到 `test_logs` 集合中的新数据

### 用户注册流程

```kotlin
// 1. 用户填写注册表单
// 2. 点击注册按钮
viewModel.register(UserRole.SENIOR)

// 3. 系统执行：
//    - 创建 Firebase 账号
//    - 自动发送验证邮件
//    - 保存用户信息到 Firestore
//    - 显示成功消息

// 4. 用户收到邮件并点击验证链接
// 5. 用户可以登录
```

### 登录流程

```kotlin
// 1. 用户输入邮箱和密码
// 2. 点击登录按钮
viewModel.login(UserRole.SENIOR)

// 3. 系统执行：
//    - 调用 Firebase Auth 登录
//    - 检查邮箱是否已验证
//    - 验证通过则跳转到主页
//    - 未验证则显示错误提示
```

### 忘记密码流程

```kotlin
// 1. 用户输入邮箱
// 2. 点击发送重置链接
viewModel.sendResetCode()

// 3. 系统执行：
//    - 发送密码重置邮件
//    - 显示确认消息

// 4. 用户收到邮件并点击重置链接
// 5. 在 Firebase 提供的页面设置新密码
```

## 🔒 Firestore 数据结构

### users 集合
```
users/
  {uid}/
    uid: String           // Firebase UID
    email: String         // 用户邮箱
    username: String      // 用户名
    role: String          // SENIOR 或 CAREGIVER
    createdAt: Long       // 创建时间戳
    emailVerified: Boolean // 邮箱验证状态
```

### test_logs 集合
```
test_logs/
  {auto_id}/
    timestamp: Long       // 时间戳
    status: String        // "connected"
    message: String       // 测试消息
```

## ⚠️ 重要注意事项

1. **邮箱验证强制要求**
   - 登录时会检查 `isEmailVerified()`
   - 未验证的用户无法登录
   - 可以通过 `sendEmailVerification()` 重新发送邮件

2. **错误处理**
   - 所有 Repository 方法都返回 `Result<T>`
   - 成功：`Result.success(value)`
   - 失败：`Result.failure(exception)`

3. **协程使用**
   - 所有 Firebase 操作都是 `suspend` 函数
   - 使用 `.await()` 而非回调
   - 在 ViewModel 的 `viewModelScope` 中执行

4. **安全规则**
   - 记得在 Firebase Console 配置 Firestore 安全规则
   - 示例规则：
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /test_logs/{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## 🎯 下一步

1. ✅ 测试注册功能
2. ✅ 测试登录功能
3. ✅ 测试密码重置功能
4. ✅ 测试 Firestore 连接
5. 配置 Firestore 安全规则
6. 实现健康数据的 CRUD 操作
7. 添加用户资料更新功能
