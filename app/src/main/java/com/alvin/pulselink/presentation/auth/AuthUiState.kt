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
    // 老人端登录字段（虚拟ID）
    val virtualId: String = "",
    
    // 注册额外字段
    val username: String = "",
    val phoneNumber: String = "",
    val confirmPassword: String = "",
    
    // 老人注册专用字段
    val age: Int = 0,
    val gender: String = "",  // "Male" or "Female"
    
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
    val virtualIdError: String? = null,
    val ageError: String? = null,
    val genderError: String? = null,
    
    // 注册成功标志
    val registrationSuccess: Boolean = false
)
