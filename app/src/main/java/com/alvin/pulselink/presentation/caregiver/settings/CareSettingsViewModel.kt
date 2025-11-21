package com.alvin.pulselink.presentation.caregiver.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.usecase.ChangePasswordUseCase
import com.alvin.pulselink.domain.usecase.DeleteAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 子女端设置页面 ViewModel
 */
@HiltViewModel
class CareSettingsViewModel @Inject constructor(
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CareSettingsUiState())
    val uiState: StateFlow<CareSettingsUiState> = _uiState.asStateFlow()

    fun onNewPasswordChanged(password: String) {
        _uiState.update { it.copy(
            newPassword = password,
            newPasswordError = null
        ) }
    }

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update { it.copy(
            confirmPassword = password,
            confirmPasswordError = null
        ) }
    }

    fun changePassword() {
        // Validate inputs
        val newPassword = _uiState.value.newPassword
        val confirmPassword = _uiState.value.confirmPassword
        
        var hasError = false
        
        // Validate new password
        if (newPassword.isEmpty()) {
            _uiState.update { it.copy(newPasswordError = "Password cannot be empty") }
            hasError = true
        } else if (newPassword.length < 8) {
            _uiState.update { it.copy(newPasswordError = "Password must be at least 8 characters") }
            hasError = true
        } else if (!isPasswordStrong(newPassword)) {
            _uiState.update { it.copy(
                newPasswordError = "Password must include uppercase, lowercase, number, and special character"
            ) }
            hasError = true
        }
        
        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            _uiState.update { it.copy(confirmPasswordError = "Please confirm your password") }
            hasError = true
        } else if (newPassword != confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Passwords do not match") }
            hasError = true
        }
        
        if (hasError) return
        
        // Proceed with password change
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            changePasswordUseCase(newPassword)
                .onSuccess {
                    _uiState.update { it.copy(
                        isLoading = false,
                        successMessage = "Password changed successfully!",
                        newPassword = "",
                        confirmPassword = ""
                    ) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to change password"
                    ) }
                }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            deleteAccountUseCase()
                .onSuccess {
                    _uiState.update { it.copy(
                        isLoading = false,
                        successMessage = "Account deleted successfully"
                    ) }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to delete account"
                    ) }
                }
        }
    }

    private fun isPasswordStrong(password: String): Boolean {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        
        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
