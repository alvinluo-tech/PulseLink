package com.alvin.pulselink.presentation.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthDataViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(HealthDataUiState())
    val uiState: StateFlow<HealthDataUiState> = _uiState.asStateFlow()
    
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
                            error = "Please fill in all fields"
                        )
                    }
                    return@launch
                }
                
                // TODO: Save to repository
                // Simulate network delay
                kotlinx.coroutines.delay(500)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isSaved = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to save health data"
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
