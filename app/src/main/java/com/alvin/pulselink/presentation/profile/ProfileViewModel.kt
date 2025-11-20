package com.alvin.pulselink.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadProfileData()
    }
    
    private fun loadProfileData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // 从 AuthRepository 获取真实的用户信息
            val user = authRepository.getCurrentUser()
            
            _uiState.update { 
                it.copy(
                    userName = user?.username ?: "User",
                    isLoading = false
                )
            }
        }
    }
}
