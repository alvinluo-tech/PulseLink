# 语音输入改造实现指南

## 已完成的工作

### 1. 数据模型更新
✅ **ChatMessage.kt** - 添加音频消息支持
- `type: MessageType` (TEXT/AUDIO)
- `audioGcsUri: String?` (GCS URI for backend)
- `audioDownloadUrl: String?` (HTTPS URL for playback)
- `duration: Int` (音频时长-秒)

### 2. 音频录制与播放
✅ **AudioRecorder.kt** - 录音器with音量监测
- `startRecording()` - 开始录音
- `getMaxAmplitude()` - 获取当前音量(0~32767)
- `stopRecording()` - 停止并返回文件

✅ **AudioPlayer.kt** - 音频播放器
- `playLocal(file)` - 播放本地文件
- `playUrl(url)` - 播放网络URL
- `isPlaying: StateFlow<Boolean>` - 播放状态

### 3. Firebase Storage集成
✅ **AudioStorageManager.kt** - 音频文件上传
- `uploadAudioFile(file, userId)` - 上传并返回GCS URI + Download URL
- `deleteAudioFile(gcsUri)` - 删除音频
- `getDownloadUrl(gcsUri)` - 获取下载URL

### 4. AI理解语音
✅ **ChatWithAudioUseCase.kt** - 发送音频给AI
- 输入: GCS URI + 健康数据
- AI自动speech-to-text理解语音
- 输出: AI文本回复

### 5. UI组件
✅ **VoiceInputButton.kt** - 波纹动画按钮
- 按住录音
- 音量驱动波纹缩放(1.0~1.5x)
- 录音时按钮变红

✅ **AudioMessageCard.kt** - 音频消息卡片
- 显示时长
- 播放/暂停按钮
- 支持点击播放

### 6. UI State扩展
✅ **AssistantUiState.kt** - 添加音频相关状态
- `isRecording: Boolean` - 录音状态
- `recordingAmplitude: Float` - 归一化音量
- `recordedAudioFile: File?` - 录制的文件
- `playingMessageId: String?` - 当前播放消息ID

### 7. Dependency Injection
✅ **AppModule.kt** - 添加FirebaseStorage provider

## 需要完成的工作

### ViewModel改造 (AssistantViewModel.kt)

#### 1. 注入新依赖
```kotlin
@HiltViewModel
class AssistantViewModel @Inject constructor(
    // ... 现有依赖 ...
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val audioStorageManager: AudioStorageManager,
    private val chatWithAudioUseCase: ChatWithAudioUseCase,
    private val localDataSource: LocalDataSource
) : ViewModel()
```

#### 2. 添加音量监测协程
```kotlin
private var amplitudeJob: Job? = null

fun onMicPressed() {
    Log.d(TAG, "Starting audio recording...")
    
    try {
        val file = audioRecorder.startRecording()
        _uiState.update { 
            it.copy(
                isRecording = true,
                recordedAudioFile = file
            ) 
        }
        
        // 启动音量监测
        amplitudeJob = viewModelScope.launch {
            while (isActive) {
                val maxAmp = audioRecorder.getMaxAmplitude()
                val normalizedAmp = (maxAmp / 32767f).coerceIn(0f, 1f)
                _uiState.update { it.copy(recordingAmplitude = normalizedAmp) }
                delay(100) // 每100ms采样
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to start recording", e)
        _uiState.update { it.copy(error = "Failed to start recording") }
    }
}
```

