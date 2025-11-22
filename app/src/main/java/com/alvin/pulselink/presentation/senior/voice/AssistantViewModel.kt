package com.alvin.pulselink.presentation.senior.voice

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.usecase.ChatWithAIUseCase
import com.alvin.pulselink.domain.usecase.GetHealthDataUseCase
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
    private val getHealthDataUseCase: GetHealthDataUseCase
): ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "AssistantViewModel"
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendTextMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        
        Log.d(TAG, "Sending message: $text")
        
        viewModelScope.launch {
            // Add user message
            val userMsg = ChatMessage(
                id = System.currentTimeMillis(), 
                fromAssistant = false, 
                text = text
            )
            _uiState.update { 
                it.copy(
                    messages = it.messages + userMsg, 
                    inputText = "", 
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
                        id = System.currentTimeMillis() + 1,
                        fromAssistant = true,
                        text = reply
                    )
                    _uiState.update { 
                        it.copy(
                            messages = it.messages + aiReply, 
                            sending = false
                        ) 
                    }
                } else {
                    val error = aiResult.exceptionOrNull()
                    Log.e(TAG, "AI call failed", error)
                    
                    // 开发阶段显示详细错误信息
                    val errorMsg = ChatMessage(
                        id = System.currentTimeMillis() + 1,
                        fromAssistant = true,
                        text = """
                            ❌ Error occurred:
                            
                            Type: ${error?.javaClass?.simpleName}
                            Message: ${error?.message}
                            
                            Please check:
                            1. Firebase Authentication (are you logged in?)
                            2. Cloud Function deployed?
                            3. API Key configured?
                            4. Internet connection?
                        """.trimIndent()
                    )
                    _uiState.update { 
                        it.copy(
                            messages = it.messages + errorMsg, 
                            sending = false,
                            error = error?.message
                        ) 
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in sendTextMessage", e)
                
                val errorMsg = ChatMessage(
                    id = System.currentTimeMillis() + 1,
                    fromAssistant = true,
                    text = """
                        ❌ Exception occurred:
                        
                        Type: ${e.javaClass.simpleName}
                        Message: ${e.localizedMessage}
                        
                        Stack trace: ${e.stackTraceToString().take(500)}
                    """.trimIndent()
                )
                _uiState.update { 
                    it.copy(
                        messages = it.messages + errorMsg, 
                        sending = false,
                        error = e.message
                    ) 
                }
            }
        }
    }

    fun onMicClicked() {
        // TODO: Implement voice input with Speech Recognition
        _uiState.update { it.copy(listening = !it.listening) }
    }
}
