package com.alvin.pulselink.presentation.senior.history

data class HealthRecord(
    val id: Long,
    val date: String,
    val time: String,
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int,
    val status: HealthStatus
)

enum class HealthStatus {
    NORMAL,
    HIGH,
    LOW
}

data class HealthHistoryUiState(
    val records: List<HealthRecord> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
