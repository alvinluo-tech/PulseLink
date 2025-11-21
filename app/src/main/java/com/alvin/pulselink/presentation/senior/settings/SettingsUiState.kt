package com.alvin.pulselink.presentation.senior.settings

enum class FontSize {
    STANDARD,
    LARGE,
    EXTRA_LARGE
}

data class SettingsUiState(
    val fontSize: FontSize = FontSize.STANDARD,
    val shareDataWithFamily: Boolean = true,
    val healthData: Boolean = true,
    val activityData: Boolean = true,
    val isLoading: Boolean = false
)
