package com.alvin.pulselink.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Avatar Helper
 * Provides appropriate avatar icons based on age and gender
 */
object AvatarHelper {
    
    /**
     * 根据年龄和性别选择合适的头像类型
     */
    fun getAvatarType(age: Int, gender: String): String {
        return when {
            age >= 80 -> if (gender == "Male") "ELDERLY_MALE" else "ELDERLY_FEMALE"
            age >= 60 -> if (gender == "Male") "SENIOR_MALE" else "SENIOR_FEMALE"
            age >= 40 -> if (gender == "Male") "MIDDLE_AGED_MALE" else "MIDDLE_AGED_FEMALE"
            else -> if (gender == "Male") "ADULT_MALE" else "ADULT_FEMALE"
        }
    }
    
    /**
     * 根据头像类型获取对应的图标
     * 使用 Material Icons 的不同图标来表示不同年龄段和性别
     */
    fun getAvatarIcon(avatarType: String): ImageVector {
        return when (avatarType) {
            "ELDERLY_MALE" -> Icons.Default.Elderly
            "ELDERLY_FEMALE" -> Icons.Default.Elderly
            "SENIOR_MALE" -> Icons.Default.Man
            "SENIOR_FEMALE" -> Icons.Default.Woman
            "MIDDLE_AGED_MALE" -> Icons.Default.Man
            "MIDDLE_AGED_FEMALE" -> Icons.Default.Woman
            "ADULT_MALE" -> Icons.Default.Person
            "ADULT_FEMALE" -> Icons.Default.Person
            else -> Icons.Default.Person // 默认图标
        }
    }
    
    /**
     * 直接根据年龄和性别获取图标（便捷方法）
     */
    fun getAvatarIconByAgeGender(age: Int, gender: String): ImageVector {
        val avatarType = getAvatarType(age, gender)
        return getAvatarIcon(avatarType)
    }
}
