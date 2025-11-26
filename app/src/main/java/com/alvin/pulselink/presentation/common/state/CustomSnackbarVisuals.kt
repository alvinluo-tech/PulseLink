package com.alvin.pulselink.presentation.common.state

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals

/**
 * 自定义 Snackbar Visuals
 * 扩展标准 SnackbarVisuals 以支持自定义类型和样式
 */
data class CustomSnackbarVisuals(
    override val message: String,
    val type: SnackbarType = SnackbarType.INFO,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = true,
    override val duration: SnackbarDuration = SnackbarDuration.Short
) : SnackbarVisuals
