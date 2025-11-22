package com.alvin.pulselink.presentation.senior.home

import com.alvin.pulselink.domain.model.HealthData

data class HomeUiState(
    val username: String = "User",  // 默认用户名，会从 Firebase 加载真实数据
    val healthData: HealthData? = null,
    val nextReminderTime: String = "No reminders",  // 最近一次提醒的时间
    val isLoading: Boolean = false,
    val error: String? = null
)
