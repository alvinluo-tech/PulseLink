package com.alvin.pulselink.presentation.senior.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HealthHistoryViewModel @Inject constructor(
    private val healthRepository: HealthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HealthHistoryUiState())
    val uiState: StateFlow<HealthHistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadHealthRecords()
    }
    
    private fun loadHealthRecords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = healthRepository.getHealthHistory()
            _uiState.update { state ->
                if (result.isSuccess) {
                    val list = result.getOrNull().orEmpty()
                    val dateFmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())

                    val records = list.mapIndexed { index, hd ->
                        val date = Date(hd.timestamp)
                        HealthRecord(
                            id = index.toLong() + 1L,
                            date = dateFmt.format(date),
                            time = timeFmt.format(date),
                            systolic = hd.systolic,
                            diastolic = hd.diastolic,
                            heartRate = hd.heartRate,
                            status = classifyStatus(hd.systolic, hd.diastolic)
                        )
                    }
                    state.copy(records = records, isLoading = false)
                } else {
                    state.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "加载健康历史失败"
                    )
                }
            }
        }
    }

    private fun classifyStatus(systolic: Int, diastolic: Int): HealthStatus {
        return when {
            systolic >= 140 || diastolic >= 90 -> HealthStatus.HIGH
            systolic < 90 || diastolic < 60 -> HealthStatus.LOW
            else -> HealthStatus.NORMAL
        }
    }
}
