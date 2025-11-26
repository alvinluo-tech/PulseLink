package com.alvin.pulselink.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * QR Code Saver Utility
 * 用于将二维码保存到设备相册
 */
object QRCodeSaver {
    
    /**
     * 保存二维码图片到相册
     * 
     * @param context Context
     * @param bitmap QR code bitmap
     * @param fileName 文件名（不包含扩展名）
     * @return Boolean 是否保存成功
     */
    fun saveQRCodeToGallery(
        context: Context,
        bitmap: Bitmap?,
        fileName: String = "PulseLink_QRCode_${System.currentTimeMillis()}"
    ): Boolean {
        if (bitmap == null) {
            Toast.makeText(context, "QR Code not available", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return try {
            val outputStream: OutputStream?
            val imageUri: android.net.Uri?
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 及以上：使用 MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.png")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PulseLink")
                }
                
                imageUri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                outputStream = imageUri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                // Android 9 及以下：使用传统文件存储
                val imagesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "PulseLink"
                )
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                
                val imageFile = File(imagesDir, "$fileName.png")
                outputStream = FileOutputStream(imageFile)
                
                // 通知系统相册更新
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                }
                context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
            }
            
            outputStream?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            
            Toast.makeText(context, "QR Code saved to gallery", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save QR Code: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
    
    /**
     * 保存二维码并显示成功消息
     * 
     * @param context Context
     * @param bitmap QR code bitmap
     * @param seniorName 老人姓名（用于生成文件名）
     */
    fun saveSeniorQRCode(
        context: Context,
        bitmap: Bitmap?,
        seniorName: String
    ): Boolean {
        val sanitizedName = seniorName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val fileName = "PulseLink_${sanitizedName}_${System.currentTimeMillis()}"
        return saveQRCodeToGallery(context, bitmap, fileName)
    }
    
    /**
     * 保存登录二维码
     * 
     * @param context Context
     * @param bitmap QR code bitmap
     * @param seniorId 老人ID
     */
    fun saveLoginQRCode(
        context: Context,
        bitmap: Bitmap?,
        seniorId: String
    ): Boolean {
        val fileName = "PulseLink_Login_${seniorId}_${System.currentTimeMillis()}"
        return saveQRCodeToGallery(context, bitmap, fileName)
    }
}
