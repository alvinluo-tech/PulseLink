package com.alvin.pulselink.presentation.caregiver.senior

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.Senior
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.SeniorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 绑定老人账户 ViewModel
 */
@HiltViewModel
class LinkSeniorViewModel @Inject constructor(
    private val seniorRepository: SeniorRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LinkSeniorUiState())
    val uiState: StateFlow<LinkSeniorUiState> = _uiState.asStateFlow()

    init {
        loadLinkedSeniors()
    }

    fun loadLinkedSeniors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingList = true) }
            
            val caregiverId = authRepository.getCurrentUid() ?: ""
            
            // 获取所有绑定的老人（caregiverIds 包含当前用户）
            seniorRepository.getSeniorsByCaregiver(caregiverId)
                .onSuccess { allSeniors ->
                    // 过滤出只绑定的（不是创建的）
                    val linkedOnly = allSeniors.filter { it.creatorId != caregiverId }
                    _uiState.update { it.copy(
                        linkedSeniors = linkedOnly,
                        isLoadingList = false
                    ) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoadingList = false,
                        errorMessage = error.message ?: "Failed to load linked seniors"
                    ) }
                }
        }
    }

    fun onSeniorIdChanged(id: String) {
        _uiState.update { it.copy(
            seniorId = id.uppercase().trim(),
            seniorIdError = null,
            errorMessage = null
        ) }
    }

    fun linkSenior() {
        val seniorId = _uiState.value.seniorId
        
        // Validate
        if (seniorId.isBlank()) {
            _uiState.update { it.copy(seniorIdError = "Please enter a Senior ID") }
            return
        }
        
        if (!seniorId.matches(Regex("^SNR-[A-Z0-9]{8}$"))) {
            _uiState.update { it.copy(
                seniorIdError = "Invalid ID format. Expected: SNR-XXXXXXXX"
            ) }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val caregiverId = authRepository.getCurrentUid() ?: ""
            
            // 1. 验证老人账户是否存在
            seniorRepository.getSeniorById(seniorId)
                .onSuccess { senior ->
                    // 2. 检查是否已经绑定
                    if (senior.caregiverIds.contains(caregiverId)) {
                        _uiState.update { it.copy(
                            isLoading = false,
                            errorMessage = "This senior is already linked to your account"
                        ) }
                        return@launch
                    }
                    
                    // 3. 创建绑定关系（更新 caregiverIds）
                    val updatedSenior = senior.copy(
                        caregiverIds = (senior.caregiverIds + caregiverId).distinct()
                    )
                    seniorRepository.updateSenior(updatedSenior)
                        .onSuccess {
                            _uiState.update { it.copy(
                                isLoading = false,
                                isSuccess = true
                            ) }
                        }
                        .onFailure { error ->
                            _uiState.update { it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to link senior account"
                            ) }
                        }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = "Senior ID not found. Please check and try again."
                    ) }
                }
        }
    }

    fun resetLinkForm() {
        _uiState.update { it.copy(
            seniorId = "",
            seniorIdError = null,
            errorMessage = null,
            isLoading = false,
            isSuccess = false
        ) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
