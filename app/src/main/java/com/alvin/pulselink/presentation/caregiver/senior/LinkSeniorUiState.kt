package com.alvin.pulselink.presentation.caregiver.senior

import com.alvin.pulselink.domain.model.Senior

/**
 * 绑定老人账户 UI 状态
 */
data class LinkSeniorUiState(
    val seniorId: String = "",
    val seniorIdError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val linkedSeniors: List<Senior> = emptyList(),
    val isLoadingList: Boolean = false
)
