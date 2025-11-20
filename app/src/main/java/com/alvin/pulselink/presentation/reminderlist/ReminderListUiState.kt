package com.alvin.pulselink.presentation.reminderlist

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
    val status: ReminderStatus
)

enum class ReminderStatus {
    TAKEN,
    PENDING,
    MISSED
}
