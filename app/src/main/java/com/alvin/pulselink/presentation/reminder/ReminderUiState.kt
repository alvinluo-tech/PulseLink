package com.alvin.pulselink.presentation.reminder

data class ReminderUiState(
    val reminder: MedicationReminder? = null,
    val isLoading: Boolean = false
)

data class MedicationReminder(
    val medicationName: String,
    val dosage: String,
    val time: String,
    val isTaken: Boolean = false
)
