package com.alvin.pulselink.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.UserRole
import com.alvin.pulselink.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()
    
    fun onUsernameChanged(username: String) {
        _uiState.update { it.copy(username = username, usernameError = null) }
    }
    
    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }
    
    fun onPhoneNumberChanged(phone: String) {
        _uiState.update { it.copy(phoneNumber = phone, phoneError = null) }
    }
    
    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }
    
    fun onConfirmPasswordChanged(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }
    }
    
    fun onAgreeToTermsChanged(agree: Boolean) {
        _uiState.update { it.copy(agreeToTerms = agree) }
    }
    
    fun register(userRole: UserRole) {
        if (!validateInputs()) {
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val currentState = _uiState.value
            val result = registerUseCase(
                email = currentState.email,
                password = currentState.password,
                username = currentState.username,
                role = userRole
            )
            
            _uiState.update { 
                if (result.isSuccess) {
                    it.copy(
                        isLoading = false,
                        registrationSuccess = true,
                        errorMessage = "Registration successful! Please check your email to verify your account."
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Registration failed"
                    )
                }
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        val currentState = _uiState.value
        var isValid = true
        
        // Validate username
        if (currentState.username.isBlank()) {
            _uiState.update { it.copy(usernameError = "Username is required") }
            isValid = false
        } else if (currentState.username.length < 3) {
            _uiState.update { it.copy(usernameError = "Username must be at least 3 characters") }
            isValid = false
        }
        
        // Validate email
        if (currentState.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email is required") }
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(currentState.email).matches()) {
            _uiState.update { it.copy(emailError = "Invalid email format") }
            isValid = false
        }
        
        // Validate phone number
        if (currentState.phoneNumber.isBlank()) {
            _uiState.update { it.copy(phoneError = "Phone number is required") }
            isValid = false
        } else if (currentState.phoneNumber.length < 10) {
            _uiState.update { it.copy(phoneError = "Invalid phone number") }
            isValid = false
        }
        
        // Validate password
        if (currentState.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password is required") }
            isValid = false
        } else if (currentState.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            isValid = false
        }
        
        // Validate confirm password
        if (currentState.confirmPassword.isBlank()) {
            _uiState.update { it.copy(confirmPasswordError = "Please confirm your password") }
            isValid = false
        } else if (currentState.password != currentState.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Passwords do not match") }
            isValid = false
        }
        
        // Validate terms agreement
        if (!currentState.agreeToTerms) {
            _uiState.update { it.copy(errorMessage = "Please agree to the Privacy Policy and Terms of Service") }
            isValid = false
        }
        
        return isValid
    }
}
