package com.alvin.pulselink.util

import kotlin.random.Random

/**
 * SNR-ID 生成器
 * 
 * 格式：SNR-XXXXXXXXXXXX (12位)
 * - 前8位：当前时间戳的 base-36 编码（大写）
 * - 后4位：随机字母（A-Z）
 * 
 * 优势：
 * - 时间排序性：前缀基于时间戳，天然有序
 * - 高唯一性：timestamp + random 组合，碰撞概率极低
 * - 可读性：全大写字母数字，易于输入
 */
object SnrIdGenerator {
    
    private const val PREFIX = "SNR-"
    private const val TIMESTAMP_LENGTH = 8
    private const val RANDOM_LENGTH = 4
    
    /**
     * 生成新的 SNR-ID
     * 
     * @return SNR-ID 字符串，格式：SNR-XXXXXXXXXXXX
     * 
     * 示例：
     * - SNR-KXM2VQW7ABCD
     * - SNR-KXM2VQW7XYZW
     */
    fun generate(): String {
        // 1. 获取当前时间戳（毫秒）
        val timestamp = System.currentTimeMillis()
        
        // 2. 转换为 base-36（使用0-9, A-Z）并取前8位
        val timestampPart = timestamp
            .toString(36)  // 转为 base-36
            .uppercase()   // 转大写
            .takeLast(TIMESTAMP_LENGTH)  // 取最后8位
            .padStart(TIMESTAMP_LENGTH, '0')  // 不足8位补0
        
        // 3. 生成4位随机字母（A-Z）
        val randomPart = (1..RANDOM_LENGTH)
            .map { Random.nextInt(26) + 'A'.code }  // 生成随机大写字母
            .map { it.toChar() }
            .joinToString("")
        
        // 4. 组合
        return "$PREFIX$timestampPart$randomPart"
    }
    
    /**
     * 验证 SNR-ID 格式是否正确
     * 
     * @param snrId 待验证的 SNR-ID
     * @return true 如果格式正确
     */
    fun isValid(snrId: String): Boolean {
        return snrId.matches(Regex("^SNR-[A-Z0-9]{12}$"))
    }
    
    /**
     * 从 SNR-ID 中提取时间戳部分（用于调试）
     * 
     * @param snrId SNR-ID 字符串
     * @return 时间戳的 base-36 字符串
     */
    fun extractTimestampPart(snrId: String): String? {
        if (!isValid(snrId)) return null
        return snrId.substring(4, 12)  // 跳过 "SNR-"，取前8位
    }
    
    /**
     * 从 SNR-ID 中提取随机部分（用于调试）
     * 
     * @param snrId SNR-ID 字符串
     * @return 随机部分的4位字符串
     */
    fun extractRandomPart(snrId: String): String? {
        if (!isValid(snrId)) return null
        return snrId.substring(12)  // 取最后4位
    }
}
