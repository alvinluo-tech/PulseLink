package com.alvin.pulselink.presentation.reminder

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(
        ReminderUiState(
            reminder = MedicationReminder(
                medicationName = "Blood Pressure Medicine",
                dosage = "1 tablet",
                time = "04:15 PM",
                isTaken = false
            )
        )
    )
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()
    
    fun markAsTaken() {
        _uiState.value = _uiState.value.copy(
            reminder = _uiState.value.reminder?.copy(isTaken = true)
        )
    }
    
    fun markAsCannotTake() {
        // Handle cannot take action
        _uiState.value = _uiState.value.copy(
            reminder = null
        )
    }
}
