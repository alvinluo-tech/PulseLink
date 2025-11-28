package com.alvin.pulselink.data.repository

import android.net.Uri
import android.util.Log
import com.alvin.pulselink.domain.model.ChatMessage
import com.alvin.pulselink.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val storage: FirebaseStorage
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
                        // 解析消息类型
                        val typeString = doc.getString("type") ?: "text"
                        val messageType = when (typeString) {
                            "audio" -> com.alvin.pulselink.domain.model.MessageType.AUDIO
                            else -> com.alvin.pulselink.domain.model.MessageType.TEXT
                        }
                        
                        // 解析timestamp - 可能是Timestamp对象或Long
                        val timestamp = try {
                            doc.getLong("timestamp") ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            // 如果是Timestamp对象，使用getTimestamp()
                            doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                        }
                        
                        ChatMessage(
                            id = doc.id,
                            userId = uid,
                            text = doc.getString("text") ?: "",
                            fromAssistant = doc.getBoolean("fromAssistant") ?: false,
                            type = messageType,
                            audioGcsUri = doc.getString("audioGcsUri"),
                            audioDownloadUrl = doc.getString("audioDownloadUrl"),
                            duration = (doc.getLong("duration") ?: 0L).toInt(),
                            timestamp = timestamp
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
    
    override suspend fun sendVoiceMessage(audioFile: File, durationSeconds: Int): Result<Unit> {
        return try {
            val userId = firebaseAuth.currentUser?.uid 
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            Log.d(TAG, "Sending voice message for user: $userId")
            Log.d(TAG, "Audio file: ${audioFile.absolutePath}, size: ${audioFile.length()} bytes")
            
            val fileName = "${UUID.randomUUID()}.m4a"
            
            // 1. 定义存储路径: voice_messages/{userId}/{fileName}
            val storageRef = storage.reference
                .child("voice_messages")
                .child(userId)
                .child(fileName)
            
            Log.d(TAG, "Storage path: ${storageRef.path}")
            Log.d(TAG, "Starting upload...")
            
            // 2. 上传文件到 Storage (使用 await() 挂起直到上传完成)
            val uploadTask = storageRef.putFile(Uri.fromFile(audioFile))
            uploadTask.await()
            
            Log.d(TAG, "Upload completed successfully")
            
            // 3. 获取 GCS 路径 (给 Gemini 用) 和 下载链接 (给前端回放用)
            // 使用 storageRef.bucket 获取实际的 bucket 名称
            val bucketName = storageRef.bucket
            val gcsUri = "gs://$bucketName/voice_messages/$userId/$fileName"
            
            // 获取 HTTP 下载链接 (用于在 App 里点击播放)
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            Log.d(TAG, "Audio uploaded - GCS URI: $gcsUri")
            Log.d(TAG, "Download URL: $downloadUrl")
            
            // 4. 写入 Firestore (这将触发云函数)
            val messageData = hashMapOf(
                "type" to "audio",
                "userId" to userId,
                "audioGcsUri" to gcsUri,        // 给 AI 读
                "audioDownloadUrl" to downloadUrl, // 给 UI 播
                "duration" to durationSeconds,
                "timestamp" to FieldValue.serverTimestamp(),
                "fromAssistant" to false
            )
            
            firestore.collection(COLLECTION_CHAT)
                .document(userId)
                .collection("messages")
                .add(messageData)
                .await()
            
            Log.d(TAG, "Voice message saved to Firestore successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending voice message", e)
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
