# PulseLink 架构风险评估和改进方案

## 🔴 高优先级（P0 - 立即修复）

### 1. SNR-ID 唯一性保证 ✅ **已修复**

**当前问题**：
```kotlin
private fun generateSeniorId(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    val randomPart = (1..8).map { chars.random() }.joinToString("")
    return "SNR-$randomPart"
}
```
- ❌ 无唯一性检查
- ❌ 可能产生 ID 碰撞
- ❌ Firestore `.set()` 会覆盖已有数据

**✅ 已采用解决方案 C**：使用时间戳 + 随机数
```kotlin
/**
 * 生成唯一的 Senior ID (SNR-XXXXXXXXXXXX)
 * 使用时间戳 + 随机数保证唯一性
 */
private fun generateSeniorId(): String {
    // 时间戳转36进制并转大写（36进制：0-9 + A-Z）
    val timestamp = System.currentTimeMillis().toString(36).uppercase()
    
    // 4位随机大写字母
    val random = (1..4).map { ('A'..'Z').random() }.joinToString("")
    
    // 拼接并取后12位（确保长度一致性）
    val combined = timestamp + random
    return "SNR-${combined.takeLast(12)}"
}
```

**修改内容**：
- ✅ 创建 `AuthConstants.kt` 统一管理常量和正则表达式
- ✅ 更新 `AuthRepositoryImpl.generateSeniorId()` 使用新算法
- ✅ 更新所有正则验证：`^SNR-[A-Z0-9]{8}$` → `^SNR-[A-Z0-9]{12}$`
- ✅ 更新虚拟邮箱生成逻辑使用 `AuthConstants`
- ✅ 更新字符串资源中的提示信息
- ✅ 更新 Firebase Functions 验证逻辑
- ✅ 添加单元测试验证唯一性和格式正确性

**受影响的文件**：
- `AuthConstants.kt` (新建)
- `AuthRepositoryImpl.kt`
- `AuthViewModel.kt`
- `LinkSeniorViewModel.kt`
- `functions/src/index.ts`
- `values/strings.xml`
- `values-zh/strings.xml`

**测试文件**：
- `AuthConstantsTest.kt` - 常量和正则表达式验证
- `SeniorIdGenerationTest.kt` - 唯一性测试（100次、1000次生成无重复）

---

### 2. 密码安全存储

**当前问题**：
```kotlin
"password" to password,  // ❌ 明文存储
```

**解决方案**：移除密码存储，改用其他方式生成二维码

**方案 A - 使用 JWT Token（推荐）**：
```kotlin
// 注册时不存储密码
"password" to "",  // 或直接移除此字段

// 生成二维码时创建临时 token
private suspend fun generateQRCodeToken(seniorId: String): String {
    val tokenDoc = hashMapOf(
        "seniorId" to seniorId,
        "createdAt" to System.currentTimeMillis(),
        "expiresAt" to System.currentTimeMillis() + 300_000, // 5分钟有效期
        "used" to false
    )
    
    val tokenRef = firestore.collection("qr_tokens").document()
    tokenRef.set(tokenDoc).await()
    
    return tokenRef.id
}

// 二维码内容
val qrCodeData = """
{
    "type": "pulselink_login",
    "token": "${generateQRCodeToken(seniorId)}"
}
""".trimIndent()
```

**方案 B - 使用 Firebase Custom Token**：
```kotlin
// 后端 Cloud Function 生成
exports.generateSeniorLoginToken = functions.https.onCall(async (data, context) => {
    const seniorId = data.seniorId;
    const token = await admin.auth().createCustomToken(seniorId);
    return { token };
});
```

---

### 3. 虚拟邮箱配置化

**当前问题**：
```kotlin
"senior_${email}@pulselink.app"  // ❌ 硬编码
```

**解决方案**：配置化管理
```kotlin
// BuildConfig 或配置文件
object AppConfig {
    const val VIRTUAL_EMAIL_DOMAIN = "pulselink.app"
    const val VIRTUAL_EMAIL_PREFIX = "senior_"
    
    fun generateVirtualEmail(seniorId: String): String {
        return "$VIRTUAL_EMAIL_PREFIX$seniorId@$VIRTUAL_EMAIL_DOMAIN"
    }
}

// 使用
val loginEmail = if (email.matches(Regex("^SNR-[A-Z0-9]{8}$"))) {
    AppConfig.generateVirtualEmail(email)
} else {
    email
}
```

