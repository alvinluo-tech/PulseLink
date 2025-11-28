package com.alvin.pulselink.presentation.caregiver.seniordetail.viewmodels

import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.presentation.caregiver.seniordetail.tabs.AlertFilterType
import com.alvin.pulselink.presentation.common.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * Alerts ViewModel
 * 管理健康历史记录
 */
@HiltViewModel
class AlertsViewModel @Inject constructor(
    // TODO: Inject HealthHistoryRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private var allAlerts: List<HealthAlert> = emptyList()

    /**
     * 加载健康历史记录
     */
    fun loadAlerts(seniorId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // TODO: Fetch alerts from repository
                val mockAlerts = listOf(
                    HealthAlert(
                        id = UUID.randomUUID().toString(),
                        type = AlertType.BLOOD_PRESSURE,
                        title = "Blood Pressure Reading",
                        value = "127/78 mmHg",
                        status = AlertStatus.NORMAL,
                        timestamp = Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000)
                    ),
                    HealthAlert(
                        id = UUID.randomUUID().toString(),
                        type = AlertType.HEART_RATE,
                        title = "Heart Rate Check",
                        value = "68 bpm",
                        status = AlertStatus.NORMAL,
                        timestamp = Date(System.currentTimeMillis() - 3 * 60 * 60 * 1000)
                    ),
                    HealthAlert(
                        id = UUID.randomUUID().toString(),
                        type = AlertType.MEDICATION,
                        title = "Medication Taken",
                        value = "Blood Pressure Pill",
                        status = AlertStatus.NORMAL,
                        timestamp = Date(System.currentTimeMillis() - 5 * 60 * 60 * 1000)
                    ),
                    HealthAlert(
                        id = UUID.randomUUID().toString(),
                        type = AlertType.BLOOD_PRESSURE,
                        title = "Blood Pressure Reading",
                        value = "135/88 mmHg",
                        status = AlertStatus.WARNING,
                        timestamp = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
                    ),
                    HealthAlert(
                        id = UUID.randomUUID().toString(),
                        type = AlertType.MEDICATION,
                        title = "Medication Missed",
                        value = "Vitamin D",
                        status = AlertStatus.WARNING,
                        timestamp = Date(System.currentTimeMillis() - 26 * 60 * 60 * 1000)
                    )
                )
                
                allAlerts = mockAlerts
                _uiState.update { it.copy(
                    filteredAlerts = mockAlerts,
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                showError("Failed to load health records: ${e.message}")
            }
        }
    }

    /**
     * 根据类型筛选记录
     */
    fun filterAlerts(filterType: AlertFilterType) {
        val filtered = when (filterType) {
            AlertFilterType.ALL -> allAlerts
            AlertFilterType.BLOOD_PRESSURE -> allAlerts.filter { it.type == AlertType.BLOOD_PRESSURE }
            AlertFilterType.HEART_RATE -> allAlerts.filter { it.type == AlertType.HEART_RATE }
            AlertFilterType.MEDICATION -> allAlerts.filter { it.type == AlertType.MEDICATION }
            AlertFilterType.ACTIVITY -> allAlerts.filter { it.type == AlertType.ACTIVITY }
        }
        
        _uiState.update { it.copy(filteredAlerts = filtered) }
    }
}

/**
 * Alerts UI State
 */
data class AlertsUiState(
    val isLoading: Boolean = false,
    val filteredAlerts: List<HealthAlert> = emptyList()
)

/**
 * Health Alert - Simplified without note
 */
data class HealthAlert(
    val id: String,
    val type: AlertType,
    val title: String,
    val value: String,
    val status: AlertStatus,
    val timestamp: Date
)

enum class AlertType {
    BLOOD_PRESSURE,
    HEART_RATE,
    MEDICATION,
    ACTIVITY
}

enum class AlertStatus {
    NORMAL,
    WARNING,
    CRITICAL
}
