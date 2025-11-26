package com.alvin.pulselink.domain.model

data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val text: String = "",
    val fromAssistant: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
