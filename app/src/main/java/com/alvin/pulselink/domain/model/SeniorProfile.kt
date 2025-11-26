package com.alvin.pulselink.domain.model

/**
 * 老人档案数据模型（简化版）
 * 
 * 方案C重构：将关系管理和健康数据移至独立集合
 * - 关系管理 → CaregiverRelation 集合
 * - 健康数据 → HealthRecord 集合
 */
data class SeniorProfile(
    val id: String = "",                    // 唯一虚拟ID (例如: SNR-KXM2VQW7ABCD)
    val userId: String? = null,             // 关联的 Firebase Auth UID（老人登录后绑定，可为空）
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",                // "Male" or "Female"
    val avatarType: String = "",            // 头像类型标识
    val creatorId: String = "",             // 创建者 UID（护理者代创建时的 UID）
    val createdAt: Long = System.currentTimeMillis(),
    val registrationType: String = "CAREGIVER_CREATED"  // "SELF_REGISTERED" | "CAREGIVER_CREATED"
) {
    companion object {
        const val REGISTRATION_SELF = "SELF_REGISTERED"
        const val REGISTRATION_CAREGIVER = "CAREGIVER_CREATED"
    }
    
    /**
     * 判断是否为自主注册的老人
     */
    val isSelfRegistered: Boolean
        get() = registrationType == REGISTRATION_SELF
    
    /**
     * 判断是否为护理者代创建的老人
     */
    val isCaregiverCreated: Boolean
        get() = registrationType == REGISTRATION_CAREGIVER
}
