package com.alvin.pulselink.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(): ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendTextMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            val userMsg = ChatMessage(id = System.currentTimeMillis(), fromAssistant = false, text = text)
            _uiState.update { it.copy(messages = it.messages + userMsg, inputText = "", sending = true) }
            // TODO: integrate with real AI backend
            kotlinx.coroutines.delay(300)
            val reply = ChatMessage(
                id = System.currentTimeMillis() + 1,
                fromAssistant = true,
                text = "You said: \"$text\"\n(I'll answer better after backend is connected.)"
            )
            _uiState.update { it.copy(messages = it.messages + reply, sending = false) }
        }
    }

    fun onMicClicked() {
        // Placeholder for voice input
        _uiState.update { it.copy(listening = !it.listening) }
    }
}
