package com.alvin.pulselink.presentation.health

data class HealthDataUiState(
    val systolicPressure: String = "",
    val diastolicPressure: String = "",
    val heartRate: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)
