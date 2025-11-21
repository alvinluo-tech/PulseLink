package com.alvin.pulselink.domain.model

/**
 * 老人账户数据模型
 */
data class Senior(
    val id: String = "", // 唯一虚拟ID
    val name: String = "",
    val age: Int = 0,
    val gender: String = "", // "Male" or "Female"
    val healthHistory: HealthHistory = HealthHistory(),
    val caregiverIds: List<String> = emptyList(), // 关联的多个 caregiver ID
    val creatorId: String = "", // 创建者 caregiver ID
    val createdAt: Long = System.currentTimeMillis()
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
