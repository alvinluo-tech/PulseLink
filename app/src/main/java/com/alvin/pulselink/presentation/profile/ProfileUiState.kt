package com.alvin.pulselink.presentation.profile

data class ProfileUiState(
    val userName: String = "User",  // 默认用户名，会从 Firebase 加载真实数据
    val age: Int = 65,
    val daysUsed: Int = 30,
    val bloodPressure: String = "120/80",
    val bloodPressureStatus: String = "BP Normal",
    val heartRate: Int = 72,
    val isLoading: Boolean = false
)
