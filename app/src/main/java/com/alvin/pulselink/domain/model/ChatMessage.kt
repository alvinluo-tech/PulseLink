package com.alvin.pulselink.domain.model

data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val text: String = "",
    val fromAssistant: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    // 音频消息相关字段
    val type: MessageType = MessageType.TEXT,
    val audioGcsUri: String? = null,      // GCS URI for backend AI (gs://...)
    val audioDownloadUrl: String? = null,  // HTTPS URL for playback
    val duration: Int = 0                   // Audio duration in seconds
)

enum class MessageType {
    TEXT,
    AUDIO
}
