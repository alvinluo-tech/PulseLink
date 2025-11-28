package com.alvin.pulselink.domain.model

import java.util.UUID

/**
 * Medication Reminder - 用药提醒（静态规则）
 * Firestore Path: /reminders/{reminderId}
 * 
 * 核心设计原则：
 * 1. 这是"课表"，定义了"应该在什么时候吃什么药"
 * 2. 由护工端创建和管理，老人端只读（除了 currentStock）
 * 3. status 只有 ACTIVE/PAUSED，用于管理是否提醒
 * 4. "已吃/未吃"等动态状态不存储在这里，而是从 MedicationLog 实时计算
 * 5. 唯一例外：currentStock 会在老人标记 TAKEN 时原子化递减
 */
data class MedicationReminder(
    val id: String = UUID.randomUUID().toString(),
    val seniorId: String = "",
    
    // --- 基本信息 ---
    val name: String = "",
    val nickname: String? = null,
    val imageUrl: String? = null, // 图片路径
    val iconType: MedicationIconType = MedicationIconType.PILL, // 默认药片图标
    val colorHex: String? = null, // "#FF5722"
    
    // --- 剂量与用法 ---
    val dosage: Double = 1.0,
    val unit: String = "片", // 本地化字符串
    val instruction: IntakeInstruction = IntakeInstruction.NONE,
    
    // --- 调度逻辑 ---
    val frequency: FrequencyType = FrequencyType.DAILY,
    val specificWeekDays: List<Int> = emptyList(), // 1=Mon, 7=Sun
    val intervalDays: Int = 0,
    val timeSlots: List<String> = emptyList(), // ["08:00", "20:00"] 24h格式
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    
    // --- 库存管理 ---
    val currentStock: Int = 0, // ★ 唯一会被老人操作影响的字段：每次标记 TAKEN 时原子化递减
    val lowStockThreshold: Int = 5,
    val enableStockAlert: Boolean = true,
    
    // --- 状态 ---
    val status: ReminderStatus = ReminderStatus.ACTIVE,
    val createdBy: String = "", // Caregiver UID
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 药物图标类型
 */
enum class MedicationIconType {
    PILL,       // 药片
    CAPSULE,    // 胶囊
    BOTTLE,     // 药瓶
    INJECTION,  // 注射
    POWDER      // 粉末
}

/**
 * 服用说明
 */
enum class IntakeInstruction {
    NONE,           // 无特殊要求
    BEFORE_MEAL,    // 饭前
    AFTER_MEAL,     // 饭后
    WITH_FOOD,      // 随餐
    BEFORE_SLEEP    // 睡前
}

/**
 * 频率类型
 */
enum class FrequencyType {
    DAILY,          // 每天
    SPECIFIC_DAYS,  // 特定星期几
    INTERVAL        // 间隔天数
}

/**
 * 提醒管理状态 (仅用于护工端管理，不代表用药完成状态)
 * 注意："是否吃药" 的状态应该从 MedicationLog 动态计算，不存储在 Reminder 中
 */
enum class ReminderStatus {
    ACTIVE,     // 激活中（正在提醒）
    PAUSED      // 已暂停（临时停止提醒）
}

/**
 * 用药记录 - 单次服药的记录（动态状态的唯一来源）
 * Firestore Path: /medication_logs/{logId}
 * 
 * 设计原则：
 * - 这是"签到表"，记录每次实际的用药行为
 * - UI 的"已吃/未吃/错过"状态应该通过拉链算法从这里计算
 * - 由老人端写入，护工端只读
 */
data class MedicationLog(
    val id: String = UUID.randomUUID().toString(),
    val reminderId: String = "",
    val seniorId: String = "",
    val scheduledTime: Long = 0L, // 计划时间
    val takenTime: Long? = null, // 实际服用时间
    val status: MedicationLogStatus = MedicationLogStatus.PENDING,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 用药记录状态
 */
enum class MedicationLogStatus {
    PENDING,    // 待服用
    TAKEN,      // 已服用
    MISSED,     // 已错过
    SKIPPED     // 已跳过
}
