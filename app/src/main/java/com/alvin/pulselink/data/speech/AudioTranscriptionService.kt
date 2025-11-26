package com.alvin.pulselink.data.speech

import android.util.Base64
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio transcription service using Cloud Function
 * 将音频文件转换为文本
 */
@Singleton
class AudioTranscriptionService @Inject constructor(
    private val functions: FirebaseFunctions
) {
    companion object {
        private const val TAG = "AudioTranscription"
    }
    
    /**
     * 转录音频文件为文本
     * @param audioFile 音频文件 (M4A 格式)
     * @return 转录的文本
     */
    suspend fun transcribe(audioFile: File): Result<String> {
        return try {
            Log.d(TAG, "Transcribing audio file: ${audioFile.name}, size: ${audioFile.length()} bytes")
            
            // 读取音频文件并转换为 Base64
            val audioBytes = audioFile.readBytes()
            val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            
            Log.d(TAG, "Audio encoded to Base64, length: ${audioBase64.length}")
            
            // 调用 Cloud Function
            val data = hashMapOf(
                "audio" to audioBase64,
                "encoding" to "MP4",
                "sampleRateHertz" to 44100,
                "languageCode" to "en-US"
            )
            
            val result = functions
                .getHttpsCallable("transcribeAudio")
                .call(data)
                .await()
            
            val response = result.getData() as? Map<*, *>
                ?: return Result.failure(Exception("Invalid response from transcription service"))
            
            if (response["success"] != true) {
                val error = response["error"] as? String ?: "Unknown error"
                Log.e(TAG, "Transcription failed: $error")
                return Result.failure(Exception(error))
            }
            
            val transcript = response["transcript"] as? String ?: ""
            Log.d(TAG, "Transcription successful: $transcript")
            
            Result.success(transcript)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error transcribing audio", e)
            Result.failure(e)
        }
    }
}
