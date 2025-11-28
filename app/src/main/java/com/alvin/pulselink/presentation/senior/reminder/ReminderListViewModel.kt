package com.alvin.pulselink.presentation.senior.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.domain.usecase.GetRemindersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ReminderListViewModel @Inject constructor(
    private val getRemindersUseCase: GetRemindersUseCase,
    private val localDataSource: LocalDataSource
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReminderListUiState())
    val uiState: StateFlow<ReminderListUiState> = _uiState.asStateFlow()
    
    init {
        loadReminders()
    }
    
    fun loadReminders() {
        viewModelScope.launch {
            val local = localDataSource.getUser()
            val seniorId = local?.first ?: ""
            
            if (seniorId.isNotEmpty()) {
                getRemindersUseCase(seniorId).collect { reminders ->
                    _uiState.value = ReminderListUiState(
                        date = LocalDate.now(),
                        reminders = reminders,
                        takenCount = reminders.count { it.status == ReminderStatus.TAKEN },
                        pendingCount = reminders.count { it.status == ReminderStatus.PENDING },
                        missedCount = reminders.count { 
                            it.status == ReminderStatus.MISSED || it.status == ReminderStatus.SKIPPED 
                        }
                    )
                }
            }
        }
    }
}
