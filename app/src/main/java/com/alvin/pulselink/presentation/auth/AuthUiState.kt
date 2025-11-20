package com.alvin.pulselink.presentation.auth

/**
 * 统一的认证 UI 状态
 * 同时支持登录和注册功能
 */
data class AuthUiState(
    // 登录字段
    val email: String = "",
    val password: String = "",
    val agreedToTerms: Boolean = false,
    
    // 注册额外字段
    val username: String = "",
    val phoneNumber: String = "",
    val confirmPassword: String = "",
    
    // 状态
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val showResendVerification: Boolean = false,
    
    // 错误信息
    val error: String? = null,
    val emailError: String? = null,
    val usernameError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    
    // 注册成功标志
    val registrationSuccess: Boolean = false
)
