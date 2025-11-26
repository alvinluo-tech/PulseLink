package com.alvin.pulselink.data.repository

import android.util.Log
import com.alvin.pulselink.domain.model.ChatMessage
import com.alvin.pulselink.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : ChatRepository {
    
    companion object {
        private const val TAG = "ChatRepositoryImpl"
        private const val COLLECTION_CHAT = "chat_history"
    }
    
    override fun getChatHistory(): Flow<List<ChatMessage>> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid
        
        if (uid == null) {
            Log.w(TAG, "User not authenticated, returning empty chat history")
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = firestore
            .collection(COLLECTION_CHAT)
            .document(uid)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to chat history", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ChatMessage(
                            id = doc.id,
                            userId = uid,
                            text = doc.getString("text") ?: "",
                            fromAssistant = doc.getBoolean("fromAssistant") ?: false,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(messages)
            }
        
        awaitClose { listener.remove() }
    }
    
    override suspend fun saveMessage(message: ChatMessage): Result<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                return Result.failure(IllegalStateException("User not authenticated"))
            }
            
            val messageData = hashMapOf(
                "text" to message.text,
                "fromAssistant" to message.fromAssistant,
                "timestamp" to message.timestamp
            )
            
            if (message.id.isEmpty()) {
                // Create new message with auto-generated ID
                firestore
                    .collection(COLLECTION_CHAT)
                    .document(uid)
                    .collection("messages")
                    .add(messageData)
                    .await()
            } else {
                // Update existing message
                firestore
                    .collection(COLLECTION_CHAT)
                    .document(uid)
                    .collection("messages")
                    .document(message.id)
                    .set(messageData)
                    .await()
            }
            
            Log.d(TAG, "Message saved successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving message", e)
            Result.failure(e)
        }
    }
    
    override suspend fun clearChatHistory(): Result<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                return Result.failure(IllegalStateException("User not authenticated"))
            }
            
            val snapshot = firestore
                .collection(COLLECTION_CHAT)
                .document(uid)
                .collection("messages")
                .get()
                .await()
            
            // Delete all messages in batch
            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            
            Log.d(TAG, "Chat history cleared successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing chat history", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                return Result.failure(IllegalStateException("User not authenticated"))
            }
            
            firestore
                .collection(COLLECTION_CHAT)
                .document(uid)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()
            
            Log.d(TAG, "Message deleted successfully: $messageId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message: $messageId", e)
            Result.failure(e)
        }
    }
}
