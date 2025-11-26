package com.alvin.pulselink.data.speech

import android.content.Context
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    companion object {
        private const val TAG = "AudioRecorder"
    }
    
    fun startRecording(): Boolean {
        if (_isRecording.value) {
            Log.w(TAG, "Already recording")
            return false
        }
        
        try {
            // 创建临时音频文件
            audioFile = File(context.cacheDir, "voice_input_${System.currentTimeMillis()}.m4a")
            
            Log.d(TAG, "Creating audio file: ${audioFile?.absolutePath}")
            
            // 初始化 MediaRecorder
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile?.absolutePath)
                
                prepare()
                start()
            }
            
            _isRecording.value = true
            _error.value = null
            Log.d(TAG, "Recording started")
            return true
            
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording", e)
            _error.value = "Failed to start recording: ${e.message}"
            cleanup()
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error starting recording", e)
            _error.value = "Recording error: ${e.message}"
            cleanup()
            return false
        }
    }
    
    fun stopRecording(): File? {
        if (!_isRecording.value) {
            Log.w(TAG, "Not recording")
            return null
        }
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            _isRecording.value = false
            Log.d(TAG, "Recording stopped. File size: ${audioFile?.length()} bytes")
            
            return audioFile
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            _error.value = "Failed to stop recording: ${e.message}"
            cleanup()
            return null
        }
    }
    
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            // 删除音频文件
            audioFile?.delete()
            audioFile = null
            
            _isRecording.value = false
            Log.d(TAG, "Recording cancelled")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling recording", e)
            cleanup()
        }
    }
    
    fun getBase64Audio(file: File): String? {
        return try {
            val bytes = file.readBytes()
            Log.d(TAG, "Converting ${bytes.size} bytes to Base64")
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading audio file", e)
            _error.value = "Failed to read audio: ${e.message}"
            null
        }
    }
    
    private fun cleanup() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
            audioFile?.delete()
            audioFile = null
            _isRecording.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    fun destroy() {
        cleanup()
        Log.d(TAG, "AudioRecorderManager destroyed")
    }
}
