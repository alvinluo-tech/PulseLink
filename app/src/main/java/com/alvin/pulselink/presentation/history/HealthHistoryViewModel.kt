package com.alvin.pulselink.presentation.history

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class HealthHistoryViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(HealthHistoryUiState())
    val uiState: StateFlow<HealthHistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadHealthRecords()
    }
    
    private fun loadHealthRecords() {
        // TODO: Load from repository
        // Mock data for now
        val mockRecords = listOf(
            HealthRecord(
                id = 1L,
                date = "Nov 18, 2025",
                time = "03:49 PM",
                systolic = 190,
                diastolic = 89,
                heartRate = 90,
                status = HealthStatus.HIGH
            ),
            HealthRecord(
                id = 2L,
                date = "Nov 17, 2025",
                time = "10:30 AM",
                systolic = 120,
                diastolic = 80,
                heartRate = 72,
                status = HealthStatus.NORMAL
            ),
            HealthRecord(
                id = 3L,
                date = "Nov 16, 2025",
                time = "02:15 PM",
                systolic = 125,
                diastolic = 82,
                heartRate = 75,
                status = HealthStatus.NORMAL
            )
        )
        
        _uiState.update { it.copy(records = mockRecords) }
    }
}