#### 3. 处理录音完成 - 上传并发送
```kotlin
fun onMicReleased() {
    Log.d(TAG, "Stopping audio recording...")
    
    // 停止音量监测
    amplitudeJob?.cancel()
    
    val audioFile = audioRecorder.stopRecording()
    if (audioFile == null) {
        _uiState.update { 
            it.copy(
                isRecording = false,
                recordingAmplitude = 0f,
                error = "Recording failed"
            ) 
        }
        return
    }
    
    viewModelScope.launch {
        try {
            _uiState.update { 
                it.copy(
                    isRecording = false,
                    recordingAmplitude = 0f,
                    sending = true
                ) 
            }
            
            // 1. 上传到Firebase Storage
            Log.d(TAG, "Uploading audio to Firebase Storage...")
            val userId = localDataSource.getUser()?.first ?: ""
            val uploadResult = audioStorageManager.uploadAudioFile(audioFile, userId)
            
            if (uploadResult.isFailure) {
                throw uploadResult.exceptionOrNull() ?: Exception("Upload failed")
            }
            
            val (gcsUri, downloadUrl) = uploadResult.getOrThrow()
            Log.d(TAG, "Upload successful: $gcsUri")
            
            // 2. 计算音频时长
            val duration = getAudioDuration(audioFile)
            
            // 3. 保存用户音频消息到Firestore
            val userAudioMsg = ChatMessage(
                text = "", // 音频消息无文本
                fromAssistant = false,
                type = MessageType.AUDIO,
                audioGcsUri = gcsUri,
                audioDownloadUrl = downloadUrl,
                duration = duration,
                timestamp = System.currentTimeMillis()
            )
            chatRepository.saveMessage(userAudioMsg)
            
            // 4. 删除本地临时文件
            audioFile.delete()
            
            // 5. 发送给AI理解并回复
            Log.d(TAG, "Sending audio to AI...")
            val healthDataResult = getHealthDataUseCase()
            val healthContext = healthDataResult.getOrNull()?.let { data ->
                "Blood Pressure: \${data.systolic}/\${data.diastolic} mmHg"
            }
            
            val aiResult = chatWithAudioUseCase(gcsUri, healthContext)
            
            if (aiResult.isSuccess) {
                val reply = aiResult.getOrNull() ?: "Sorry, I couldn't understand that."
                
                val aiReply = ChatMessage(
                    text = reply,
                    fromAssistant = true,
                    type = MessageType.TEXT,
                    timestamp = System.currentTimeMillis()
                )
                chatRepository.saveMessage(aiReply)
                
                _uiState.update { it.copy(sending = false) }
            } else {
                throw aiResult.exceptionOrNull() ?: Exception("AI failed")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio", e)
            audioFile?.delete()
            
            val errorMsg = ChatMessage(
                text = "❌ Failed to process audio: \${e.message}",
                fromAssistant = true,
                timestamp = System.currentTimeMillis()
            )
            chatRepository.saveMessage(errorMsg)
            
            _uiState.update { 
                it.copy(
                    sending = false,
                    error = e.message
                ) 
            }
        }
    }
}

private fun getAudioDuration(file: File): Int {
    return try {
        val mediaPlayer = android.media.MediaPlayer()
        mediaPlayer.setDataSource(file.absolutePath)
        mediaPlayer.prepare()
        val durationMs = mediaPlayer.duration
        mediaPlayer.release()
        (durationMs / 1000) // 转换为秒
    } catch (e: Exception) {
        0
    }
}
```

#### 4. 音频播放功能
```kotlin
fun playAudioMessage(messageId: String, downloadUrl: String) {
    viewModelScope.launch {
        try {
            // 停止之前的播放
            if (_uiState.value.playingMessageId != null) {
                audioPlayer.stop()
            }
            
            _uiState.update { it.copy(playingMessageId = messageId) }
            audioPlayer.playUrl(downloadUrl)
            
            // 监听播放完成
            audioPlayer.isPlaying.collect { isPlaying ->
                if (!isPlaying && _uiState.value.playingMessageId == messageId) {
                    _uiState.update { it.copy(playingMessageId = null) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio", e)
            _uiState.update { 
                it.copy(
                    playingMessageId = null,
                    error = "Failed to play audio"
                ) 
            }
        }
    }
}

fun stopAudioPlayback() {
    audioPlayer.stop()
    _uiState.update { it.copy(playingMessageId = null) }
}
```

#### 5. 清理资源
```kotlin
override fun onCleared() {
    super.onCleared()
    amplitudeJob?.cancel()
    audioRecorder.destroy()
    audioPlayer.destroy()
}
```

