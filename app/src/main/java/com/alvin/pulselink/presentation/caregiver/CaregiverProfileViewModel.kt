package com.alvin.pulselink.presentation.caregiver

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
class CaregiverProfileViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(CaregiverProfileUiState())
    val uiState: StateFlow<CaregiverProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadProfileData()
    }
    
    private fun loadProfileData() {
        // Mock data - in real app, fetch from repository
        _uiState.value = CaregiverProfileUiState(
            userName = "Ms. Li",
            managedMembersCount = 4,
            goodStatusCount = 2,
            attentionCount = 1,
            urgentCount = 1,
            activeAlertsCount = 2
        )
    }
}
