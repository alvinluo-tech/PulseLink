package com.alvin.pulselink.presentation.caregiver.dashboard

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class CareDashboardUiState(
    val lovedOnes: List<LovedOne> = emptyList(),
    val goodCount: Int = 0,
    val attentionCount: Int = 0,
    val urgentCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class CareDashboardViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(CareDashboardUiState())
    val uiState: StateFlow<CareDashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadMockData()
    }
    
    private fun loadMockData() {
        val mockLovedOnes = listOf(
            LovedOne(
                id = "1",
                name = "Mother (Mrs. Zhang)",
                relationship = "Mother",
                emoji = "👩",
                status = HealthStatus.URGENT,
                statusMessage = "Medication not confirmed twice!",
                statusColor = Color(0xFFEF4444),
                borderColor = Color(0xFFEF4444)
            ),
            LovedOne(
                id = "2",
                name = "Father (Mr. Zhang)",
                relationship = "Father",
                emoji = "👨",
                status = HealthStatus.GOOD,
                statusMessage = "All metrics normal",
                statusColor = Color(0xFF10B981),
                borderColor = Color(0xFF10B981)
            ),
            LovedOne(
                id = "3",
                name = "Grandmother (Mrs. Li)",
                relationship = "Maternal Grandmother",
                emoji = "👵",
                status = HealthStatus.ATTENTION,
                statusMessage = "Blood pressure elevated in morning",
                statusColor = Color(0xFFF59E0B),
                borderColor = Color(0xFFF59E0B)
            ),
            LovedOne(
                id = "4",
                name = "Grandfather (Mr. Li)",
                relationship = "Maternal Grandfather",
                emoji = "👴",
                status = HealthStatus.GOOD,
                statusMessage = "Medication taken on time",
                statusColor = Color(0xFF10B981),
                borderColor = Color(0xFF10B981)
            )
        )
        
        _uiState.value = CareDashboardUiState(
            lovedOnes = mockLovedOnes,
            goodCount = mockLovedOnes.count { it.status == HealthStatus.GOOD },
            attentionCount = mockLovedOnes.count { it.status == HealthStatus.ATTENTION },
            urgentCount = mockLovedOnes.count { it.status == HealthStatus.URGENT }
        )
    }
}
