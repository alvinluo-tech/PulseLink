package com.alvin.pulselink.presentation.senior.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.usecase.SaveHealthDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthReportViewModel @Inject constructor(
    private val saveHealthDataUseCase: SaveHealthDataUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HealthReportUiState())
    val uiState: StateFlow<HealthReportUiState> = _uiState.asStateFlow()
    
    fun onSystolicPressureChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _uiState.update { it.copy(systolicPressure = value) }
        }
    }
    
    fun onDiastolicPressureChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _uiState.update { it.copy(diastolicPressure = value) }
        }
    }
    
    fun onHeartRateChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _uiState.update { it.copy(heartRate = value) }
        }
    }
    
    fun saveHealthData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Validate input
                val systolic = _uiState.value.systolicPressure.toIntOrNull()
                val diastolic = _uiState.value.diastolicPressure.toIntOrNull()
                val heartRate = _uiState.value.heartRate.toIntOrNull()
                
                if (systolic == null || diastolic == null || heartRate == null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "请填写所有字段"
                        )
                    }
                    return@launch
                }
                
                // Call use case to save
                val result = saveHealthDataUseCase(
                    systolic = systolic,
                    diastolic = diastolic,
                    heartRate = heartRate
                )
                
                result.fold(
                    onSuccess = {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                isSaved = true,
                                // Clear form after successful save
                                systolicPressure = "",
                                diastolicPressure = "",
                                heartRate = ""
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "保存健康数据失败"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "保存健康数据失败"
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun resetSavedState() {
        _uiState.update { it.copy(isSaved = false) }
    }
}
