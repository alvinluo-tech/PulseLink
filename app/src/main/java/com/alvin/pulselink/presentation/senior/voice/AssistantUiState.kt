package com.alvin.pulselink.presentation.senior.voice

import androidx.compose.ui.text.input.TextFieldValue
import com.alvin.pulselink.domain.model.ChatMessage

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: TextFieldValue = TextFieldValue(""),
    val listening: Boolean = false,
    val sending: Boolean = false, // 等待 AI 回复
    val isLoadingTranscription: Boolean = false, // 语音转文字中
    val error: String? = null,
    val isLoadingHistory: Boolean = true,
    val userAvatarEmoji: String = "🧓" // 默认老人头像
)
