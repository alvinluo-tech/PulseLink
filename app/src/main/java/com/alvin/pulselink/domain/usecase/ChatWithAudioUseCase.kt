package com.alvin.pulselink.domain.usecase

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 发送音频消息给AI，AI理解语音内容并回复
 * 
 * 流程：
 * 1. 发送GCS URI给AI Cloud Function
 * 2. AI使用Speech-to-Text理解语音
 * 3. AI根据内容生成回复
 * 4. 返回文本回复
 */
class ChatWithAudioUseCase @Inject constructor(
    private val functions: FirebaseFunctions
) {
    
    companion object {
        private const val TAG = "ChatWithAudioUseCase"
    }
    
    /**
     * 发送音频消息给AI并获取回复
     * 
     * @param audioGcsUri 音频文件的GCS URI (gs://...)
     * @param healthData 健康数据（可选）
     * @return AI的文本回复
     */
    suspend operator fun invoke(
        audioGcsUri: String,
        healthData: String? = null
    ): Result<String> {
        return try {
            Log.d(TAG, "=== ChatWithAudio Start ===")
            Log.d(TAG, "Audio GCS URI: $audioGcsUri")
            Log.d(TAG, "Health Data: $healthData")
            
            // 构造请求数据
            val data = hashMapOf(
                "audioGcsUri" to audioGcsUri,
                "healthData" to (healthData ?: "No recent data")
            )
            
            Log.d(TAG, "Request data: $data")
            
            // 调用云函数
            Log.d(TAG, "Calling cloud function: chatWithAudio")
            val result = functions
                .getHttpsCallable("chatWithAudio")
                .call(data)
                .await()
            
            Log.d(TAG, "Cloud function returned")
            
            // 解析响应
            val response = result.data as? Map<*, *>
            Log.d(TAG, "Response: $response")
            
            val success = response?.get("success") as? Boolean ?: false
            val reply = response?.get("reply") as? String
            val transcription = response?.get("transcription") as? String
            
            Log.d(TAG, "Success: $success")
            Log.d(TAG, "Transcription: $transcription")
            Log.d(TAG, "Reply: $reply")
            
            if (success && reply != null) {
                Log.d(TAG, "=== ChatWithAudio Success ===")
                Result.success(reply)
            } else {
                Log.e(TAG, "Invalid response - success: $success, reply: $reply")
                Result.failure(Exception("Invalid response from AI"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "=== ChatWithAudio Failed ===", e)
            Log.e(TAG, "Exception type: ${e.javaClass.name}")
            Log.e(TAG, "Exception message: ${e.message}")
            Result.failure(e)
        }
    }
}