---

## 🟡 中优先级（P1 - 2周内完成）

### 4. 老人自主注册后的关联流程

**缺失功能**：老人自己注册后，如何让 Caregiver 绑定？

**解决方案 - 邀请码系统**：

```kotlin
// 1. 老人注册后生成邀请码
data class InvitationCode(
    val code: String,              // 6位邀请码
    val seniorId: String,
    val createdBy: String,         // Senior 的 authUid
    val expiresAt: Long,           // 过期时间
    val maxUses: Int = 5,          // 最多可用次数
    val usedCount: Int = 0,
    val active: Boolean = true
)

// 2. Caregiver 输入邀请码申请绑定
suspend fun applyWithInvitationCode(code: String, relationship: String) {
    val invitation = firestore.collection("invitations")
        .whereEqualTo("code", code)
        .whereEqualTo("active", true)
        .get()
        .await()
        .documents
        .firstOrNull() ?: throw Exception("Invalid invitation code")
    
    // 创建 LinkRequest
    val request = LinkRequest(
        seniorId = invitation.getString("seniorId")!!,
        requesterId = currentUserId,
        relationship = relationship,
        invitationCode = code
    )
    
    // ... 发送审批请求
}
```

**或者 - 扫码绑定**：
```kotlin
// 老人展示个人二维码
val qrData = """
{
    "type": "senior_profile",
    "seniorId": "$seniorId",
    "name": "$name",
    "timestamp": ${System.currentTimeMillis()}
}
"""

// Caregiver 扫码后发起绑定请求
```

---

### 5. 数据一致性保证（事务处理）

**当前问题**：`users` 和 `seniors` 两个文档可能不同步

**解决方案 - 使用 Firestore Batch**：
```kotlin
override suspend fun registerSenior(...): Result<Unit> {
    return try {
        // 1. 创建 Auth 账号
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("User creation failed")
        
        val seniorId = generateUniqueSeniorId()
        
        // 2. 使用 Batch 写入（原子操作）
        val batch = firestore.batch()
        
        val userRef = firestore.collection("users").document(user.uid)
        batch.set(userRef, hashMapOf(
            "uid" to user.uid,
            "seniorId" to seniorId,
            "role" to "SENIOR",
            // ...
        ))
        
        val seniorRef = firestore.collection("seniors").document(seniorId)
        batch.set(seniorRef, hashMapOf(
            "id" to seniorId,
            "authUid" to user.uid,  // ⭐ 添加反向引用
            // ...
        ))
        
        // 提交批量操作（要么全成功，要么全失败）
        batch.commit().await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        // 失败时删除已创建的 Auth 账号
        firebaseAuth.currentUser?.delete()?.await()
        Result.failure(e)
    }
}
```

---

### 6. 统一常量管理

**当前问题**：正则表达式、字符串分散在多处

**解决方案**：
```kotlin
object AuthConstants {
    // 正则表达式
    val SNR_ID_REGEX = Regex("^SNR-[A-Z0-9]{8}$")
    val EMAIL_REGEX = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    
    // 虚拟邮箱
    const val VIRTUAL_EMAIL_DOMAIN = "pulselink.app"
    const val VIRTUAL_EMAIL_PREFIX = "senior_"
    
    // 注册类型
    const val REG_TYPE_SELF = "SELF_REGISTERED"
    const val REG_TYPE_CAREGIVER = "CAREGIVER_CREATED"
    
    // 字段名
    object Fields {
        const val REGISTRATION_TYPE = "registrationType"
        const val SENIOR_ID = "seniorId"
        const val CAREGIVER_IDS = "caregiverIds"
    }
    
    // 错误消息（使用 string resources）
    // R.string.error_invalid_snr_id
    // R.string.error_email_format
}

// 使用
if (email.matches(AuthConstants.SNR_ID_REGEX)) {
    // ...
}
```

