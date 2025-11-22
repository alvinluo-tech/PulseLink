package com.alvin.pulselink.presentation.senior.reminder

import androidx.lifecycle.ViewModel
import com.alvin.pulselink.domain.usecase.GetRemindersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val getRemindersUseCase: GetRemindersUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()
    
    private fun createInitialState(): ReminderUiState {
        // 获取所有提醒
        val allReminders = getRemindersUseCase()
        
        // 找到最近的一个 PENDING 且未到时间的提醒
        val nextReminder = findNextPendingReminder(allReminders)
        
        return ReminderUiState(
            reminder = nextReminder?.let {
                MedicationReminder(
                    medicationName = it.medicationName,
                    dosage = it.dosage,
                    time = it.time,
                    isTaken = false
                )
            }
        )
    }
    
    private fun findNextPendingReminder(reminders: List<ReminderItem>): ReminderItem? {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute
        
        // 找到第一个 PENDING 状态且时间未到的提醒
        return reminders.firstOrNull { reminder ->
            if (reminder.status != ReminderStatus.PENDING) return@firstOrNull false
            
            // 解析时间字符串 "02:00 PM" -> 14:00
            val reminderTimeInMinutes = parseTimeToMinutes(reminder.time)
            reminderTimeInMinutes > currentTimeInMinutes
        }
    }
    
    private fun parseTimeToMinutes(timeStr: String): Int {
        // 解析 "08:00 AM" 或 "02:00 PM" 格式
        val parts = timeStr.split(" ")
        if (parts.size != 2) return 0
        
        val timeParts = parts[0].split(":")
        if (timeParts.size != 2) return 0
        
        var hour = timeParts[0].toIntOrNull() ?: 0
        val minute = timeParts[1].toIntOrNull() ?: 0
        val isPM = parts[1].uppercase() == "PM"
        
        // 转换为 24 小时制
        if (isPM && hour != 12) {
            hour += 12
        } else if (!isPM && hour == 12) {
            hour = 0
        }
        
        return hour * 60 + minute
    }
    
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
