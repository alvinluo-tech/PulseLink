package com.alvin.pulselink.presentation.senior.reminder

import androidx.lifecycle.ViewModel
import com.alvin.pulselink.domain.usecase.GetRemindersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ReminderListViewModel @Inject constructor(
    private val getRemindersUseCase: GetRemindersUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(createMockState())
    val uiState: StateFlow<ReminderListUiState> = _uiState.asStateFlow()
    
    private fun createMockState(): ReminderListUiState {
        val reminders = getRemindersUseCase()
        
        return ReminderListUiState(
            date = LocalDate.now(),
            reminders = reminders,
            takenCount = reminders.count { it.status == ReminderStatus.TAKEN },
            pendingCount = reminders.count { it.status == ReminderStatus.PENDING },
            missedCount = reminders.count { it.status == ReminderStatus.MISSED }
        )
    }
}