---

## 🟢 低优先级（P2 - 后续优化）

### 7. UI 响应式设计

```kotlin
// 使用 Material3 的自适应组件
@Composable
fun AdaptiveTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (isTablet) 80.dp else 70.dp),
        textStyle = LocalTextStyle.current.copy(
            fontSize = if (isTablet) 18.sp else 16.sp
        )
    )
}
```

---

### 8. 完全国际化

```kotlin
// AuthViewModel.kt
_uiState.update {
    it.copy(
        error = context.getString(
            if (role == UserRole.SENIOR) 
                R.string.error_senior_credentials_required
            else 
                R.string.error_credentials_required
        )
    )
}
```

```xml
<!-- values/strings.xml -->
<string name="error_senior_credentials_required">Please enter account ID and password</string>

<!-- values-zh/strings.xml -->
<string name="error_senior_credentials_required">请输入账号ID和密码</string>
```

---

### 9. Firestore Rules 简化

**考虑使用 Cloud Functions 处理复杂逻辑**：
```javascript
// firestore.rules - 简化
allow create: if isAuthenticated() && isValidSeniorCreation();

// Cloud Function - 复杂验证
exports.validateSeniorCreation = functions.firestore
    .document('seniors/{seniorId}')
    .onCreate(async (snap, context) => {
        const data = snap.data();
        
        // 复杂的业务逻辑验证
        if (data.registrationType === 'SELF_REGISTERED') {
            // 验证自主注册规则
        } else {
            // 验证 Caregiver 创建规则
        }
        
        // 如果验证失败，删除文档并发送通知
    });
```

---

## 📊 风险评估总结

| 问题 | 风险等级 | 影响范围 | 修复难度 | 优先级 |
|------|---------|---------|---------|--------|
| SNR-ID 碰撞 | 🔴 高 | 数据安全 | 中 | P0 |
| 密码明文存储 | 🔴 高 | 安全合规 | 中 | P0 |
| 虚拟邮箱硬编码 | 🟡 中 | 可维护性 | 低 | P0 |
| 自主注册关联流程缺失 | 🟡 中 | 功能完整性 | 高 | P1 |
| 双ID数据一致性 | 🟡 中 | 数据完整性 | 中 | P1 |
| Rules 复杂度 | 🟡 中 | 可维护性 | 高 | P2 |
| 验证逻辑分散 | 🟢 低 | 代码质量 | 低 | P1 |
| UI 硬编码 | 🟢 低 | 可访问性 | 低 | P2 |
| 错误消息混合语言 | 🟢 低 | 国际化 | 低 | P2 |

---

## 🎯 建议的实施路径

### 阶段 1（本周完成）
1. ✅ 修复 SNR-ID 唯一性问题
2. ✅ 移除密码明文存储
3. ✅ 虚拟邮箱配置化

### 阶段 2（2周内完成）
4. ✅ 实现邀请码/扫码绑定流程
5. ✅ 使用 Firestore Batch 保证数据一致性
6. ✅ 统一常量管理

### 阶段 3（后续迭代）
7. ✅ UI 响应式优化
8. ✅ 完全国际化
9. ✅ Firestore Rules 重构

---

## 📝 测试清单

### 单元测试
- [ ] SNR-ID 生成唯一性测试（100次循环）
- [ ] 邮箱/SNR-ID 正则验证测试
- [ ] 虚拟邮箱转换测试

### 集成测试
- [ ] 老人自主注册完整流程
- [ ] Caregiver 创建老人账号流程
- [ ] 邀请码绑定流程
- [ ] 登录（邮箱/SNR-ID）双路径测试

### 安全测试
- [ ] Firestore Rules 单元测试
- [ ] 并发注册测试（ID 碰撞检测）
- [ ] 过期 token 测试

---

## 参考资料

- [Firestore Transactions and Batched Writes](https://firebase.google.com/docs/firestore/manage-data/transactions)
- [Firebase Security Rules Testing](https://firebase.google.com/docs/rules/unit-tests)
- [Material Design 3 - Accessibility](https://m3.material.io/foundations/accessible-design)
