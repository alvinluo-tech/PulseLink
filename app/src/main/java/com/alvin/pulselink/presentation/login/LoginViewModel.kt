package com.alvin.pulselink.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.UserRole
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
    private val loginUseCase: LoginUseCase
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
                    it.copy(error = "Please enter username and password")
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
            
            val result = loginUseCase(
                username = currentState.username,
                password = currentState.password,
                role = role
            )
            
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isLoading = false, isSuccess = true, error = null)
                } else {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        error = result.exceptionOrNull()?.message ?: "Login failed"
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
