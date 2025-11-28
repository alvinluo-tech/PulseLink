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
            val localId = local?.first ?: ""  // 获取 Senior ID

            val displayName = localName ?: "User"

            _uiState.update {
                it.copy(
                    username = displayName,
                    seniorId = localId
                )
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
            // 获取 seniorId
            val local = localDataSource.getUser()
            val seniorId = local?.first ?: ""
            
            if (seniorId.isNotEmpty()) {
                // 使用共享的 UseCase 获取提醒数据
                getRemindersUseCase(seniorId).collect { reminders ->
                    // 找到下一个临近的 PENDING 提醒
                    val nextReminderTime = getNextReminderFromList(reminders)
                    _uiState.update {
                        it.copy(nextReminderTime = nextReminderTime)
                    }
                }
            }
        }
    }
    
    private fun getNextReminderFromList(reminders: List<com.alvin.pulselink.presentation.senior.reminder.ReminderItem>): String {
        val pendingReminders = reminders.filter { it.status == ReminderStatus.PENDING }
        
        if (pendingReminders.isEmpty()) {
            return "No reminders"
        }
        
        // 如果只有一个提醒，直接显示时间
        if (pendingReminders.size == 1) {
            return pendingReminders.first().time
        }
        
        // 如果有多个提醒，检查是否在 30 分钟窗口内
        val firstTime = parseTimeToMinutes(pendingReminders.first().time)
        var medicationCount = 1
        
        for (i in 1 until pendingReminders.size) {
            val currentTime = parseTimeToMinutes(pendingReminders[i].time)
            if (currentTime - firstTime <= 30) {
                medicationCount++
            } else {
                break
            }
        }
        
        // 如果有多个药物在同一时段，显示批量信息
        return if (medicationCount > 1) {
            "${pendingReminders.first().time} (+$medicationCount meds)"
        } else {
            pendingReminders.first().time
        }
    }
    
    private fun parseTimeToMinutes(timeStr: String): Int {
        // 支持 "08:00 AM", "02:00 PM", "08:00" 格式
        val parts = timeStr.split(" ")
        val timeParts = parts[0].split(":")
        
        if (timeParts.size != 2) return 0
        
        var hour = timeParts[0].toIntOrNull() ?: 0
        val minute = timeParts[1].toIntOrNull() ?: 0
        
        // 如果有 AM/PM 标记，转换为 24 小时制
        if (parts.size == 2) {
            val isPM = parts[1].uppercase() == "PM"
            if (isPM && hour != 12) {
                hour += 12
            } else if (!isPM && hour == 12) {
                hour = 0
            }
        }
        
        return hour * 60 + minute
    }
    
    fun refresh() {
        loadUserInfo()
        loadHealthData()
        loadNextReminder()
    }
}
