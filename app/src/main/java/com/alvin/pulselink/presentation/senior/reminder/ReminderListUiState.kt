package com.alvin.pulselink.presentation.senior.reminder

import java.time.LocalDate

data class ReminderListUiState(
    val date: LocalDate = LocalDate.now(),
    val reminders: List<ReminderItem> = emptyList(),
    val takenCount: Int = 0,
    val pendingCount: Int = 0,
    val missedCount: Int = 0
)

data class ReminderItem(
    val id: Int,
    val time: String,
    val medicationName: String,
    val dosage: String,
    val status: ReminderStatus,
    val logId: String? = null,
    val reminderId: String? = null
)

enum class ReminderStatus {
    TAKEN,      // 已服用
    PENDING,    // 待服用
    MISSED,     // 错过/忘记
    SKIPPED     // 主动跳过
}

/**
 * 批量服药数据模型
 */
data class MedicationBatch(
    val timeLabel: String,        // "早晨"、"上午"、"下午"、"傍晚"
    val primaryTime: String,       // "08:00"
    val medications: List<MedicationItem>
)

/**
 * 单个药物项（可勾选）
 */
data class MedicationItem(
    val reminderId: String,
    val logId: String?,
    val name: String,
    val dosage: String,
    val time: String,
    val isChecked: Boolean = true
)
