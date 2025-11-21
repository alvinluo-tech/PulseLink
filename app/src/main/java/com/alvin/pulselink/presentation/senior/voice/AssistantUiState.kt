package com.alvin.pulselink.presentation.senior.voice

data class ChatMessage(
    val id: Long,
    val fromAssistant: Boolean,
    val text: String
)

data class AssistantUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            id = 1L,
            fromAssistant = true,
            text = "Hello! I am your smart assistant.\nYou can ask me about health, weather, reminders and more."
        )
    ),
    val inputText: String = "",
    val listening: Boolean = false,
    val sending: Boolean = false,
    val error: String? = null
)
