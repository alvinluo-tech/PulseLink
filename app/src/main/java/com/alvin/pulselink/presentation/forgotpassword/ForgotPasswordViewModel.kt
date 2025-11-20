package com.alvin.pulselink.presentation.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.usecase.ResetPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()
    
    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }
    
    fun sendResetCode() {
        if (!validateEmail()) {
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = resetPasswordUseCase(_uiState.value.email)
            
            _uiState.update { 
                if (result.isSuccess) {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        errorMessage = "Password reset email sent! Please check your inbox."
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to send reset email"
                    )
                }
            }
        }
    }
    
    private fun validateEmail(): Boolean {
        val currentState = _uiState.value
        
        if (currentState.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email is required") }
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(currentState.email).matches()) {
            _uiState.update { it.copy(emailError = "Invalid email format") }
            return false
        }
        
        return true
    }
}
