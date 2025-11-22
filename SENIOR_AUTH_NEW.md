# 老人端账户创建与登录方案（2.0）

## 🎯 方案概述

### 旧方案的问题
- 虚拟 ID 登录 + 匿名认证
- 每次登录 UID 不同
- 无法跨设备同步
- 依赖本地存储

### 新方案优势
- ✅ 每个老人拥有真实的 Firebase Auth 账户
- ✅ 邮箱格式: `senior_SNR-XXXXXXXX@pulselink.app`
- ✅ 支持扫码登录（子女生成二维码）
- ✅ 支持手动输入邮箱密码
- ✅ 固定 UID，支持跨设备同步
- ✅ 完整的 Firebase Auth 认证体系

---

## 📋 完整流程

### 1. 子女创建老人账户

#### 步骤 1: 在 Caregiver 端填写老人信息
- 姓名、年龄、性别、健康历史等
- 可选：自定义密码（8 位以上）
- 不填密码则系统自动生成 8 位随机密码

#### 步骤 2: 调用 Cloud Function 创建 Firebase Auth 账户
```kotlin
// CreateSeniorUseCase.kt
suspend operator fun invoke(senior: Senior, customPassword: String? = null): Result<SeniorAccountResult> {
    // 1. 创建 Firestore senior 文档（获取自动生成的 ID）
    val createdSenior = seniorRepository.createSenior(senior).getOrThrow()
    
    // 2. 调用 Cloud Function
    val data = hashMapOf(
        "seniorId" to createdSenior.id,  // 例如: SNR-ABCD1234
        "name" to createdSenior.name,
        "password" to (customPassword ?: "")
    )
    
    val result = functions.getHttpsCallable("createSeniorAccount").call(data).await()
    
    // 3. 返回账户信息（包含二维码数据）
    return Result.success(SeniorAccountResult(
        senior = createdSenior,
        email = "senior_SNR-ABCD1234@pulselink.app",
        password = "随机密码或自定义密码",
        uid = "firebase_auth_uid",
        qrCodeData = """{"type":"pulselink_login","email":"...","password":"..."}"""
    ))
}
```

#### 步骤 3: 生成二维码
```json
{
  "type": "pulselink_login",
  "email": "senior_SNR-ABCD1234@pulselink.app",
  "password": "xxxxxxxx"
}
```

UI 显示：
- 二维码图片
- 邮箱地址（可复制）
- 密码（可复制）
- 提示：老人可扫码或手动输入登录

---

### 2. 老人端登录

#### 方式 1: 扫码登录（推荐）

1. 老人端点击"扫码登录"
2. 扫描子女生成的二维码
3. 自动解析邮箱和密码
4. 调用 `authViewModel.parseQRCodeAndLogin(qrCode)`
5. 自动登录

```kotlin
// AuthViewModel.kt
fun parseQRCodeAndLogin(qrCodeData: String) {
    // 解析 JSON
    val email = extractFromJson("email", qrCodeData)
    val password = extractFromJson("password", qrCodeData)
    
    // 更新状态
    _uiState.update { it.copy(email = email, password = password) }
    
    // 自动登录
    loginSenior()
}
```

#### 方式 2: 手动输入

1. 老人端点击"手动输入"
2. 输入邮箱: `senior_SNR-ABCD1234@pulselink.app`
3. 输入密码: `xxxxxxxx`
4. 点击登录
5. 调用 `authViewModel.loginSenior()`

```kotlin
// AuthViewModel.kt
fun loginSenior() {
    // 使用标准的 Firebase Auth 邮箱登录
    val result = loginUseCase(email, password)
    
    // 验证角色是否为 SENIOR
    if (user.role != UserRole.SENIOR) {
        logout()
        showError("此账户不是老人账户")
    }
}
```

---

## 🔐 Cloud Function 实现

### `createSeniorAccount`

**功能**: 在 Firebase Auth 中创建老人账户

**权限**: 仅已登录的 Caregiver 可调用

**输入参数**:
```typescript
{
  seniorId: string,      // 例如: "SNR-ABCD1234"
  name: string,          // 老人姓名
  password?: string      // 可选，不提供则生成随机密码
}
```

**输出结果**:
```typescript
{
  success: true,
  email: "senior_SNR-ABCD1234@pulselink.app",
  password: "xxxxxxxx",
  uid: "firebase_auth_uid",
  seniorId: "SNR-ABCD1234"
}
```

**核心逻辑**:
```typescript
export const createSeniorAccount = onCall(async (request) => {
    // 1. 鉴权检查
    if (!request.auth) throw new HttpsError("unauthenticated", "请先登录");
    
    // 2. 生成邮箱和密码
    const email = `senior_${seniorId}@pulselink.app`;
    const finalPassword = password || generateRandomPassword();
    
    // 3. 创建 Firebase Auth 用户
    const userRecord = await admin.auth().createUser({
        email: email,
        password: finalPassword,
        displayName: `${name}|SENIOR`,
        emailVerified: true
    });
    
    // 4. 创建 Firestore 用户文档
    await admin.firestore().collection("users").doc(userRecord.uid).set({
        uid: userRecord.uid,
        email: email,
        username: name,
        role: "SENIOR",
        seniorId: seniorId,
        createdBy: request.auth.uid
    });
    
    return { success: true, email, password: finalPassword, uid: userRecord.uid };
});
```

---

## 📱 Android 代码变更

### 1. CreateSeniorUseCase
```kotlin
class CreateSeniorUseCase @Inject constructor(
    private val seniorRepository: SeniorRepository,
    private val functions: FirebaseFunctions  // ✅ 新增依赖
) {
    suspend operator fun invoke(
        senior: Senior,
        customPassword: String? = null  // ✅ 新增参数
    ): Result<SeniorAccountResult>  // ✅ 返回类型变更
}
```

