package com.alvin.pulselink.presentation.common.state

/**
 * Error Dialog State (persisted via StateFlow)
 * Used for critical errors that require user acknowledgment.
 */
data class ErrorDialogState(
    val title: String,
    val message: String
)
