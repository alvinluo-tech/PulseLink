# 🚀 语音输入功能部署指南

## 📋 部署清单

### 1️⃣ 部署Cloud Function
```bash
cd functions
npm install
firebase deploy --only functions:onNewChatMessage
```

**验证部署**：
- 在Firebase Console → Functions查看`onNewChatMessage`函数
- 检查触发器配置：`chat_history/{userId}/messages/{messageId}`
- 查看日志确认没有错误

### 2️⃣ 部署Storage安全规则
```bash
firebase deploy --only storage
```

**验证规则**：
- 在Firebase Console → Storage → Rules查看规则
- 确认`voice_messages/{userId}/{fileName}`路径已配置
- 测试上传权限

### 3️⃣ 安装Android应用
```bash
.\gradlew installDebug
```

## 🧪 测试流程

### 端到端测试
1. **录音测试**
   - 打开Voice Assistant页面
   - 按住麦克风按钮说话
   - 观察波纹动画是否根据音量变化
   - 松开按钮

2. **上传验证**
   - 检查Firebase Storage Console
   - 路径：`voice_messages/{userId}/{timestamp}.m4a`
   - 确认文件已上传

3. **Firestore验证**
   - 检查Firestore Console
   - 集合：`chat_history/{userId}/messages`
   - 查看新消息文档包含：
     ```
     type: "audio"
     audioGcsUri: "gs://..."
     audioDownloadUrl: "https://..."
     duration: 数字
     fromAssistant: false
     timestamp: 时间戳
     ```

4. **Cloud Function触发**
   - 在Functions日志中查看触发记录
   - 确认Gemini API调用成功
   - 检查AI回复是否写入Firestore

5. **AI回复显示**
   - 等待几秒（Gemini处理时间）
   - 在聊天界面查看AI文字回复
   - 确认回复内容相关

6. **音频回放测试**
   - 点击已发送的音频消息
   - 确认播放控件变化（播放→暂停）
   - 验证音频正常播放

## 🔧 配置检查

### Firebase配置
```bash
# 检查当前项目
firebase projects:list

# 确认使用正确项目
firebase use <project-id>

# 查看配置
firebase functions:config:get
```

### Gemini API密钥
```bash
# 设置密钥（如果还没设置）
firebase functions:secrets:set GOOGLE_API_KEY

# 验证密钥
firebase functions:secrets:access GOOGLE_API_KEY
```

### Storage配置
- 确认Storage Bucket已启用
- 路径：Firebase Console → Storage
- 区域：选择合适的区域（如asia-east1）

## 🐛 常见问题

### 问题1：上传失败
**症状**：`audioStorageManager.uploadAudioFile`返回失败
**解决**：
1. 检查Storage规则是否部署
2. 验证用户已登录（`request.auth != null`）
3. 检查文件大小是否超过10MB

### 问题2：Cloud Function未触发
**症状**：消息写入Firestore但没有AI回复
**解决**：
1. 检查Function是否部署成功
2. 查看Functions日志：`firebase functions:log`
3. 确认文档路径匹配：`chat_history/{userId}/messages/{messageId}`
4. 检查消息类型：`type: "audio"`, `fromAssistant: false`

### 问题3：Gemini调用失败
**症状**：Function触发但AI回复失败
**解决**：
1. 验证API密钥有效
2. 检查GCS URI格式：`gs://bucket-name/path/to/file.m4a`
3. 查看详细错误日志
4. 确认Gemini API配额未超限

### 问题4：音频无法播放
**症状**：点击音频消息无反应
**解决**：
1. 检查`audioDownloadUrl`是否有效
2. 验证Storage读取权限
3. 测试MediaPlayer兼容性
4. 检查网络连接

## 📊 监控

### 实时日志
```bash
# Cloud Functions日志
firebase functions:log --only onNewChatMessage

# 实时日志流
firebase functions:log --only onNewChatMessage --follow
```

### Android日志
```bash
# 过滤AssistantViewModel日志
adb logcat -s AssistantViewModel

# 过滤所有语音相关日志
adb logcat | grep -E "(Audio|Voice|Recording)"
```

## 🎯 性能优化

### Storage优化
- 考虑添加生命周期规则（自动删除旧文件）
- Firebase Console → Storage → Lifecycle
- 示例：30天后删除

### Cloud Function优化
- 监控执行时间和内存使用
- 考虑增加超时时间（当前60秒）
- 优化Gemini提示词以减少响应时间

### 客户端优化
- 实现音频压缩（减少上传大小）
- 添加本地缓存（已播放音频）
- 优化振幅采样频率（平衡流畅度和性能）

## ✅ 部署完成检查清单

- [ ] Cloud Function已部署
- [ ] Storage规则已部署  
- [ ] Android应用已安装
- [ ] 录音功能正常
- [ ] 波纹动画正常
- [ ] 文件上传成功
- [ ] Firestore写入成功
- [ ] Cloud Function触发成功
- [ ] AI回复正常显示
- [ ] 音频回放正常
- [ ] 错误处理正常
- [ ] 日志输出清晰

## 🎉 部署命令一键执行

```bash
# 部署所有Firebase资源
firebase deploy --only functions:onNewChatMessage,storage

# 安装Android应用
.\gradlew installDebug

# 查看实时日志
firebase functions:log --follow
```
