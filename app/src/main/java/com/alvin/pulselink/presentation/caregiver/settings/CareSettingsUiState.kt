package com.alvin.pulselink.presentation.caregiver.settings

/**
 * 子女端设置页面 UI 状态
 */
data class CareSettingsUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
