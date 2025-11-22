# AI Chat Troubleshooting Guide

## 🔍 如何查看日志

### Android 端日志

使用 Android Studio 的 Logcat 或命令行：

```bash
# 过滤 AI 相关日志
adb logcat -s AssistantViewModel ChatWithAIUseCase

# 或者查看所有日志
adb logcat | grep -E "AssistantViewModel|ChatWithAIUseCase"
```

### Firebase Functions 日志

```bash
# 实时查看云函数日志
firebase functions:log --only chatWithAI

# 或在 Firebase Console 查看
# https://console.firebase.google.com/project/YOUR_PROJECT/functions/logs
```

## ❌ 常见错误及解决方案

### 1. "unauthenticated" 错误

**错误信息**: 
```
FirebaseFunctionsException: UNAUTHENTICATED
```

**原因**: 用户未登录 Firebase Auth

**解决方案**:
1. 确保用户已登录
2. 检查 `FirebaseAuth.getInstance().currentUser` 不为 null
3. 重新登录应用

**检查代码**:
```kotlin
val currentUser = FirebaseAuth.getInstance().currentUser
Log.d(TAG, "Current user: ${currentUser?.uid}")
```

---

### 2. "INTERNAL" 错误 - AI 服务不可用

**错误信息**:
```
FirebaseFunctionsException: INTERNAL
Message: AI 服务暂时不可用
```

**原因**: 云函数内部错误

**可能原因**:
- ❌ Google API Key 未配置或无效
- ❌ Gemini API 配额超限
- ❌ 云函数代码错误

**解决方案**:

1. **检查 API Key**:
```bash
firebase functions:secrets:access GOOGLE_API_KEY
```

2. **查看云函数日志**:
```bash
firebase functions:log --only chatWithAI
```

3. **验证 API Key**:
   - 访问 https://makersuite.google.com/app/apikey
   - 确认 Key 有效且未过期

4. **重新设置密钥**:
```bash
firebase functions:secrets:set GOOGLE_API_KEY
```

---

### 3. "NOT_FOUND" 错误 - 云函数未找到

**错误信息**:
```
FirebaseFunctionsException: NOT_FOUND
```

**原因**: 云函数未部署或名称错误

**解决方案**:

1. **确认部署状态**:
```bash
firebase functions:list
```

应该看到 `chatWithAI` 在列表中

2. **重新部署**:
```bash
cd functions
npm install
firebase deploy --only functions
```

3. **检查函数名称**:
   - 确保 `index.ts` 中是 `export const chatWithAI`
   - 确保 Android 调用的是 `"chatWithAI"`

---

### 4. "DEADLINE_EXCEEDED" 错误 - 超时

**错误信息**:
```
FirebaseFunctionsException: DEADLINE_EXCEEDED
```

**原因**: 云函数执行超过 60 秒

**解决方案**:
1. 检查网络连接
2. Gemini API 可能响应慢
3. 增加超时时间（在 index.ts 中已设置 60 秒）

---

### 5. "PERMISSION_DENIED" 错误

**错误信息**:
```
FirebaseFunctionsException: PERMISSION_DENIED
```

**原因**: Firebase 项目配置问题

**解决方案**:
1. 检查 `google-services.json` 是否正确
2. 确认 Firebase 项目启用了 Cloud Functions
3. 检查 Firebase 账单是否正常（Functions 需要 Blaze 计划）

---

## 🔧 详细调试步骤

### Step 1: 检查用户登录状态

在 `AssistantViewModel` 查看日志：

```
D/AssistantViewModel: Sending message: hello
D/AssistantViewModel: Fetching health data...
D/AssistantViewModel: Health context: Blood Pressure: 120/80 mmHg
D/AssistantViewModel: Calling AI cloud function...
```

### Step 2: 检查 UseCase 日志

在 `ChatWithAIUseCase` 查看：

```
D/ChatWithAIUseCase: === ChatWithAI Start ===
D/ChatWithAIUseCase: Message: hello
D/ChatWithAIUseCase: Health Data: Blood Pressure: 120/80 mmHg
D/ChatWithAIUseCase: Request data: {text=hello, healthData=Blood Pressure: 120/80 mmHg}
D/ChatWithAIUseCase: Calling cloud function: chatWithAI
```

