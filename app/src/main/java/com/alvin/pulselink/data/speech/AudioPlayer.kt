package com.alvin.pulselink.data.speech

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Audio Player for voice message playback
 * 用于回放语音消息
 */
class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()
    
    companion object {
        private const val TAG = "AudioPlayer"
    }
    
    /**
     * 播放本地文件
     */
    fun playLocal(file: File) {
        try {
            stop()
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPosition.value = 0
                }
                start()
            }
            
            _isPlaying.value = true
            Log.d(TAG, "Playing local file: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play local file", e)
            _isPlaying.value = false
        }
    }
    
    /**
     * 播放网络URL
     */
    fun playUrl(url: String) {
        try {
            stop()
            
            Log.d(TAG, "Preparing to play URL: $url")
            
            mediaPlayer = MediaPlayer().apply {
                // 设置音频流类型为音乐
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                setDataSource(url)
                setVolume(1.0f, 1.0f) // 设置最大音量
                
                setOnPreparedListener {
                    Log.d(TAG, "MediaPlayer prepared, starting playback")
                    Log.d(TAG, "Duration: ${duration}ms, Volume: 1.0")
                    start()
                    _isPlaying.value = true
                }
                setOnCompletionListener {
                    Log.d(TAG, "Playback completed")
                    _isPlaying.value = false
                    _currentPosition.value = 0
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    true
                }
                setOnInfoListener { mp, what, extra ->
                    Log.d(TAG, "MediaPlayer info: what=$what, extra=$extra")
                    false
                }
                
                prepareAsync()
            }
            
            Log.d(TAG, "Playing URL: $url")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play URL", e)
            _isPlaying.value = false
        }
    }
    
    /**
     * 停止播放
     */
    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            _isPlaying.value = false
            _currentPosition.value = 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop", e)
        } finally {
            mediaPlayer = null
        }
    }
    
    /**
     * 暂停播放
     */
    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause", e)
        }
    }
    
    /**
     * 继续播放
     */
    fun resume() {
        try {
            mediaPlayer?.start()
            _isPlaying.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume", e)
        }
    }
    
    /**
     * 获取音频时长（毫秒）
     */
    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 清理资源
     */
    fun destroy() {
        stop()
    }
}
