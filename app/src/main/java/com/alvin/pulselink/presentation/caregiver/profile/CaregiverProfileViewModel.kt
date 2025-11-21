package com.alvin.pulselink.presentation.caregiver.profile

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

data class CaregiverProfileUiState(
    val userName: String = "Ms. Li",
    val managedMembersCount: Int = 4,
    val goodStatusCount: Int = 2,
    val attentionCount: Int = 1,
    val urgentCount: Int = 1,
    val activeAlertsCount: Int = 2,
    val isLoading: Boolean = false
)

@HiltViewModel
class CaregiverProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CaregiverProfileUiState())
    val uiState: StateFlow<CaregiverProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadProfileData()
    }
    
    private fun loadProfileData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val user = authRepository.getCurrentUser()

            _uiState.update {
                it.copy(
                    userName = user?.username ?: "Caregiver",
                    // 保留占位统计数据，后续可接入真实仓库
                    managedMembersCount = it.managedMembersCount,
                    goodStatusCount = it.goodStatusCount,
                    attentionCount = it.attentionCount,
                    urgentCount = it.urgentCount,
                    activeAlertsCount = it.activeAlertsCount,
                    isLoading = false
                )
            }
        }
    }
}
