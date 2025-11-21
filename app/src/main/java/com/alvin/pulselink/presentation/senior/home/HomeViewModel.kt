package com.alvin.pulselink.presentation.senior.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.data.local.LocalDataSource
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
    private val authRepository: AuthRepository,
    private val localDataSource: LocalDataSource
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadUserInfo()
        loadHealthData()
    }
    
    private fun loadUserInfo() {
        viewModelScope.launch {
            // 优先读取本地会话（虚拟ID登录时保存了 Senior.name）
            val local = localDataSource.getUser()
            val localName = if (local?.third == "senior") local.second else null

            val displayName = localName ?: "User"

            _uiState.update {
                it.copy(username = displayName)
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
