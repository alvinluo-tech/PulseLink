package com.alvin.pulselink.presentation.reminderlist

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ReminderListViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(createMockState())
    val uiState: StateFlow<ReminderListUiState> = _uiState.asStateFlow()
    
    private fun createMockState(): ReminderListUiState {
        val reminders = listOf(
            ReminderItem(
                id = 1,
                time = "08:00 AM",
                medicationName = "Blood Pressure Medicine",
                dosage = "1 tablet",
                status = ReminderStatus.TAKEN
            ),
            ReminderItem(
                id = 2,
                time = "12:00 PM",
                medicationName = "Vitamin D",
                dosage = "2 capsules",
                status = ReminderStatus.MISSED
            ),
            ReminderItem(
                id = 3,
                time = "02:00 PM",
                medicationName = "Aspirin",
                dosage = "1 tablet",
                status = ReminderStatus.PENDING
            ),
            ReminderItem(
                id = 4,
                time = "06:00 PM",
                medicationName = "Blood Pressure Medicine",
                dosage = "1 tablet",
                status = ReminderStatus.PENDING
            ),
            ReminderItem(
                id = 5,
                time = "09:00 PM",
                medicationName = "Calcium",
                dosage = "1 tablet",
                status = ReminderStatus.PENDING
            ),
            ReminderItem(
                id = 6,
                time = "09:00 PM",
                medicationName = "Calcium",
                dosage = "1 tablet",
                status = ReminderStatus.PENDING
            )
        )
        
        return ReminderListUiState(
            date = LocalDate.now(),
            reminders = reminders,
            takenCount = reminders.count { it.status == ReminderStatus.TAKEN },
            pendingCount = reminders.count { it.status == ReminderStatus.PENDING },
            missedCount = reminders.count { it.status == ReminderStatus.MISSED }
        )
    }
}
