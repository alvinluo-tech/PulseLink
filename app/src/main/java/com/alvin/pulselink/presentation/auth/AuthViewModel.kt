package com.alvin.pulselink.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.core.constants.AuthConstants
import com.alvin.pulselink.domain.model.UserRole
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.usecase.LoginUseCase
import com.alvin.pulselink.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 统一的认证 ViewModel
 * 处理登录和注册逻辑，根据角色参数区分老人端和子女端
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val authRepository: AuthRepository,
    private val localDataSource: LocalDataSource
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    // ===== 通用字段更新 =====
    
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, error = null) }
    }
    
    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, error = null) }
    }

    fun onTermsAgreementChange(agreed: Boolean) {
        _uiState.update { it.copy(agreedToTerms = agreed, error = null) }
    }

    // ===== 老人端专用字段 =====
    fun onVirtualIdChange(id: String) {
        _uiState.update { it.copy(virtualId = id.uppercase().trim(), virtualIdError = null, error = null) }
    }
    
    // ===== 注册专用字段 =====
    
    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username, usernameError = null) }
    }
    
    fun onPhoneNumberChange(phone: String) {
        _uiState.update { it.copy(phoneNumber = phone, phoneError = null) }
    }
    
    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }
    }
    
    // ===== 老人注册专用字段 =====
    
    fun onAgeChange(age: String) {
        val ageInt = age.toIntOrNull() ?: 0
        _uiState.update { it.copy(age = ageInt, ageError = null) }
    }
    
    fun onGenderChange(gender: String) {
        _uiState.update { it.copy(gender = gender, genderError = null) }
    }
    
    // ===== 登录逻辑 =====
    
    /**
     * 登录
     * @param role 用户角色 (SENIOR 或 CAREGIVER)
     */
    fun login(role: UserRole) {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            // 验证输入
            if (currentState.email.isBlank() || currentState.password.isBlank()) {
                _uiState.update {
                    it.copy(
                        error = if (role == UserRole.SENIOR) "请输入账号ID和密码" else "Please enter email and password",
                        emailError = if (currentState.email.isBlank()) {
                            if (role == UserRole.SENIOR) "账号ID不能为空" else "Email is required"
                        } else null,
                        passwordError = if (currentState.password.isBlank()) {
                            if (role == UserRole.SENIOR) "密码不能为空" else "Password is required"
                        } else null
                    )
                }
                return@launch
            }
            
            // ⭐ 老人端：验证输入格式（邮箱或SNR-ID）
            if (role == UserRole.SENIOR) {
                val input = currentState.email.trim()
                val isSNRID = input.matches(AuthConstants.SNR_ID_REGEX)
                val isEmail = isValidEmail(input)
                
                if (!isSNRID && !isEmail) {
                    _uiState.update {
                        it.copy(
                            error = "请输入有效的邮箱或老人ID（SNR-XXXXXXXXXXXX）",
                            emailError = "格式不正确"
                        )
                    }
                    return@launch
                }
            }
            
            // ⭐ 子女端：验证电子邮件格式
            if (role == UserRole.CAREGIVER) {
                if (!isValidEmail(currentState.email)) {
                    _uiState.update {
                        it.copy(
                            error = "Please enter a valid email address",
                            emailError = "Invalid email format"
                        )
                    }
                    return@launch
                }
                
                if (!currentState.agreedToTerms) {
                    _uiState.update {
                        it.copy(error = "Please agree to the Privacy Policy and Terms of Service")
                    }
                    return@launch
                }
            }
            
            _uiState.update { it.copy(isLoading = true, error = null, emailError = null, passwordError = null) }
            
            // 执行登录
            val result = loginUseCase(
                email = currentState.email,
                password = currentState.password
            )
            
            if (result.isSuccess) {
                // 检查邮箱验证状态（未验证则阻止登录并提示重发）
                val isVerified = authRepository.isEmailVerified()
                if (!isVerified) {
                    authRepository.logout()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            showResendVerification = true,
                            error = "Email not verified. Please check your inbox and click the verification link before logging in."
                        )
                    }
                    return@launch
                }

                // 读取用户信息，校验角色是否与当前登录端一致
                val user = authRepository.getCurrentUser()
                if (user == null) {
                    authRepository.logout()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            showResendVerification = false,
                            error = "Failed to retrieve user info. Please try again."
                        )
                    }
                    return@launch
                }

                if (user.role != role) {
                    // 账号角色与选择的端不一致，阻止跨端登录
                    authRepository.logout()
                    val expected = if (role.name == "SENIOR") "senior" else "caregiver"
                    val actual = if (user.role.name == "SENIOR") "senior" else "caregiver"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            showResendVerification = false,
                            error = "Role mismatch: this account is for $actual. Please login on the $expected side."
                        )
                    }
                    return@launch
                }

                // 成功且角色匹配
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null,
                        showResendVerification = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        showResendVerification = false,
                        error = result.exceptionOrNull()?.message ?: "Login failed"
                    )
                }
            }
        }
    }

    /**
     * 老人端登录：通过虚拟ID和密码
     * 
     * 流程:
     * 1. 输入虚拟ID（例如：SNR-KXM2VQW7ABCD）和密码
     * 2. 自动拼接邮箱：senior_SNR-KXM2VQW7ABCD@pulselink.app
     * 3. 调用标准的 Firebase Auth 邮箱登录
     * 4. 验证角色是否为 SENIOR
     */
    fun loginSenior() {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            // 验证虚拟ID和密码
            if (currentState.virtualId.isBlank() || currentState.password.isBlank()) {
                _uiState.update {
                    it.copy(
                        error = "请输入账号ID和密码",
                        virtualIdError = if (currentState.virtualId.isBlank()) "账号ID不能为空" else null,
                        passwordError = if (currentState.password.isBlank()) "密码不能为空" else null
                    )
                }
                return@launch
            }
            
            // 验证虚拟ID格式
            if (!currentState.virtualId.matches(AuthConstants.SNR_ID_REGEX)) {
                _uiState.update {
                    it.copy(
                        error = "ID格式不正确，应为 SNR-XXXXXXXXXXXX",
                        virtualIdError = "ID格式不正确"
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // 自动拼接邮箱：senior_{虚拟ID}@pulselink.app
            val email = AuthConstants.generateVirtualEmail(currentState.virtualId)
            
            // 执行登录（使用标准的邮箱密码登录）
            val result = loginUseCase(
                email = email,
                password = currentState.password
            )
            
            if (result.isSuccess) {
                // 验证角色是否为 SENIOR
                val user = authRepository.getCurrentUser()
                if (user == null) {
                    authRepository.logout()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            error = "获取用户信息失败，请重试"
                        )
                    }
                    return@launch
                }

                if (user.role != UserRole.SENIOR) {
                    // 角色不是老人，阻止登录
                    authRepository.logout()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            error = "此账户不是老人账户，请使用正确的登录端"
                        )
                    }
                    return@launch
                }

                // 登录成功
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        error = result.exceptionOrNull()?.message ?: "登录失败，请检查账号ID和密码"
                    )
                }
            }
        }
    }
    
    /**
     * 从二维码数据中解析登录信息
     * 
     * QR Code JSON 格式:
     * {
     *   "type": "pulselink_login",
     *   "email": "senior_SNR-XXXXXXXXXXXX@pulselink.app",
     *   "password": "xxxxxxxx"
     * }
     */
    /**
     * 解析二维码并自动登录
     * 
     * 支持格式：
     * {
     *   "type": "pulselink_login",
     *   "id": "SNR-XXXXXXXXXXXX",
     *   "password": "xxxxxxxx"
     * }
     */
    fun parseQRCodeAndLogin(qrCodeData: String) {
        try {
            // 解析 ID 和密码（支持新格式）
            val idPattern = """"id"\s*:\s*"([^"]+)"""".toRegex()
            val passwordPattern = """"password"\s*:\s*"([^"]+)"""".toRegex()
            
            val idMatch = idPattern.find(qrCodeData)
            val passwordMatch = passwordPattern.find(qrCodeData)
            
            if (idMatch != null && passwordMatch != null) {
                val seniorId = idMatch.groupValues[1]
                val password = passwordMatch.groupValues[1]
                
                // 更新状态并登录（使用 virtualId）
                _uiState.update {
                    it.copy(
                        virtualId = seniorId,
                        password = password
                    )
                }
                
                // 自动登录
                loginSenior()
            } else {
                // 兼容旧格式（email 字段）
                val emailPattern = """"email"\s*:\s*"([^"]+)"""".toRegex()
                val emailMatch = emailPattern.find(qrCodeData)
                
                if (emailMatch != null && passwordMatch != null) {
                    val email = emailMatch.groupValues[1]
                    val password = passwordMatch.groupValues[1]
                    
                    // 从邮箱中提取 ID（格式：senior_SNR-XXXXXXXXXXXX@pulselink.app）
                    val extractedId = email.substringAfter("senior_").substringBefore("@")
                    
                    _uiState.update {
                        it.copy(
                            virtualId = extractedId,
                            password = password
                        )
                    }
                    
                    loginSenior()
                } else {
                    _uiState.update {
                        it.copy(error = "二维码格式不正确")
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(error = "解析二维码失败: ${e.message}")
            }
        }
    }

    // ===== 注册逻辑 =====
    
    /**
     * 注册 - Caregiver
     * @param role 用户角色 (CAREGIVER)
     */
    fun register(role: UserRole) {
        if (!validateRegistrationInputs()) {
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val currentState = _uiState.value
            val result = registerUseCase(
                email = currentState.email,
                password = currentState.password,
                username = currentState.username,
                role = role
            )
            
            _uiState.update { 
                if (result.isSuccess) {
                    it.copy(
                        isLoading = false,
                        registrationSuccess = true,
                        error = "Registration successful! Please check your email to verify your account."
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Registration failed"
                    )
                }
            }
        }
    }
    
    /**
     * 老人自主注册
     * 需要额外的年龄和性别信息
     */
    fun registerSenior() {
        if (!validateSeniorRegistrationInputs()) {
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val currentState = _uiState.value
            val result = authRepository.registerSenior(
                email = currentState.email,
                password = currentState.password,
                name = currentState.username,  // 老人使用真实姓名
                age = currentState.age,
                gender = currentState.gender
            )
            
            _uiState.update { 
                if (result.isSuccess) {
                    it.copy(
                        isLoading = false,
                        registrationSuccess = true,
                        error = "Registration successful! Please check your email to verify your account."
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Registration failed"
                    )
                }
            }
        }
    }
    
    // ===== 邮箱验证 =====
    
    /**
     * 重新发送验证邮件
     */
    fun resendVerificationEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // 先临时登录以发送验证邮件
            val loginResult = loginUseCase(
                email = _uiState.value.email,
                password = _uiState.value.password
            )
            
            if (loginResult.isSuccess) {
                val result = authRepository.sendEmailVerification()
                authRepository.logout()  // 发送后立即登出
                
                _uiState.update {
                    if (result.isSuccess) {
                        it.copy(
                            isLoading = false,
                            error = "Verification email sent! Please check your inbox."
                        )
                    } else {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to send verification email"
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to resend verification email"
                    )
                }
            }
        }
    }
    
    // ===== 辅助函数 =====
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    private fun validateRegistrationInputs(): Boolean {
        val currentState = _uiState.value
        var isValid = true
        
        // 验证用户名
        if (currentState.username.isBlank()) {
            _uiState.update { it.copy(usernameError = "Username is required") }
            isValid = false
        } else if (currentState.username.length < 3) {
            _uiState.update { it.copy(usernameError = "Username must be at least 3 characters") }
            isValid = false
        }
        
        // 验证邮箱
        if (currentState.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email is required") }
            isValid = false
        } else if (!isValidEmail(currentState.email)) {
            _uiState.update { it.copy(emailError = "Invalid email format") }
            isValid = false
        }
        
        // 验证电话号码
        if (currentState.phoneNumber.isBlank()) {
            _uiState.update { it.copy(phoneError = "Phone number is required") }
            isValid = false
        } else if (currentState.phoneNumber.length < 10) {
            _uiState.update { it.copy(phoneError = "Invalid phone number") }
            isValid = false
        }
        
        // 验证密码
        if (currentState.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password is required") }
            isValid = false
        } else if (currentState.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            isValid = false
        }
        
        // 验证确认密码
        if (currentState.confirmPassword.isBlank()) {
            _uiState.update { it.copy(confirmPasswordError = "Please confirm your password") }
            isValid = false
        } else if (currentState.password != currentState.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Passwords do not match") }
            isValid = false
        }
        
        // 验证条款同意
        if (!currentState.agreedToTerms) {
            _uiState.update { it.copy(error = "Please agree to the Privacy Policy and Terms of Service") }
            isValid = false
        }
        
        return isValid
    }
    
    /**
     * 验证老人注册输入
     */
    private fun validateSeniorRegistrationInputs(): Boolean {
        val currentState = _uiState.value
        var isValid = true
        
        // 验证姓名
        if (currentState.username.isBlank()) {
            _uiState.update { it.copy(usernameError = "Name is required") }
            isValid = false
        } else if (currentState.username.length < 2) {
            _uiState.update { it.copy(usernameError = "Name must be at least 2 characters") }
            isValid = false
        }
        
        // 验证年龄
        if (currentState.age <= 0) {
            _uiState.update { it.copy(ageError = "Please enter a valid age") }
            isValid = false
        } else if (currentState.age < 18 || currentState.age > 120) {
            _uiState.update { it.copy(ageError = "Age must be between 18 and 120") }
            isValid = false
        }
        
        // 验证性别
        if (currentState.gender.isBlank()) {
            _uiState.update { it.copy(genderError = "Please select gender") }
            isValid = false
        }
        
        // 验证邮箱
        if (currentState.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email is required") }
            isValid = false
        } else if (!isValidEmail(currentState.email)) {
            _uiState.update { it.copy(emailError = "Invalid email format") }
            isValid = false
        }
        
        // 验证密码
        if (currentState.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password is required") }
            isValid = false
        } else if (currentState.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            isValid = false
        }
        
        // 验证确认密码
        if (currentState.confirmPassword.isBlank()) {
            _uiState.update { it.copy(confirmPasswordError = "Please confirm your password") }
            isValid = false
        } else if (currentState.password != currentState.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Passwords do not match") }
            isValid = false
        }
        
        // 验证条款同意
        if (!currentState.agreedToTerms) {
            _uiState.update { it.copy(error = "Please agree to the Privacy Policy and Terms of Service") }
            isValid = false
        }
        
        return isValid
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun resetState() {
        _uiState.value = AuthUiState()
    }
}
