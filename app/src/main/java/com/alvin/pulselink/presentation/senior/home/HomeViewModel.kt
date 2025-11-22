package com.alvin.pulselink.presentation.senior.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.usecase.GetHealthDataUseCase
import com.alvin.pulselink.domain.usecase.GetRemindersUseCase
import com.alvin.pulselink.presentation.senior.reminder.ReminderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHealthDataUseCase: GetHealthDataUseCase,
    private val getRemindersUseCase: GetRemindersUseCase,
    private val authRepository: AuthRepository,
    private val localDataSource: LocalDataSource
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadUserInfo()
        loadHealthData()
        loadNextReminder()
    }
    
    private fun loadUserInfo() {
        viewModelScope.launch {
            // 优先读取本地会话（虚拟ID登录时保存了 Senior.name）
            val local = localDataSource.getUser()
            val localName = if (local?.third == "senior") local.second else null

            val displayName = localName ?: "User"

            _uiState.update {
                it.copy(username = displayName)
            }
        }
    }
    
    private fun loadHealthData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = getHealthDataUseCase()
            
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        healthData = result.getOrNull(),
                        isLoading = false,
                        error = null
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }
    
    private fun loadNextReminder() {
        viewModelScope.launch {
            // 使用共享的 UseCase 获取提醒数据
            val reminders = getRemindersUseCase()
            
            // 找到下一个临近的 PENDING 提醒
            val nextReminderTime = getNextReminderFromList(reminders)
            _uiState.update {
                it.copy(nextReminderTime = nextReminderTime)
            }
        }
    }
    
    private fun getNextReminderFromList(reminders: List<com.alvin.pulselink.presentation.senior.reminder.ReminderItem>): String {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute
        
        // 找到第一个 PENDING 状态且时间未到的提醒
        val nextReminder = reminders.firstOrNull { reminder ->
            if (reminder.status != ReminderStatus.PENDING) return@firstOrNull false
            
            // 解析时间字符串 "02:00 PM" -> 14:00
            val reminderTimeInMinutes = parseTimeToMinutes(reminder.time)
            reminderTimeInMinutes > currentTimeInMinutes
        }
        
        return nextReminder?.time ?: "No reminders"
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
    
    fun refresh() {
        loadUserInfo()
        loadHealthData()
        loadNextReminder()
    }
}
