# Voice Input Debugging Guide

## 问题：长按麦克风没有反应

### 可能的原因

1. **权限问题**
   - 检查是否授予了 RECORD_AUDIO 权限
   - 在 Logcat 中查找 "AssistantViewModel" 和 "SpeechRecognizer" 标签

2. **Google 语音服务未安装**
   - 需要设备安装 Google app 或 Google Speech Services
   - 检查 SpeechRecognizer.isRecognitionAvailable(context)

3. **手势检测问题**
   - `pointerInput` 可能与其他手势冲突
   - 检查 Logcat 中是否有 "onMicPressed" 日志

### 调试步骤

#### 1. 检查权限
在 VoiceAssistantScreen 加载时，查看 Logcat：
```
hasAudioPermission = true/false
```

#### 2. 查看日志输出
```bash
adb logcat | grep -E "AssistantViewModel|SpeechRecognizer"
```

应该看到：
```
AssistantViewModel: onMicPressed() called - starting speech recognition
SpeechRecognizer: Started listening
SpeechRecognizer: Ready for speech
```

#### 3. 检查语音服务
```kotlin
if (!SpeechRecognizer.isRecognitionAvailable(context)) {
    Log.e(TAG, "Speech recognition not available on this device")
}
```

### 解决方案

#### 方案 1：添加调试日志
在 `VoiceAssistantScreen.kt` 的 `onMicPressed` 中添加：
```kotlin
onMicPressed = {
    Log.d("VoiceDebug", "Mic button pressed, hasPermission=$hasAudioPermission")
    if (hasAudioPermission) {
        viewModel.onMicPressed()
    } else {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}
```

#### 方案 2：添加 Toast 提示
```kotlin
import android.widget.Toast

onMicPressed = {
    Toast.makeText(context, "Mic pressed!", Toast.LENGTH_SHORT).show()
    if (hasAudioPermission) {
        viewModel.onMicPressed()
    } else {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}
```

#### 方案 3：检查设备兼容性
某些设备可能需要额外的权限或设置：
- 检查是否安装了 Google app
- 检查语音输入语言设置
- 尝试在设备设置中测试语音输入

### 常见错误

1. **ERROR_RECOGNIZER_BUSY**
   - 之前的识别会话未正确关闭
   - 解决：在 ViewModel 的 onCleared() 中调用 destroy()

2. **ERROR_NO_MATCH**
   - 没有检测到语音
   - 正常现象，不影响功能

3. **ERROR_NETWORK**
   - 需要网络连接
   - 检查设备网络状态

4. **ERROR_INSUFFICIENT_PERMISSIONS**
   - 权限未授予
   - 检查 AndroidManifest.xml 和运行时权限

### 测试清单

- [ ] 检查 Logcat 日志
- [ ] 验证权限已授予
- [ ] 测试 Toast 提示是否显示
- [ ] 检查 Google 语音服务是否可用
- [ ] 测试在不同网络环境下
- [ ] 测试说话后是否有文字上屏
- [ ] 测试监听指示器是否显示

### 临时解决方案

如果长按手势有问题，可以改为点击触发：

```kotlin
Box(
    modifier = Modifier
        .size(60.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(if (listening) Color(0xFF1976D2) else Color(0xFF448AFF))
        .clickable {
            if (!listening) {
                onMicPressed()
            } else {
                onMicReleased()
            }
        },
    contentAlignment = Alignment.Center
) {
    // Icon...
}
```

### 联系支持

如果问题persist，提供以下信息：
1. 设备型号和 Android 版本
2. Logcat 完整日志
3. 是否安装了 Google app
4. 权限授予截图
