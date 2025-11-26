package com.alvin.pulselink.domain.repository

import com.alvin.pulselink.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

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
     * Delete all chat history for the current user
     */
    suspend fun clearChatHistory(): Result<Unit>
    
    /**
     * Delete a specific message
     */
    suspend fun deleteMessage(messageId: String): Result<Unit>
}
