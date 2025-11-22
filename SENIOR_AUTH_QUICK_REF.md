# 老人账户创建与登录 - 快速参考

## 🎯 核心改动

### 账户格式
- **邮箱**: `senior_{虚拟ID}@pulselink.app`
  - 例如: `senior_SNR-ABCD1234@pulselink.app`
- **密码**: 8 位随机密码（或子女自定义）
- **虚拟ID**: 保持 `SNR-XXXXXXXX` 格式（由 Firestore 自动生成）

### 登录方式
1. **扫码登录**（推荐）：扫描子女生成的二维码自动填充
2. **手动输入**：输入邮箱和密码登录

---

## 📝 二维码格式

```json
{
  "type": "pulselink_login",
  "email": "senior_SNR-ABCD1234@pulselink.app",
  "password": "xxxxxxxx"
}
```

---

## 🔧 关键代码位置

### Cloud Function
**文件**: `functions/src/index.ts`
**函数**: `createSeniorAccount`
```typescript
export const createSeniorAccount = onCall(async (request) => {
    const email = `senior_${seniorId}@pulselink.app`;
    const password = password || generateRandomPassword();
    
    const userRecord = await admin.auth().createUser({
        email, password,
        displayName: `${name}|SENIOR`,
        emailVerified: true
    });
    
    return { email, password, uid: userRecord.uid };
});
```

### Android UseCase
**文件**: `app/src/main/java/.../domain/usecase/CreateSeniorUseCase.kt`
```kotlin
suspend operator fun invoke(
    senior: Senior,
    customPassword: String? = null
): Result<SeniorAccountResult> {
    // 1. 创建 Firestore senior 文档
    val createdSenior = seniorRepository.createSenior(senior).getOrThrow()
    
    // 2. 调用 Cloud Function 创建 Firebase Auth 账户
    val result = functions.getHttpsCallable("createSeniorAccount")
        .call(mapOf("seniorId" to createdSenior.id, ...))
        .await()
    
    // 3. 返回账户信息 + 二维码数据
    return Result.success(SeniorAccountResult(...))
}
```

### Android ViewModel
**文件**: `app/src/main/java/.../presentation/auth/AuthViewModel.kt`

**老人登录（邮箱密码）**:
```kotlin
fun loginSenior() {
    loginUseCase(email, password)
    // 验证角色为 SENIOR
}
```

**扫码登录**:
```kotlin
fun parseQRCodeAndLogin(qrCodeData: String) {
    val (email, password) = parseJSON(qrCodeData)
    loginSenior()
}
```

---

## ✅ 测试清单

### 部署 Cloud Function
- [ ] `cd functions && npm run build`
- [ ] `firebase deploy --only functions:createSeniorAccount`
- [ ] 检查 Firebase Console 中函数是否部署成功

### Caregiver 端测试
- [ ] 登录 Caregiver 账户
- [ ] 创建老人账户（填写姓名、年龄等）
- [ ] 验证返回邮箱格式: `senior_SNR-XXX@pulselink.app`
- [ ] 验证返回密码（8 位）
- [ ] 验证二维码数据包含 email 和 password
- [ ] 检查 Firestore `seniors/{id}` 文档
- [ ] 检查 Firebase Auth 是否有对应用户
- [ ] 检查 Firestore `users/{uid}` 文档

### Senior 端测试（手动登录）
- [ ] 输入邮箱: `senior_SNR-XXX@pulselink.app`
- [ ] 输入密码
- [ ] 点击登录
- [ ] 验证登录成功
- [ ] 验证跳转到老人端首页

### Senior 端测试（扫码登录）
- [ ] 点击"扫码登录"
- [ ] 扫描二维码或手动输入二维码数据
- [ ] 验证自动填充邮箱和密码
- [ ] 验证自动登录

### AI 对话测试
- [ ] 老人端登录成功
- [ ] 进入语音助手
- [ ] 发送消息："你好"
- [ ] 验证收到 AI 回复
- [ ] 验证没有 UNAUTHENTICATED 错误

---

## 🐛 常见问题

### 1. Cloud Function 调用失败
**错误**: "Failed to create Firebase Auth account"
**原因**: Cloud Function 未部署或权限问题
**解决**: 
```bash
firebase deploy --only functions:createSeniorAccount
```

### 2. 老人登录失败
**错误**: "此账户不是老人账户"
**原因**: 角色验证失败
**解决**: 检查 Firestore `users/{uid}` 中 `role` 字段是否为 "SENIOR"

### 3. 二维码解析失败
**错误**: "二维码格式不正确"
**原因**: JSON 格式错误
**解决**: 确保二维码数据为:
```json
{"type":"pulselink_login","email":"...","password":"..."}
```

### 4. UNAUTHENTICATED 错误
**错误**: Cloud Function 返回 UNAUTHENTICATED
**原因**: Firebase Auth 未登录
**解决**: 确保使用 `loginSenior()` 而不是 `loginSeniorById()`

---

## 📄 完整文档
详细说明请参阅: [SENIOR_AUTH_NEW.md](SENIOR_AUTH_NEW.md)
