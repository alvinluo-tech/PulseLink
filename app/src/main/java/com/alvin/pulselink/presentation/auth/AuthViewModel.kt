package com.alvin.pulselink.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.UserRole
import com.alvin.pulselink.domain.repository.SeniorRepository
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
    private val seniorRepository: SeniorRepository,
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
                    it.copy(error = "Please enter email and password")
                }
                return@launch
            }
            
            // 验证电子邮件格式
            if (!isValidEmail(currentState.email)) {
                _uiState.update {
                    it.copy(error = "Please enter a valid email address")
                }
                return@launch
            }
            
            if (!currentState.agreedToTerms) {
                _uiState.update {
                    it.copy(error = "Please agree to the Privacy Policy and Terms of Service")
                }
                return@launch
            }
            
            _uiState.update { it.copy(isLoading = true, error = null) }
            
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
     * 老人端登录：通过虚拟ID验证，无需邮箱与密码
     */
    fun loginSeniorById() {
        viewModelScope.launch {
            val id = _uiState.value.virtualId

            // 输入校验
            if (id.isBlank()) {
                _uiState.update { it.copy(virtualIdError = "请输入虚拟ID") }
                return@launch
            }
            if (!id.matches(Regex("^SNR-[A-Z0-9]{8}$"))) {
                _uiState.update { it.copy(virtualIdError = "ID格式不正确，应为 SNR-XXXXXXXX") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            // 查询老人账户是否存在
            seniorRepository.getSeniorById(id)
                .onSuccess { senior ->
                    // 保存本地会话（仅用于应用内导航与状态）
                    localDataSource.saveUser(
                        id = senior.id,
                        username = senior.name,
                        role = "senior"
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            error = null,
                            virtualIdError = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            error = error.message ?: "未找到该老人ID，请检查后重试"
                        )
                    }
                }
        }
    }
    
    // ===== 注册逻辑 =====
    
    /**
     * 注册
     * @param role 用户角色 (SENIOR 或 CAREGIVER)
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
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun resetState() {
        _uiState.value = AuthUiState()
    }
}