### UI改造 (VoiceAssistantScreen.kt)

#### 1. 更新ChatMessageItem支持音频
```kotlin
@Composable
private fun ChatMessageItem(
    message: ChatMessage, 
    userAvatarEmoji: String,
    onPlayAudio: (String, String) -> Unit,
    playingMessageId: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = if (message.fromAssistant) Arrangement.Start else Arrangement.End
    ) {
        // ... Avatar ...
        
        // 根据消息类型显示不同内容
        when (message.type) {
            MessageType.TEXT -> {
                Surface(...) {
                    Text(text = message.text, ...)
                }
            }
            MessageType.AUDIO -> {
                AudioMessageCard(
                    duration = message.duration,
                    isPlaying = playingMessageId == message.id,
                    isFromAssistant = message.fromAssistant,
                    onPlayClick = {
                        if (playingMessageId == message.id) {
                            viewModel.stopAudioPlayback()
                        } else {
                            onPlayAudio(message.id, message.audioDownloadUrl ?: "")
                        }
                    }
                )
            }
        }
        
        // ... Avatar ...
    }
}
```

#### 2. 更新BottomBar使用新按钮
```kotlin
@Composable
private fun BottomBar(...) {
    Column {
        // ... 输入框 ...
        
        // 使用新的VoiceInputButton
        VoiceInputButton(
            amplitude = uiState.recordingAmplitude,
            isRecording = uiState.isRecording,
            onPressed = onMicPressed,
            onReleased = onMicReleased,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        // ... 底部导航 ...
    }
}
```

## Cloud Function实现 (functions/src/index.ts)

### chatWithAudio function
```typescript
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { SpeechClient } from "@google-cloud/speech";
import { Anthropic } from "@anthropic-ai/sdk";

const speechClient = new SpeechClient();
const anthropic = new Anthropic({
  apiKey: process.env.ANTHROPIC_API_KEY,
});

export const chatWithAudio = functions.https.onCall(async (data, context) => {
  // 验证认证
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Must be authenticated"
    );
  }

  const { audioGcsUri, healthData } = data;

  try {
    // 1. Speech-to-Text转录音频
    const [operation] = await speechClient.longRunningRecognize({
      config: {
        encoding: "MP3",
        sampleRateHertz: 16000,
        languageCode: "en-US",
      },
      audio: {
        uri: audioGcsUri,
      },
    });

    const [response] = await operation.promise();
    const transcription = response.results
      ?.map((result) => result.alternatives?.[0]?.transcript)
      .join(" ")
      .trim();

    if (!transcription) {
      return {
        success: false,
        error: "No speech detected",
      };
    }

    console.log("Transcription:", transcription);

    // 2. 发送给Claude AI
    const message = await anthropic.messages.create({
      model: "claude-3-5-sonnet-20241022",
      max_tokens: 1024,
      messages: [
        {
          role: "user",
          content: `User's health data: ${healthData}\n\nUser said: ${transcription}\n\nPlease provide a helpful response.`,
        },
      ],
    });

    const reply = message.content[0].text;

    return {
      success: true,
      transcription: transcription,
      reply: reply,
    };
  } catch (error) {
    console.error("Error:", error);
    throw new functions.https.HttpsError("internal", "Processing failed");
  }
});
```

## 测试步骤

1. **权限检查**: 确保RECORD_AUDIO权限已授予
2. **按住录音**: 观察波纹动画跟随音量变化
3. **松开发送**: 音频上传→AI理解→回复
4. **播放回听**: 点击音频卡片播放
5. **查看Firestore**: 确认消息正确保存(包含GCS URI和Download URL)

## 注意事项

- ✅ 音频文件自动删除（上传后）
- ✅ 错误处理完善（上传失败、AI失败）
- ✅ 播放状态管理（一次只播放一个）
- ✅ 资源清理（ViewModel onCleared）
- ⚠️ Cloud Function需要部署chatWithAudio
- ⚠️ Firebase Storage权限配置
