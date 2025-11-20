package com.alvin.pulselink.presentation.profile

data class ProfileUiState(
    val userName: String = "Mrs. Zhang",
    val age: Int = 65,
    val daysUsed: Int = 30,
    val bloodPressure: String = "120/80",
    val bloodPressureStatus: String = "BP Normal",
    val heartRate: Int = 72,
    val isLoading: Boolean = false
)
