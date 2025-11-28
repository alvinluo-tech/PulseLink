package com.alvin.pulselink.data.speech

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Audio Recorder with amplitude monitoring
 * 录音并支持实时音量监测
 */
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    
    companion object {
        private const val TAG = "AudioRecorder"
    }
    
    /**
     * 开始录音并返回文件
     */
    fun startRecording(): File {
        outputFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
                Log.d(TAG, "Recording started: ${outputFile?.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                throw e
            }
        }
        
        return outputFile!!
    }
    
    /**
     * 获取当前音量 (0~32767)
     * 用于驱动波纹动画
     */
    fun getMaxAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get amplitude", e)
            0
        }
    }
    
    /**
     * 停止录音并返回文件
     */
    fun stopRecording(): File? {
        try {
            recorder?.stop()
            recorder?.release()
            Log.d(TAG, "Recording stopped: ${outputFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
        } finally {
            recorder = null
        }
        return outputFile
    }
    
    /**
     * 取消录音并删除文件
     */
    fun cancelRecording() {
        try {
            recorder?.stop()
            recorder?.release()
            outputFile?.delete()
            Log.d(TAG, "Recording cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel recording", e)
        } finally {
            recorder = null
            outputFile = null
        }
    }
    
    /**
     * 清理资源
     */
    fun destroy() {
        try {
            recorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to destroy recorder", e)
        } finally {
            recorder = null
        }
    }
}
