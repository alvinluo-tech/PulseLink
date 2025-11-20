package com.alvin.pulselink.presentation.login

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val agreedToTerms: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val showResendVerification: Boolean = false  // 是否显示重新发送验证邮件按钮
)
