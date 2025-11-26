# Voice Input Implementation

## 功能说明
为语音助手添加了 Android 原生语音识别功能，老人可以按住麦克风按钮说话，识别的文字会自动显示在输入框中。

## 实现细节

### 1. SpeechRecognizerManager
- **位置**: `data/speech/SpeechRecognizerManager.kt`
- **功能**:
  - 封装 Android SpeechRecognizer API
  - 提供 Flow 接口实时返回识别结果
  - 支持部分结果（边说边显示）
  - 完整的错误处理

### 2. 权限管理
- **新增权限**: `RECORD_AUDIO` (已添加到 AndroidManifest.xml)
- **运行时权限请求**: 首次使用时自动请求
- **权限拒绝处理**: 显示提示对话框

### 3. UI 交互
- **按住说话**: 使用 `pointerInput` + `detectTapGestures` 实现长按
- **视觉反馈**:
  - 按下时麦克风按钮颜色变深
  - 顶部显示 "Listening... Release to stop" 提示条
- **自动上屏**: 识别结果实时显示在输入框

### 4. ViewModel 集成
- 注入 `SpeechRecognizerManager`
- 监听识别结果并更新 `inputText`
- 监听识别状态更新 `listening` 标志
- `onMicPressed()`: 开始识别
- `onMicReleased()`: 停止识别
- `onCleared()`: 清理资源

## 使用流程

1. **按住麦克风按钮** → 开始录音
2. **说话** → 实时显示识别文字
3. **松开按钮** → 停止录音，文字留在输入框
4. **点击发送** → 发送消息给 AI

## 技术特性

✅ **实时识别**: 使用 `EXTRA_PARTIAL_RESULTS` 边说边显示
✅ **错误处理**: 网络错误、无匹配、超时等全覆盖
✅ **资源管理**: ViewModel 销毁时自动清理
✅ **权限友好**: 首次使用时请求，拒绝后提示
✅ **英文识别**: 配置为 `en-US` 语言模型

## 配置说明

### 语言设置
当前设置为英文识别 (`en-US`)，如需支持中文：
```kotlin
putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
```

### 识别模式
使用自由形式模型 (`LANGUAGE_MODEL_FREE_FORM`)，适合对话场景

## 测试建议

1. ✅ 测试权限请求流程
2. ✅ 测试按住说话，松开停止
3. ✅ 测试识别结果是否正确上屏
4. ✅ 测试网络错误时的提示
5. ✅ 测试无声音输入的处理
6. ✅ 测试多次连续使用

## 注意事项

⚠️ **设备要求**: 需要设备支持 Google 语音识别服务
⚠️ **网络依赖**: 语音识别需要网络连接
⚠️ **权限必需**: 必须授予录音权限才能使用
⚠️ **语言包**: 首次使用可能需要下载语言包

## 相关文件

### 新增
- `data/speech/SpeechRecognizerManager.kt`
- `util/PermissionUtil.kt`
- `VOICE_INPUT.md` (本文件)

### 修改
- `AndroidManifest.xml` - 添加 RECORD_AUDIO 权限
- `presentation/senior/voice/AssistantViewModel.kt` - 集成语音识别
- `presentation/senior/voice/VoiceAssistantScreen.kt` - 更新 UI 交互
