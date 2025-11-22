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
            text = "Hello! I'm PulseLink, your health assistant.\n\nI can help you with:\n• Health advice based on your blood pressure\n• Medication reminders\n• General wellness tips\n\nHow can I help you today?"
        )
    ),
    val inputText: String = "",
    val listening: Boolean = false,
    val sending: Boolean = false,
    val error: String? = null
)
