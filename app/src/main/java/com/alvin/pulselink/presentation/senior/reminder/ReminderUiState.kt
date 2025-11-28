package com.alvin.pulselink.presentation.senior.reminder

data class ReminderUiState(
    val medicationBatch: MedicationBatch? = null,
    val isLoading: Boolean = false,
    // 保留旧字段以向后兼容（已废弃）
    @Deprecated("使用 medicationBatch 代替")
    val reminder: MedicationReminder? = null
)

@Deprecated("使用 MedicationBatch + MedicationItem 代替")
data class MedicationReminder(
    val medicationName: String,
    val dosage: String,
    val time: String,
    val isTaken: Boolean = false
)
