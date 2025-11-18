package com.alvin.pulselink.presentation.home

import com.alvin.pulselink.domain.model.HealthData

data class HomeUiState(
    val username: String = "Mrs. Zhang",
    val healthData: HealthData? = null,
    val remindersCount: Int = 3,
    val isLoading: Boolean = false,
    val error: String? = null
)
