package com.alvin.pulselink.domain.usecase

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

class VoiceToTextUseCase @Inject constructor(
    private val functions: FirebaseFunctions
) {
    companion object {
        private const val TAG = "VoiceToTextUseCase"
    }
    
    /**
     * 将音频文件转换为文本
     * @param audioBase64 Base64 编码的音频数据
     * @return Result<String> 识别的文本
     */
    suspend operator fun invoke(audioBase64: String): Result<String> {
        return try {
            Log.d(TAG, "Calling voiceToText function with ${audioBase64.length} chars")
            
            val data = hashMapOf(
                "audio" to audioBase64
            )
            
            val result = functions
                .getHttpsCallable("voiceToText")
                .call(data)
                .await()
            
            val response = result.getData() as? Map<*, *>
                ?: return Result.failure(Exception("Invalid response from cloud function"))
            
            if (response["success"] != true) {
                val error = response["error"] as? String ?: "Unknown error"
                Log.e(TAG, "Voice to text failed: $error")
                return Result.failure(Exception(error))
            }
            
            val text = response["text"] as? String ?: ""
            Log.d(TAG, "Transcription result: $text")
            
            Result.success(text)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calling voiceToText function", e)
            Result.failure(e)
        }
    }
}
