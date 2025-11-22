package com.alvin.pulselink.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * 二维码生成工具
 */
object QRCodeGenerator {
    
    /**
     * 生成二维码 Bitmap
     * 
     * @param content 二维码内容（JSON 字符串）
     * @param size 二维码尺寸（像素，默认 512）
     * @return Bitmap 对象，失败返回 null
     */
    fun generateQRCode(content: String, size: Int = 512): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1 // 边距
            )
            
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )
            
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(
                        x, 
                        y, 
                        if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    )
                }
            }
            
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
