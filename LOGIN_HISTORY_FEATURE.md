# Login History Feature - 账号密码组合记忆

## Overview
登录界面的账号和密码作为组合保存，选择历史账号时自动填充对应的密码。

## Implementation Details

### 1. LoginHistoryManager (Singleton)
**Location:** `app/src/main/java/com/alvin/pulselink/util/LoginHistoryManager.kt`

**核心改进：**
- ✅ 使用 `kotlinx.serialization` 序列化存储
- ✅ 账号和密码作为 `LoginCredential` 组合保存
- ✅ 选择账号时自动填充对应的密码
- ✅ 最多保存 5 组最近的登录凭证

**数据结构：**
```kotlin
@Serializable
data class LoginCredential(
    val account: String,  // 邮箱或SNR-ID
    val password: String  // 对应的密码
)
```

**核心方法：**

**Caregiver 端：**
```kotlin
// 保存账号密码组合
LoginHistoryManager.saveEmailCredential(context, email, password)

// 获取账号列表（用于下拉显示）
LoginHistoryManager.getEmailHistory(context): List<String>

// 根据账号获取密码
LoginHistoryManager.getPasswordForEmail(context, email): String?
```

**Senior 端：**
```kotlin
// 保存账号密码组合
LoginHistoryManager.saveVirtualIdCredential(context, virtualId, password)

// 获取账号列表（用于下拉显示）
LoginHistoryManager.getVirtualIdHistory(context): List<String>

// 根据账号获取密码
LoginHistoryManager.getPasswordForVirtualId(context, virtualId): String?
```

### 2. LoginScreen 自动填充逻辑
**Location:** `app/src/main/java/com/alvin/pulselink/presentation/auth/LoginScreen.kt`

**实现细节：**

1. **输入时自动填充：**
   ```kotlin
   onValueChange = { newValue ->
       onEmailChange(newValue)
       expanded = true
       
       // 自动填充对应的密码
       val password = if (userRole == UserRole.SENIOR) {
           LoginHistoryManager.getPasswordForVirtualId(context, newValue)
       } else {
           LoginHistoryManager.getPasswordForEmail(context, newValue)
       }
       password?.let { onPasswordChange(it) }
   }
   ```

2. **下拉选择自动填充：**
   ```kotlin
   DropdownMenuItem(
       text = { Text(suggestion) },
       onClick = {
           onEmailChange(suggestion)
           expanded = false
           
           // 自动填充对应的密码
           val password = if (userRole == UserRole.SENIOR) {
               LoginHistoryManager.getPasswordForVirtualId(context, suggestion)
           } else {
               LoginHistoryManager.getPasswordForEmail(context, suggestion)
           }
           password?.let { onPasswordChange(it) }
       }
   )
   ```

3. **登录成功后保存：**
   ```kotlin
   LaunchedEffect(uiState.isSuccess) {
       if (uiState.isSuccess) {
           // 保存账号密码组合
           if (userRole == UserRole.SENIOR) {
               LoginHistoryManager.saveVirtualIdCredential(context, uiState.email, uiState.password)
           } else {
               LoginHistoryManager.saveEmailCredential(context, uiState.email, uiState.password)
           }
           onNavigateToHome()
       }
   }
   ```

### 3. 依赖配置
**Location:** `app/build.gradle.kts`

添加了 Kotlinx Serialization 支持：
```kotlin
plugins {
    // ...
    kotlin("plugin.serialization") version "2.0.21"
}

dependencies {
    // ...
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
```

## User Experience Flow

### Caregiver 登录流程：
1. 打开登录界面
2. 点击邮箱输入框 → 显示历史邮箱列表
3. **选择历史邮箱 → 密码自动填充** ✨
4. 点击登录 → 成功后保存邮箱+密码组合
5. 下次登录时可以快速选择

### Senior 登录流程：
1. 打开登录界面
2. 点击账号ID输入框 → 显示历史SNR-ID列表
3. **选择历史SNR-ID → 密码自动填充** ✨
4. 点击登录 → 成功后保存SNR-ID+密码组合
5. 下次登录时可以快速选择

## Technical Implementation

### 数据存储格式
使用 JSON 序列化后用分隔符拼接：
```
{"account":"test@example.com","password":"pass123"}|||{"account":"user@test.com","password":"pass456"}
```

### 安全考虑
- ⚠️ 密码存储在 SharedPreferences 中（明文）
- 建议未来升级：使用 Android Keystore 加密存储
- 仅限本地设备存储，不会上传到服务器

### 更新机制
- 相同账号再次登录会更新密码并移到最前
- 自动去重，最多保留 5 组
- 支持清空历史：`clearEmailHistory()` / `clearVirtualIdHistory()`

## Testing Scenarios

✅ **测试场景：**
1. 首次登录保存账号密码组合
2. 选择历史账号自动填充密码
3. 手动输入历史账号也能自动填充密码
4. 相同账号不同密码会更新记录
5. 密码错误不影响历史记录（仅成功登录才保存）
6. 历史记录跨应用重启持久化
7. Senior 和 Caregiver 历史记录分离存储

## Implementation Date
- 初版（仅账号记忆）: 2024-11-26
- 升级版（账号密码组合）: 2024-11-26

## Related Files
- `LoginHistoryManager.kt` - 核心工具类（账号密码组合存储）
- `LoginScreen.kt` - UI集成（自动填充逻辑）
- `AuthViewModel.kt` - 登录流程
- `build.gradle.kts` - Kotlinx Serialization 依赖配置

