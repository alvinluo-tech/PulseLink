package com.alvin.pulselink.domain.model

/**
 * 老人账户数据模型
 */
data class Senior(
    val id: String = "", // 唯一虚拟ID (例如: SNR-ABCD1234)
    val name: String = "",
    val age: Int = 0,
    val gender: String = "", // "Male" or "Female"
    val avatarType: String = "", // 头像类型标识，根据年龄和性别自动选择
    val healthHistory: HealthHistory = HealthHistory(),
    val caregiverIds: List<String> = emptyList(), // 关联的多个 caregiver ID (已批准的)
    val pendingCaregiversIds: List<String> = emptyList(), // 待审核的 caregiver ID
    val caregiverRelationships: Map<String, CaregiverRelationship> = emptyMap(), // 每个护理者与老人的关系映射 (key: caregiverId, value: 关系信息)
    val creatorId: String = "", // 创建者 caregiver ID
    val createdAt: Long = System.currentTimeMillis(),
    val password: String = "" // 登录密码（加密存储，用于生成二维码）
)

/**
 * 护理者与老人的关系信息
 */
data class CaregiverRelationship(
    val relationship: String = "", // 护理者是老人的什么 (例如: "Son", "Daughter", "Grandson", etc.)
    val nickname: String = "", // 护理者对老人的自定义称呼 (例如: "Dad", "Mom", "Grandpa", etc.) - 可选
    val linkedAt: Long = System.currentTimeMillis(), // 绑定时间
    val status: String = "active", // 状态: "pending"(待审核), "active"(已激活), "rejected"(已拒绝)
    val message: String = "" // 申请消息 (例如: "I would like to help monitor your mother's health data.")
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
