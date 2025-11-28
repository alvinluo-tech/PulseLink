package com.alvin.pulselink.data.storage

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Storage Manager for audio files
 * 管理音频文件的上传和下载
 */
@Singleton
class AudioStorageManager @Inject constructor(
    private val storage: FirebaseStorage
) {
    companion object {
        private const val TAG = "AudioStorageManager"
        private const val AUDIO_PATH = "voice_messages"
    }
    
    /**
     * 上传音频文件到 Firebase Storage
     * @param file 本地音频文件
     * @param userId 用户ID
     * @return Pair<GCS URI, Download URL>
     */
    suspend fun uploadAudioFile(file: File, userId: String): Result<Pair<String, String>> {
        return try {
            val timestamp = System.currentTimeMillis()
            val fileName = "${userId}_${timestamp}.m4a"
            val storageRef: StorageReference = storage.reference
                .child(AUDIO_PATH)
                .child(userId)
                .child(fileName)
            
            Log.d(TAG, "Uploading audio file: $fileName")
            
            // 上传文件
            val uploadTask = storageRef.putFile(Uri.fromFile(file))
            uploadTask.await()
            
            // 获取下载 URL
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            // 构造 GCS URI
            val gcsUri = "gs://${storageRef.bucket}/${storageRef.path}"
            
            Log.d(TAG, "Upload successful")
            Log.d(TAG, "GCS URI: $gcsUri")
            Log.d(TAG, "Download URL: $downloadUrl")
            
            Result.success(Pair(gcsUri, downloadUrl))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload audio file", e)
            Result.failure(e)
        }
    }
    
    /**
     * 删除音频文件
     * @param gcsUri GCS URI (gs://...)
     */
    suspend fun deleteAudioFile(gcsUri: String): Result<Unit> {
        return try {
            val storageRef = storage.getReferenceFromUrl(gcsUri)
            storageRef.delete().await()
            Log.d(TAG, "Audio file deleted: $gcsUri")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete audio file", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取音频文件的下载URL
     * @param gcsUri GCS URI
     */
    suspend fun getDownloadUrl(gcsUri: String): Result<String> {
        return try {
            val storageRef = storage.getReferenceFromUrl(gcsUri)
            val url = storageRef.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get download URL", e)
            Result.failure(e)
        }
    }
}
