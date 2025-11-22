# AI Integration Guide - PulseLink

## Overview
PulseLink 已集成 Google Gemini AI 通过 Firebase Cloud Functions 实现智能对话功能。

## Architecture

```
┌─────────────────┐
│  Android App    │
│  (Senior Side)  │
└────────┬────────┘
         │ HTTP Callable
         ▼
┌─────────────────┐
│ Cloud Functions │
│   chatWithAI    │
└────────┬────────┘
         │ API Call
         ▼
┌─────────────────┐
│  Gemini 1.5     │
│     Flash       │
└─────────────────┘
```

## Files Structure

### Frontend (Android)
- **UseCase**: `domain/usecase/ChatWithAIUseCase.kt`
  - 封装了对 Firebase Cloud Functions 的调用
  - 处理请求/响应的序列化

- **ViewModel**: `presentation/senior/voice/AssistantViewModel.kt`
  - 管理对话状态
  - 调用 UseCase 获取 AI 回复
  - 自动传递最新健康数据给 AI

- **UI**: `presentation/senior/voice/VoiceAssistantScreen.kt`
  - 聊天界面
  - 支持文本输入和语音输入（语音功能待实现）

### Backend (Cloud Functions)
- **Function**: `functions/src/index.ts`
  - 接收用户消息和健康数据
  - 调用 Gemini API
  - 返回 AI 回复

## Setup Instructions

### 1. Configure Google API Key

在 Firebase Console 中设置密钥：

```bash
# 进入 functions 目录
cd functions

# 设置 Google AI API Key
firebase functions:secrets:set GOOGLE_API_KEY

# 按提示输入你的 API Key (从 https://makersuite.google.com/app/apikey 获取)
```

### 2. Deploy Cloud Functions

```bash
# 确保在 functions 目录
cd functions

# 安装依赖
npm install

# 部署到 Firebase
firebase deploy --only functions
```

### 3. Test Locally (Optional)

```bash
# 在 functions 目录下启动本地模拟器
firebase emulators:start

# 修改 Android 代码连接到本地模拟器
# FirebaseFunctions.getInstance().useEmulator("10.0.2.2", 5001)
```

## Usage Flow

1. **用户输入**: 用户在 VoiceAssistantScreen 输入消息
2. **获取健康数据**: ViewModel 自动获取最新血压数据
3. **调用云函数**: 
   ```kotlin
   chatWithAIUseCase(
       message = "我的血压怎么样？",
       healthData = "Blood Pressure: 120/80 mmHg"
   )
   ```
4. **云函数处理**:
   - 验证用户身份
   - 调用 Gemini API
   - 提供系统人设和健康数据上下文
5. **返回回复**: AI 回复显示在聊天界面

## Features

### Current Features ✅
- ✅ 文本对话
- ✅ 健康数据上下文
- ✅ 错误处理和重试
- ✅ 用户认证

### Planned Features 🚧
- 🚧 语音输入 (Speech-to-Text)
- 🚧 语音输出 (Text-to-Speech)
- 🚧 多轮对话记忆
- 🚧 个性化建议

## Cost Optimization

Gemini 1.5 Flash 配额：
- **免费额度**: 每分钟 15 次请求
- **价格**: 超出后 $0.075 / 1M tokens (input), $0.30 / 1M tokens (output)

优化建议：
1. 使用 `systemInstruction` 限制回复长度（≤100字）
2. 缓存常见问题答案
3. 设置请求频率限制

## Security

1. **认证**: 所有请求需要 Firebase Auth token
2. **密钥管理**: API Key 存储在 Firebase Secrets
3. **输入验证**: 云函数验证用户输入

## Monitoring

查看云函数日志：
```bash
firebase functions:log
```

Firebase Console: 
- Functions 使用情况
- 错误率
- 执行时间

## Troubleshooting

### 常见问题

**Q: 调用失败 "unauthenticated"**
- A: 确保用户已登录 Firebase Auth

**Q: "AI 服务暂时不可用"**
- A: 检查 GOOGLE_API_KEY 是否正确配置
- A: 查看 Firebase Functions 日志了解详细错误

**Q: 响应慢**
- A: Cloud Functions 冷启动需要时间
- A: 考虑使用 Firebase Functions 的 min instances 设置

**Q: 本地测试连接失败**
- A: Android 模拟器使用 `10.0.2.2` 而不是 `localhost`
- A: 确保 Firebase emulators 正在运行

## Example Conversations

```
User: 我的血压怎么样？
AI: 您的血压 120/80 mmHg 处于正常范围内，继续保持良好的生活习惯哦！

User: 我感觉有点头晕
AI: 头晕可能与血压波动有关。建议您：
1. 坐下休息
2. 测量血压
3. 如果持续不适，请联系医生

User: 今天天气怎么样？
AI: 抱歉，我主要帮助您管理健康。关于天气信息，建议查看天气应用。
```

## Next Steps

1. [ ] 部署云函数到生产环境
2. [ ] 在 Android 测试 AI 对话功能
3. [ ] 收集用户反馈
4. [ ] 优化 AI 回复质量
5. [ ] 添加语音输入功能
