package com.alvin.pulselink.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.usecase.GetHealthDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHealthDataUseCase: GetHealthDataUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadUserInfo()
        loadHealthData()
    }
    
    private fun loadUserInfo() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _uiState.update { 
                it.copy(username = user?.username ?: "User")
            }
        }
    }
    
    private fun loadHealthData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = getHealthDataUseCase()
            
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        healthData = result.getOrNull(),
                        isLoading = false,
                        error = null
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }
    
    fun refresh() {
        loadUserInfo()
        loadHealthData()
    }
}
