package com.alvin.pulselink.domain.repository

import com.alvin.pulselink.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ChatRepository {
    /**
     * Get chat history for the current user
     */
    fun getChatHistory(): Flow<List<ChatMessage>>
    
    /**
     * Save a new chat message
     */
    suspend fun saveMessage(message: ChatMessage): Result<Unit>
    
    /**
     * Send a voice message: upload to Storage and write to Firestore
     * This will trigger the Cloud Function to process the audio
     */
    suspend fun sendVoiceMessage(audioFile: File, durationSeconds: Int): Result<Unit>
    
    /**
     * Delete all chat history for the current user
     */
    suspend fun clearChatHistory(): Result<Unit>
    
    /**
     * Delete a specific message
     */
    suspend fun deleteMessage(messageId: String): Result<Unit>
}
