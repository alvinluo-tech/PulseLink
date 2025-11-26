package com.alvin.pulselink.presentation.senior.voice

import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.data.speech.AudioRecorderManager
import com.alvin.pulselink.domain.model.ChatMessage
import com.alvin.pulselink.domain.repository.ChatRepository
import com.alvin.pulselink.domain.repository.SeniorProfileRepository
import com.alvin.pulselink.domain.usecase.ChatWithAIUseCase
import com.alvin.pulselink.domain.usecase.GetHealthDataUseCase
import com.alvin.pulselink.domain.usecase.VoiceToTextUseCase
import com.alvin.pulselink.util.AvatarHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val chatWithAIUseCase: ChatWithAIUseCase,
    private val getHealthDataUseCase: GetHealthDataUseCase,
    private val chatRepository: ChatRepository,
    private val audioRecorderManager: AudioRecorderManager,
    private val voiceToTextUseCase: VoiceToTextUseCase,
    private val seniorProfileRepository: SeniorProfileRepository,
    private val localDataSource: LocalDataSource
): ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "AssistantViewModel"
    }
    
    init {
        loadChatHistory()
        observeRecordingState()
        loadUserAvatar()
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
                        _uiState.update { 
                            it.copy(
                                messages = messages,
                                isLoadingHistory = false
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
                val cachedUser = localDataSource.getUser()
                val seniorId = cachedUser?.first
                
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
        Log.d(TAG, "onMicPressed() called - starting audio recording")
        val started = audioRecorderManager.startRecording()
        if (!started) {
            _uiState.update { it.copy(error = "Failed to start recording") }
        }
    }
    
    fun onMicReleased() {
        Log.d(TAG, "onMicReleased() called - stopping audio recording")
        
        val audioFile = audioRecorderManager.stopRecording()
        if (audioFile == null) {
            Log.e(TAG, "No audio file generated")
            _uiState.update { it.copy(error = "Recording failed") }
            return
        }
        
        // 转换音频为文本
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingTranscription = true) }
                
                // 读取音频文件并转换为 Base64
                val base64Audio = audioRecorderManager.getBase64Audio(audioFile)
                if (base64Audio == null) {
                    _uiState.update { 
                        it.copy(
                            isLoadingTranscription = false,
                            error = "Failed to read audio file"
                        ) 
                    }
                    return@launch
                }
                
                Log.d(TAG, "Sending ${base64Audio.length} chars to voice-to-text service")
                
                // 调用 Cloud Function 转换为文本
                val result = voiceToTextUseCase(base64Audio)
                
                result.onSuccess { text ->
                    Log.d(TAG, "Transcription successful: $text")
                    
                    // 删除临时文件
                    audioFile.delete()
                    
                    if (text.isNotBlank()) {
                        // 将识别的文本填入输入框，并将光标设置在末尾
                        _uiState.update { 
                            it.copy(
                                inputText = TextFieldValue(
                                    text = text,
                                    selection = TextRange(text.length) // 光标在末尾
                                ),
                                isLoadingTranscription = false,
                                error = null
                            ) 
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isLoadingTranscription = false,
                                error = "No speech detected"
                            ) 
                        }
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Transcription failed", e)
                    audioFile.delete()
                    _uiState.update { 
                        it.copy(
                            isLoadingTranscription = false,
                            error = "Voice recognition failed: ${e.message}"
                        ) 
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing audio", e)
                audioFile?.delete()
                _uiState.update { 
                    it.copy(
                        isLoadingTranscription = false,
                        error = "Error processing audio: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        audioRecorderManager.destroy()
    }
}