### 2. ManageSeniorsViewModel
```kotlin
fun createSenior(onSuccess: () -> Unit) {
    createSeniorUseCase(senior)
        .onSuccess { result ->  // ✅ 结果类型变更
            _createSeniorState.update {
                it.copy(
                    createdAccountEmail = result.email,      // ✅ 显示账户信息
                    createdAccountPassword = result.password,
                    qrCodeData = result.qrCodeData           // ✅ 生成二维码
                )
            }
        }
}
```

### 3. AuthViewModel
```kotlin
// ✅ 新增老人端邮箱登录
fun loginSenior() {
    val result = loginUseCase(email, password)
    // 验证角色为 SENIOR
}

// ✅ 新增二维码解析
fun parseQRCodeAndLogin(qrCodeData: String) {
    val (email, password) = parseJSON(qrCodeData)
    loginSenior()
}

// ⚠️ 废弃虚拟ID登录
@Deprecated
fun loginSeniorById()
```

### 4. UI State 变更
```kotlin
data class CreateSeniorUiState(
    // ... 原有字段
    val createdAccountEmail: String? = null,      // ✅ 新增
    val createdAccountPassword: String? = null,   // ✅ 新增
    val qrCodeData: String? = null                // ✅ 新增
)
```

---

## 🧪 测试步骤

### 1. 部署 Cloud Function
```bash
cd functions
npm run build
firebase deploy --only functions:createSeniorAccount
```

### 2. 测试创建老人账户
1. Caregiver 端登录
2. 创建老人账户（填写姓名、年龄等）
3. 检查是否返回邮箱和密码
4. 检查 Firestore 是否有 senior 文档
5. 检查 Firebase Auth 是否有对应用户
6. 检查二维码是否生成

### 3. 测试扫码登录
1. 使用二维码扫描工具获取 JSON 字符串
2. 老人端点击"扫码登录"
3. 输入二维码数据
4. 检查是否自动登录

### 4. 测试手动登录
1. 老人端输入邮箱: `senior_SNR-XXXXXXXX@pulselink.app`
2. 输入密码
3. 点击登录
4. 检查是否成功

### 5. 测试 AI 对话
1. 老人端登录成功
2. 进入语音助手
3. 发送消息
4. 检查是否收到 AI 回复（不应有 UNAUTHENTICATED 错误）

---

## 🔄 迁移指南

### 从旧方案迁移

#### 对于现有老人账户
1. **方案 A**: 保留虚拟 ID 登录（兼容性）
   - `loginSeniorById()` 标记为 `@Deprecated` 但仍可用
   - 新创建的老人使用邮箱登录

2. **方案 B**: 数据迁移（推荐）
   - 为现有老人创建 Firebase Auth 账户
   - 迁移脚本:
   ```kotlin
   suspend fun migrateSeniorToAuth(seniorId: String) {
       val senior = seniorRepository.getSeniorById(seniorId).getOrThrow()
       val result = functions.getHttpsCallable("createSeniorAccount")
           .call(mapOf("seniorId" to seniorId, "name" to senior.name))
           .await()
       // 通知子女新的登录凭据
   }
   ```

### UI 更新建议

#### Caregiver 端
- 创建成功页面显示:
  - ✅ 二维码
  - ✅ 邮箱（可复制）
  - ✅ 密码（可复制）
  - ✅ 分享按钮（发送给老人）

#### Senior 端
- 登录页面选项:
  - ✅ 扫码登录（主推）
  - ✅ 手动输入（备选）
  - ⚠️ 虚拟 ID 登录（兼容模式，可隐藏）

---

## 📊 数据结构

### Firestore: `seniors/{seniorId}`
```json
{
  "id": "SNR-ABCD1234",
  "name": "张三",
  "age": 75,
  "gender": "Male",
  "caregiverIds": ["caregiver_uid_1"],
  "creatorId": "caregiver_uid_1",
  "createdAt": 1234567890
}
```

### Firestore: `users/{uid}`
```json
{
  "uid": "firebase_auth_uid",
  "email": "senior_SNR-ABCD1234@pulselink.app",
  "username": "张三",
  "role": "SENIOR",
  "seniorId": "SNR-ABCD1234",
  "createdBy": "caregiver_uid_1",
  "emailVerified": true
}
```

### Firebase Auth User
```
Email: senior_SNR-ABCD1234@pulselink.app
Display Name: 张三|SENIOR
Email Verified: true
UID: firebase_auth_uid
```

---

## ⚠️ 注意事项

### 安全性
- ✅ 密码由 Cloud Function 生成，保证随机性
- ✅ 只有 Caregiver 可以创建老人账户
- ✅ 老人登录时验证角色，防止跨端登录
- ⚠️ 密码通过二维码传输，确保安全环境下使用

### 用户体验
- ✅ 二维码简化登录流程
- ✅ 邮箱地址可读性高（包含虚拟 ID）
- ⚠️ 老人可能不熟悉邮箱概念，需 UI 引导

### 兼容性
- ✅ 保留 `loginSeniorById()` 用于兼容
- ✅ 新老账户可共存
- ⚠️ 建议逐步迁移到新方案

---

## 📚 相关文档
- [AI_INTEGRATION.md](AI_INTEGRATION.md) - AI 功能集成指南
- [DEPLOY_AI.md](DEPLOY_AI.md) - Cloud Functions 部署指南
- [SENIOR_AUTH_FIX.md](SENIOR_AUTH_FIX.md) - 旧的匿名登录方案（已弃用）
