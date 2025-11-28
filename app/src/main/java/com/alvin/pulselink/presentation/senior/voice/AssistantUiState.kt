package com.alvin.pulselink.presentation.senior.voice

import androidx.compose.ui.text.input.TextFieldValue
import com.alvin.pulselink.domain.model.ChatMessage
import java.io.File

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: TextFieldValue = TextFieldValue(""),
    val listening: Boolean = false,
    val sending: Boolean = false, // 等待 AI 回复
    val isLoadingTranscription: Boolean = false, // 语音转文字中
    val error: String? = null,
    val isLoadingHistory: Boolean = true,
    val userAvatarEmoji: String = "🧓", // 默认老人头像
    // 音频录制相关
    val isRecording: Boolean = false,
    val recordingAmplitude: Float = 0f, // 归一化音量 (0.0 - 1.0)
    val recordedAudioFile: File? = null,
    // 音频播放相关
    val playingMessageId: String? = null // 当前正在播放的消息ID
)
