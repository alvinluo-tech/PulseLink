package com.alvin.pulselink.core.constants

/**
 * 认证相关常量
 */
object AuthConstants {
    
    // ===== SNR-ID 格式定义 =====
    
    /**
     * Senior ID 正则表达式
     * 格式：SNR-{12位大写字母和数字}
     * 示例：SNR-KXM2VQW7ABCD
     */
    val SNR_ID_REGEX = Regex("^SNR-[A-Z0-9]{12}$")
    
    /**
     * Senior ID 前缀
     */
    const val SNR_ID_PREFIX = "SNR-"
    
    /**
     * Senior ID 内容长度（不含前缀）
     */
    const val SNR_ID_CONTENT_LENGTH = 12
    
    /**
     * Senior ID 完整长度（含前缀）
     */
    const val SNR_ID_FULL_LENGTH = 16  // "SNR-" + 12
    
    // ===== 虚拟邮箱配置 =====
    
    /**
     * 虚拟邮箱域名
     */
    const val VIRTUAL_EMAIL_DOMAIN = "pulselink.app"
    
    /**
     * 虚拟邮箱前缀
     */
    const val VIRTUAL_EMAIL_PREFIX = "senior_"
    
    /**
     * 生成虚拟邮箱地址
     * @param seniorId Senior ID (例如：SNR-KXM2VQW7ABCD)
     * @return 虚拟邮箱地址 (例如：senior_SNR-KXM2VQW7ABCD@pulselink.app)
     */
    fun generateVirtualEmail(seniorId: String): String {
        return "$VIRTUAL_EMAIL_PREFIX$seniorId@$VIRTUAL_EMAIL_DOMAIN"
    }
    
    /**
     * 从虚拟邮箱中提取 Senior ID
     * @param email 虚拟邮箱地址
     * @return Senior ID 或 null
     */
    fun extractSeniorIdFromEmail(email: String): String? {
        val pattern = Regex("^${VIRTUAL_EMAIL_PREFIX}(SNR-[A-Z0-9]{12})@$VIRTUAL_EMAIL_DOMAIN$")
        return pattern.find(email)?.groupValues?.getOrNull(1)
    }
    
    // ===== 注册类型 =====
    
    /**
     * 自主注册
     */
    const val REG_TYPE_SELF = "SELF_REGISTERED"
    
    /**
     * Caregiver 创建
     */
    const val REG_TYPE_CAREGIVER = "CAREGIVER_CREATED"
    
    // ===== Firestore 字段名 =====
    
    object Fields {
        const val REGISTRATION_TYPE = "registrationType"
        const val SENIOR_ID = "seniorId"
        const val CAREGIVER_IDS = "caregiverIds"
        const val CREATOR_ID = "creatorId"
        const val AUTH_UID = "uid"
        const val ROLE = "role"
        const val EMAIL = "email"
        const val USERNAME = "username"
        const val PASSWORD = "password"
    }
    
    // ===== 角色定义 =====
    
    object Roles {
        const val SENIOR = "SENIOR"
        const val CAREGIVER = "CAREGIVER"
    }
}
