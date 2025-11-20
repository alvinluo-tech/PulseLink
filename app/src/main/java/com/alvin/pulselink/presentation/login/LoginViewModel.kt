package com.alvin.pulselink.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.UserRole
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username, error = null) }
    }
    
    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }
    
    fun onTermsAgreementChange(agreed: Boolean) {
        _uiState.update { it.copy(agreedToTerms = agreed, error = null) }
    }
    
    fun login(role: UserRole) {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            if (currentState.username.isBlank() || currentState.password.isBlank()) {
                _uiState.update {
                    it.copy(error = "Please enter email and password")
                }
                return@launch
            }
            
            // 验证电子邮件格式
            if (!isValidEmail(currentState.username)) {
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
            
            // 使用 email 而不是 username 登录
            val result = loginUseCase(
                email = currentState.username,
                password = currentState.password
            )
            
            _uiState.update {
                if (result.isSuccess) {
                    // 检查邮箱验证状态
                    val isVerified = authRepository.isEmailVerified()
                    if (!isVerified) {
                        // 邮箱未验证，立即登出用户
                        authRepository.logout()
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            showResendVerification = true,  // 显示重新发送按钮
                            error = "Email not verified. Please check your inbox and click the verification link before logging in."
                        )
                    } else {
                        it.copy(
                            isLoading = false, 
                            isSuccess = true, 
                            error = null,
                            showResendVerification = false
                        )
                    }
                } else {
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
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    fun resendVerificationEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // 先临时登录以发送验证邮件
            val loginResult = loginUseCase(
                email = _uiState.value.username,
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
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
