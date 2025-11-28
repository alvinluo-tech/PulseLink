package com.alvin.pulselink.presentation.senior.voice

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.data.speech.AudioRecorderManager
import com.alvin.pulselink.domain.model.ChatMessage
import com.alvin.pulselink.domain.model.MessageType
import com.alvin.pulselink.domain.repository.ChatRepository
import com.alvin.pulselink.domain.repository.SeniorProfileRepository
import com.alvin.pulselink.domain.usecase.ChatWithAIUseCase
import com.alvin.pulselink.domain.usecase.GetHealthDataUseCase
import com.alvin.pulselink.domain.usecase.VoiceToTextUseCase
import com.alvin.pulselink.util.AvatarHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val chatWithAIUseCase: ChatWithAIUseCase,
    private val getHealthDataUseCase: GetHealthDataUseCase,
    private val chatRepository: ChatRepository,
    private val audioRecorderManager: AudioRecorderManager,
    private val voiceToTextUseCase: VoiceToTextUseCase,
    private val seniorProfileRepository: SeniorProfileRepository,
    private val firebaseAuth: com.google.firebase.auth.FirebaseAuth,
    @ApplicationContext private val context: Context
): ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()
    
    private val audioRecorder = com.alvin.pulselink.data.speech.AudioRecorder(context)
    private val audioPlayer = com.alvin.pulselink.data.speech.AudioPlayer(context)
    private var amplitudeJob: kotlinx.coroutines.Job? = null
    
    companion object {
        private const val TAG = "AssistantViewModel"
    }
    
    init {
        loadChatHistory()
        observeRecordingState()
        loadUserAvatar()
        observeAudioPlayback()
    }
    
    private fun observeRecordingState() {
        viewModelScope.launch {
            // 监听录音状态
            audioRecorderManager.isRecording.collect { isRecording ->
                _uiState.update { it.copy(listening = isRecording) }
            }
        }
        
        viewModelScope.launch {
            // 监听错误
            audioRecorderManager.error.collect { error ->
                if (error != null) {
                    _uiState.update { it.copy(error = error) }
                }
            }
        }
    }
    
    private fun observeAudioPlayback() {
        viewModelScope.launch {
            audioPlayer.isPlaying.collect { isPlaying ->
                if (!isPlaying) {
                    _uiState.update { it.copy(playingMessageId = null) }
                }
            }
        }
    }
    
    private fun loadChatHistory() {
        viewModelScope.launch {
            try {
                chatRepository.getChatHistory().collect { messages ->
                    Log.d(TAG, "Loaded ${messages.size} messages from history")
                    
                    // If no messages exist, add welcome message
                    if (messages.isEmpty()) {
                        val welcomeMessage = ChatMessage(
                            text = "Hello! I'm PulseLink, your health assistant.\n\nI can help you with:\n• Health advice based on your blood pressure\n• Medication reminders\n• General wellness tips\n\nHow can I help you today?",
                            fromAssistant = true,
                            timestamp = System.currentTimeMillis()
                        )
                        chatRepository.saveMessage(welcomeMessage)
                    } else {
                        // 检查是否有新的AI回复消息，如果有则关闭sending状态
                        val hasNewAiMessage = messages.lastOrNull()?.fromAssistant == true
                        
                        _uiState.update { 
                            it.copy(
                                messages = messages,
                                isLoadingHistory = false,
                                sending = if (hasNewAiMessage) false else it.sending
                            ) 
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chat history", e)
                _uiState.update { 
                    it.copy(
                        isLoadingHistory = false,
                        error = "Failed to load chat history"
                    ) 
                }
            }
        }
    }
    
    private fun loadUserAvatar() {
        viewModelScope.launch {
            try {
                // 获取当前用户的 senior ID
                val seniorId = firebaseAuth.currentUser?.uid
                
                if (seniorId.isNullOrBlank()) {
                    Log.w(TAG, "No senior ID found, using default avatar")
                    _uiState.update { it.copy(userAvatarEmoji = "🧓") }
                    return@launch
                }
                
                // 获取 senior 数据
                val result = seniorProfileRepository.getProfileById(seniorId)
                result.onSuccess { profile ->
                    val avatarEmoji = if (profile.avatarType.isNotBlank()) {
                        AvatarHelper.getAvatarEmoji(profile.avatarType)
                    } else {
                        AvatarHelper.getAvatarEmojiByAgeGender(profile.age, profile.gender)
                    }
                    Log.d(TAG, "User avatar loaded: $avatarEmoji (type: ${profile.avatarType})")
                    _uiState.update { it.copy(userAvatarEmoji = avatarEmoji) }
                }.onFailure { e ->
                    Log.e(TAG, "Failed to load user avatar", e)
                    // 使用默认头像
                    _uiState.update { it.copy(userAvatarEmoji = "🧓") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user avatar", e)
                _uiState.update { it.copy(userAvatarEmoji = "🧓") }
            }
        }
    }

    fun onInputChange(newValue: TextFieldValue) {
        _uiState.update { it.copy(inputText = newValue) }
    }

    fun sendTextMessage() {
        val text = _uiState.value.inputText.text.trim()
        if (text.isEmpty()) return
        
        Log.d(TAG, "Sending message: $text")
        
        viewModelScope.launch {
            // Add user message
            val userMsg = ChatMessage(
                text = text,
                fromAssistant = false,
                timestamp = System.currentTimeMillis()
            )
            
            // Save user message to Firestore
            chatRepository.saveMessage(userMsg)
            
            _uiState.update { 
                it.copy(
                    inputText = TextFieldValue(""), 
                    sending = true,
                    error = null
                ) 
            }
            
            try {
                // Get latest health data to provide context
                Log.d(TAG, "Fetching health data...")
                val healthDataResult = getHealthDataUseCase()
                val healthContext = healthDataResult.getOrNull()?.let { data ->
                    "Blood Pressure: ${data.systolic}/${data.diastolic} mmHg"
                }
                Log.d(TAG, "Health context: $healthContext")
                
                // Call AI Cloud Function
                Log.d(TAG, "Calling AI cloud function...")
                val aiResult = chatWithAIUseCase(text, healthContext)
                
                if (aiResult.isSuccess) {
                    val reply = aiResult.getOrNull() ?: "Sorry, I couldn't process that."
                    Log.d(TAG, "AI Reply: $reply")
                    
                    val aiReply = ChatMessage(
                        text = reply,
                        fromAssistant = true,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    // Save AI response to Firestore
                    chatRepository.saveMessage(aiReply)
                    
                    _uiState.update { 
                        it.copy(sending = false) 
                    }
                } else {
                    val error = aiResult.exceptionOrNull()
                    Log.e(TAG, "AI call failed", error)
                    
                    val errorMsg = ChatMessage(
                        text = """
                            ❌ Error occurred:
                            
                            Type: ${error?.javaClass?.simpleName}
                            Message: ${error?.message}
                            
                            Please check:
                            1. Firebase Authentication (are you logged in?)
                            2. Cloud Function deployed?
                            3. API Key configured?
                            4. Internet connection?
                        """.trimIndent(),
                        fromAssistant = true,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    chatRepository.saveMessage(errorMsg)
                    
                    _uiState.update { 
                        it.copy(
                            sending = false,
                            error = error?.message
                        ) 
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in sendTextMessage", e)
                
                val errorMsg = ChatMessage(
                    text = """
                        ❌ Exception occurred:
                        
                        Type: ${e.javaClass.simpleName}
                        Message: ${e.localizedMessage}
                        
                        Stack trace: ${e.stackTraceToString().take(500)}
                    """.trimIndent(),
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
    
    fun clearChatHistory() {
        viewModelScope.launch {
            try {
                chatRepository.clearChatHistory().onSuccess {
                    Log.d(TAG, "Chat history cleared")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to clear chat history", e)
                    _uiState.update { it.copy(error = "Failed to clear chat history") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception clearing chat history", e)
            }
        }
    }

    fun onMicPressed() {
        Log.d(TAG, "Starting audio recording with amplitude monitoring...")
        
        try {
            val file = audioRecorder.startRecording()
            _uiState.update { 
                it.copy(
                    isRecording = true,
                    recordedAudioFile = file,
                    recordingAmplitude = 0f
                ) 
            }
            
            // 启动音量监测协程
            amplitudeJob = viewModelScope.launch {
                while (isActive) {
                    val maxAmp = audioRecorder.getMaxAmplitude()
                    val normalizedAmp = (maxAmp / 32767f).coerceIn(0f, 1f)
                    _uiState.update { it.copy(recordingAmplitude = normalizedAmp) }
                    kotlinx.coroutines.delay(100) // 每100ms采样一次
                }
            }
            
            Log.d(TAG, "Recording started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            _uiState.update { 
                it.copy(
                    isRecording = false,
                    recordingAmplitude = 0f,
                    error = "Failed to start recording: ${e.message}"
                ) 
            }
        }
    }
    
    fun onMicReleased() {
        Log.d(TAG, "Stopping audio recording and processing...")
        
        // 停止音量监测
        amplitudeJob?.cancel()
        amplitudeJob = null
        
        val audioFile = audioRecorder.stopRecording()
        if (audioFile == null) {
            Log.e(TAG, "No audio file generated")
            _uiState.update { 
                it.copy(
                    isRecording = false,
                    recordingAmplitude = 0f,
                    recordedAudioFile = null,
                    error = "Recording failed"
                ) 
            }
            return
        }
        
        // 处理录音：调用Repository发送语音消息
        viewModelScope.launch {
            try {
                _uiState.update { 
                    it.copy(
                        isRecording = false,
                        recordingAmplitude = 0f,
                        sending = false // 先不显示sending,等消息发送后再显示
                    ) 
                }
                
                // 1. 计算音频时长
                val duration = getAudioDuration(audioFile)
                Log.d(TAG, "Audio duration: ${duration}s")
                
                // 2. 调用Repository发送语音消息
                // Repository会负责：上传到Storage + 写入Firestore
                // Firestore写入会自动触发Cloud Function进行AI处理
                val result = chatRepository.sendVoiceMessage(audioFile, duration.toInt())
                
                result.onSuccess {
                    Log.d(TAG, "Voice message sent successfully")
                    // 3. 删除本地临时文件
                    audioFile.delete()
                    
                    // 4. 延迟一点再显示AI thinking状态(让用户看到自己的消息)
                    kotlinx.coroutines.delay(500)
                    
                    _uiState.update { 
                        it.copy(
                            recordedAudioFile = null,
                            sending = true // 现在显示AI正在思考
                        )
                    }
                }.onFailure { e ->
                    throw e
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing audio message", e)
                audioFile?.delete()
                
                val errorMsg = ChatMessage(
                    text = "❌ Failed to process audio: ${e.message}",
                    fromAssistant = true,
                    type = com.alvin.pulselink.domain.model.MessageType.TEXT,
                    timestamp = System.currentTimeMillis()
                )
                chatRepository.saveMessage(errorMsg)
                
                _uiState.update { 
                    it.copy(
                        sending = false,
                        recordedAudioFile = null,
                        error = "Audio processing failed: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * 获取音频文件时长（秒）
     */
    private fun getAudioDuration(file: java.io.File): Int {
        return try {
            val mediaPlayer = android.media.MediaPlayer()
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.prepare()
            val durationMs = mediaPlayer.duration
            mediaPlayer.release()
            (durationMs / 1000) // 转换为秒
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get audio duration", e)
            0
        }
    }
    
    /**
     * 播放音频消息
     */
    fun playAudioMessage(messageId: String, downloadUrl: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Playing audio message: $messageId")
                Log.d(TAG, "Download URL: $downloadUrl")
                
                if (downloadUrl.isBlank()) {
                    Log.e(TAG, "Download URL is empty")
                    _uiState.update { 
                        it.copy(error = "Audio URL is missing") 
                    }
                    return@launch
                }
                
                // 停止之前的播放
                if (_uiState.value.playingMessageId != null) {
                    Log.d(TAG, "Stopping previous playback")
                    audioPlayer.stop()
                }
                
                _uiState.update { it.copy(playingMessageId = messageId) }
                audioPlayer.playUrl(downloadUrl)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play audio", e)
                _uiState.update { 
                    it.copy(
                        playingMessageId = null,
                        error = "Failed to play audio: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * 停止音频播放
     */
    fun stopAudioPlayback() {
        Log.d(TAG, "Stopping audio playback")
        audioPlayer.stop()
        _uiState.update { it.copy(playingMessageId = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        amplitudeJob?.cancel()
        audioRecorderManager.destroy()
        audioRecorder.destroy()
        audioPlayer.destroy()
    }
}
