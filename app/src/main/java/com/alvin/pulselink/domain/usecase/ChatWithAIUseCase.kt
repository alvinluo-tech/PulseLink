package com.alvin.pulselink.domain.usecase

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 调用 Firebase Cloud Functions 实现 AI 对话
 */
class ChatWithAIUseCase @Inject constructor(
    private val functions: FirebaseFunctions
) {
    
    companion object {
        private const val TAG = "ChatWithAIUseCase"
    }
    
    /**
     * 发送消息给 AI 并获取回复
     * 
     * @param message 用户消息
     * @param healthData 健康数据（可选），用于给 AI 提供上下文
     * @return AI 的回复文本
     */
    suspend operator fun invoke(
        message: String,
        healthData: String? = null
    ): Result<String> {
        return try {
            Log.d(TAG, "=== ChatWithAI Start ===")
            Log.d(TAG, "Message: $message")
            Log.d(TAG, "Health Data: $healthData")
            
            // 构造请求数据
            val data = hashMapOf(
                "text" to message,
                "healthData" to (healthData ?: "No recent data")
            )
            
            Log.d(TAG, "Request data: $data")
            
            // 调用云函数
            Log.d(TAG, "Calling cloud function: chatWithAI")
            val result = functions
                .getHttpsCallable("chatWithAI")
                .call(data)
                .await()
            
            Log.d(TAG, "Cloud function returned")
            
            // 解析响应
            val response = result.data as? Map<*, *>
            Log.d(TAG, "Response: $response")
            
            val success = response?.get("success") as? Boolean ?: false
            val reply = response?.get("reply") as? String
            
            Log.d(TAG, "Success: $success, Reply: $reply")
            
            if (success && reply != null) {
                Log.d(TAG, "=== ChatWithAI Success ===")
                Result.success(reply)
            } else {
                Log.e(TAG, "Invalid response - success: $success, reply: $reply")
                Result.failure(Exception("Invalid response from AI - success: $success, reply is null: ${reply == null}"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "=== ChatWithAI Failed ===", e)
            Log.e(TAG, "Exception type: ${e.javaClass.name}")
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "Exception cause: ${e.cause}")
            Result.failure(e)
        }
    }
}
