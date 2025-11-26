package com.alvin.pulselink.data.speech

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechRecognizerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transcriptionService: AudioTranscriptionService
) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    companion object {
        private const val TAG = "SpeechRecognizer"
    }
    
    fun startListening() {
        Log.d(TAG, "startListening() called")
        
        if (_isListening.value) {
            Log.w(TAG, "Already listening")
            return
        }
        
        try {
            // 创建临时音频文件
            audioFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            
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
            
            _isListening.value = true
            _error.value = null
            Log.d(TAG, "Recording started: ${audioFile?.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            _error.value = "Failed to start recording: ${e.message}"
            _isListening.value = false
            cleanup()
        }
    }
    
    suspend fun stopListening() {
        Log.d(TAG, "stopListening() called")
        
        if (!_isListening.value) {
            Log.w(TAG, "Not currently listening")
            return
        }
        
        try {
            // 停止录音
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isListening.value = false
            
            // 发送音频文件到 Gemini API 进行转录
            audioFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    Log.d(TAG, "Audio file ready: ${file.absolutePath}, size: ${file.length()} bytes")
                    transcribeAudio(file)
                } else {
                    Log.e(TAG, "Audio file is empty or doesn't exist")
                    _error.value = "Recording failed - no audio captured"
                    cleanup()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            _error.value = "Failed to stop recording: ${e.message}"
            cleanup()
        }
    }
    
    private suspend fun transcribeAudio(audioFile: File) {
        try {
            Log.d(TAG, "Transcribing audio with Cloud Speech-to-Text API...")
            
            val result = transcriptionService.transcribe(audioFile)
            
            if (result.isSuccess) {
                val transcript = result.getOrNull() ?: ""
                if (transcript.isNotEmpty()) {
                    Log.d(TAG, "Transcription successful: $transcript")
                    _recognizedText.value = transcript
                } else {
                    Log.w(TAG, "Transcription returned empty text")
                    _error.value = "No speech detected"
                }
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "Transcription failed", error)
                _error.value = "Speech recognition failed: ${error?.message}"
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error transcribing audio", e)
            _error.value = "Transcription failed: ${e.message}"
        } finally {
            cleanup()
        }
    }
    
    fun cancelListening() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling recording", e)
        }
        _isListening.value = false
        _recognizedText.value = ""
        cleanup()
    }
    
    private fun cleanup() {
        audioFile?.delete()
        audioFile = null
    }
    
    fun destroy() {
        cancelListening()
        Log.d(TAG, "Speech recognizer destroyed")
    }
}