### Step 3: 如果卡在这里

说明调用云函数失败，可能原因：
- 网络问题
- 云函数未部署
- Firebase 配置错误

**检查网络**:
```bash
adb shell ping 8.8.8.8
```

**检查 Firebase 连接**:
```kotlin
Firebase.auth.currentUser?.let {
    Log.d(TAG, "User UID: ${it.uid}")
    Log.d(TAG, "User Email: ${it.email}")
}
```

### Step 4: 查看云函数日志

```bash
firebase functions:log --only chatWithAI --limit 50
```

期望看到：
```
Function execution started
Received request from user: xxx
Calling Gemini API...
Gemini response received
Function execution completed
```

### Step 5: 如果看到错误

**Gemini API 错误**:
```
ERROR: Gemini API error: API key not valid
```
→ 重新配置 API Key

**权限错误**:
```
ERROR: Permission denied
```
→ 检查 Firebase Auth 配置

---

## 📱 在 UI 中显示的错误信息

现在 AI 会返回详细错误信息：

```
❌ Error occurred:

Type: FirebaseFunctionsException
Message: UNAUTHENTICATED

Please check:
1. Firebase Authentication (are you logged in?)
2. Cloud Function deployed?
3. API Key configured?
4. Internet connection?
```

这可以帮助快速定位问题！

---

## ✅ 成功的日志示例

**Android Logcat**:
```
D/AssistantViewModel: Sending message: 我的血压怎么样？
D/AssistantViewModel: Fetching health data...
D/AssistantViewModel: Health context: Blood Pressure: 120/80 mmHg
D/AssistantViewModel: Calling AI cloud function...
D/ChatWithAIUseCase: === ChatWithAI Start ===
D/ChatWithAIUseCase: Message: 我的血压怎么样？
D/ChatWithAIUseCase: Calling cloud function: chatWithAI
D/ChatWithAIUseCase: Cloud function returned
D/ChatWithAIUseCase: Response: {success=true, reply=您的血压120/80 mmHg处于正常范围...}
D/ChatWithAIUseCase: Success: true, Reply: 您的血压120/80 mmHg处于正常范围...
D/ChatWithAIUseCase: === ChatWithAI Success ===
D/AssistantViewModel: AI Reply: 您的血压120/80 mmHg处于正常范围...
```

**Firebase Functions Log**:
```
Function execution started
Message: 我的血压怎么样？
Health Data: Blood Pressure: 120/80 mmHg
Calling Gemini API...
Gemini response: 您的血压120/80 mmHg处于正常范围...
Function execution completed
```

---

## 🚀 快速诊断命令

一键检查所有可能的问题：

```bash
# 1. 检查用户登录
adb logcat -d | grep "Current user"

# 2. 检查云函数调用
adb logcat -d | grep "Calling cloud function"

# 3. 检查云函数响应
adb logcat -d | grep "Cloud function returned"

# 4. 查看错误
adb logcat -d | grep -E "ERROR|Exception"

# 5. 检查云函数日志
firebase functions:log --only chatWithAI --limit 10
```

---

## 💡 开发建议

1. **始终查看 Logcat** - 详细的日志会告诉你问题所在
2. **检查 Firebase Console** - Functions、Auth、Billing 状态
3. **验证 API Key** - 确保 Google AI Studio 的 Key 有效
4. **测试网络** - 确保设备能访问 Firebase 和 Google API
5. **查看错误消息** - UI 现在会显示详细的错误信息

---

## 📞 还是解决不了？

提供以下信息以便诊断：

1. **完整的 Logcat 日志** (包含 AssistantViewModel 和 ChatWithAIUseCase)
2. **Firebase Functions 日志** (最近 10 条)
3. **错误截图** (UI 显示的错误信息)
4. **部署状态**: `firebase functions:list` 的输出
5. **用户状态**: 是否已登录？UID 是什么？

有了这些信息，就能快速找到问题根源！🎯
