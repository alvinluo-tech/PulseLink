package com.alvin.pulselink.domain.model

/**
 * 老人账户数据模型
 */
data class Senior(
    val id: String = "", // 唯一虚拟ID (例如: SNR-ABCD1234)
    val name: String = "",
    val age: Int = 0,
    val gender: String = "", // "Male" or "Female"
    val healthHistory: HealthHistory = HealthHistory(),
    val caregiverIds: List<String> = emptyList(), // 关联的多个 caregiver ID
    val creatorId: String = "", // 创建者 caregiver ID
    val createdAt: Long = System.currentTimeMillis(),
    val password: String = "" // 登录密码（加密存储，用于生成二维码）
)

/**
 * 健康历史数据
 */
data class HealthHistory(
    val bloodPressure: BloodPressureRecord? = null,
    val heartRate: Int? = null,
    val bloodSugar: Double? = null,
    val medicalConditions: List<String> = emptyList(), // 既往病史
    val medications: List<String> = emptyList(), // 当前用药
    val allergies: List<String> = emptyList() // 过敏史
)

/**
 * 血压记录
 */
data class BloodPressureRecord(
    val systolic: Int, // 收缩压
    val diastolic: Int, // 舒张压
    val recordedAt: Long = System.currentTimeMillis()
)
