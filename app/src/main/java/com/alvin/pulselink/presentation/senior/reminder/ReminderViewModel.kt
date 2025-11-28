package com.alvin.pulselink.presentation.senior.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.domain.usecase.GetRemindersUseCase
import com.alvin.pulselink.domain.usecase.MarkMedicationAsTakenUseCase
import com.alvin.pulselink.domain.usecase.MarkMedicationAsSkippedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val getRemindersUseCase: GetRemindersUseCase,
    private val markAsTakenUseCase: MarkMedicationAsTakenUseCase,
    private val markAsSkippedUseCase: MarkMedicationAsSkippedUseCase,
    private val createLogUseCase: com.alvin.pulselink.domain.usecase.CreateMedicationLogUseCase,
    private val localDataSource: LocalDataSource
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val GROUPING_WINDOW_MINUTES = 30 // 30分钟内的提醒视为同一批次
    }
    
    init {
        loadNextMedicationBatch()
    }
    
    fun loadNextMedicationBatch() {
        viewModelScope.launch {
            val local = localDataSource.getUser()
            val seniorId = local?.first ?: ""
            
            if (seniorId.isNotEmpty()) {
                getRemindersUseCase(seniorId).collect { allReminders ->
                    val pendingReminders = allReminders.filter { it.status == ReminderStatus.PENDING }
                    
                    if (pendingReminders.isEmpty()) {
                        _uiState.value = ReminderUiState(
                            medicationBatch = null,
                            isLoading = false
                        )
                        return@collect
                    }
                    
                    // 智能分组：30分钟内的提醒视为同一批次
                    val batch = groupNearbyReminders(pendingReminders)
                    
                    _uiState.value = ReminderUiState(
                        medicationBatch = batch,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * 智能分组算法：将时间接近的提醒分为一组
     */
    private fun groupNearbyReminders(reminders: List<ReminderItem>): MedicationBatch {
        if (reminders.isEmpty()) {
            throw IllegalArgumentException("Cannot group empty list")
        }
        
        // 按时间排序
        val sorted = reminders.sortedBy { parseTimeHHMM(it.time) }
        val firstReminder = sorted.first()
        val firstTimeMinutes = parseTimeHHMM(firstReminder.time)
        
        // 收集30分钟窗口内的所有提醒
        val batchItems = sorted
            .takeWhile { 
                val timeMinutes = parseTimeHHMM(it.time)
                (timeMinutes - firstTimeMinutes) <= GROUPING_WINDOW_MINUTES
            }
            .map { reminder ->
                MedicationItem(
                    reminderId = reminder.reminderId ?: "",
                    logId = reminder.logId,
                    name = reminder.medicationName,
                    dosage = reminder.dosage,
                    time = reminder.time,
                    isChecked = true // 默认全选
                )
            }
        
        // 确定时段标签
        val timeLabel = getTimePeriodLabel(firstReminder.time)
        
        return MedicationBatch(
            timeLabel = timeLabel,
            primaryTime = firstReminder.time,
            medications = batchItems
        )
    }
    
    /**
     * 将 "HH:mm" 转换为分钟数 (便于比较)
     */
    private fun parseTimeHHMM(time: String): Int {
        val parts = time.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        return hour * 60 + minute
    }
    
    /**
     * 根据时间确定时段标签
     */
    private fun getTimePeriodLabel(time: String): String {
        val hour = time.split(":")[0].toInt()
        return when (hour) {
            in 5..9 -> "Morning"
            in 10..12 -> "Late Morning"
            in 13..17 -> "Afternoon"
            in 18..21 -> "Evening"
            else -> "Night"
        }
    }
    
    /**
     * 切换某个药物的勾选状态
     */
    fun toggleMedicationCheck(medicationIndex: Int) {
        val currentBatch = _uiState.value.medicationBatch ?: return
        val updatedMedications = currentBatch.medications.toMutableList()
        
        if (medicationIndex in updatedMedications.indices) {
            val item = updatedMedications[medicationIndex]
            updatedMedications[medicationIndex] = item.copy(isChecked = !item.isChecked)
            
            _uiState.value = _uiState.value.copy(
                medicationBatch = currentBatch.copy(medications = updatedMedications)
            )
        }
    }
    
    /**
     * 确认批量服药
     */
    fun confirmBatchTaken() {
        viewModelScope.launch {
            val local = localDataSource.getUser()
            val seniorId = local?.first ?: ""
            val batch = _uiState.value.medicationBatch ?: return@launch
            
            batch.medications.forEach { medication ->
                if (medication.isChecked) {
                    // 勾选的药物标记为 TAKEN
                    processMedicationTaken(seniorId, medication)
                } else {
                    // 未勾选的药物标记为 SKIPPED
                    processMedicationSkipped(seniorId, medication)
                }
            }
        }
    }
    
    private suspend fun processMedicationTaken(seniorId: String, medication: MedicationItem) {
        if (medication.logId != null) {
            markAsTakenUseCase(medication.logId)
        } else {
            val log = com.alvin.pulselink.domain.model.MedicationLog(
                id = "${medication.reminderId}_${System.currentTimeMillis()}",
                reminderId = medication.reminderId,
                seniorId = seniorId,
                scheduledTime = parseTimeToMillis(medication.time),
                takenTime = System.currentTimeMillis(),
                status = com.alvin.pulselink.domain.model.MedicationLogStatus.TAKEN
            )
            createLogUseCase(log)
        }
    }
    
    private suspend fun processMedicationSkipped(seniorId: String, medication: MedicationItem) {
        if (medication.logId != null) {
            markAsSkippedUseCase(medication.logId)
        } else {
            val log = com.alvin.pulselink.domain.model.MedicationLog(
                id = "${medication.reminderId}_${System.currentTimeMillis()}",
                reminderId = medication.reminderId,
                seniorId = seniorId,
                scheduledTime = parseTimeToMillis(medication.time),
                takenTime = null,
                status = com.alvin.pulselink.domain.model.MedicationLogStatus.SKIPPED
            )
            createLogUseCase(log)
        }
    }
    

    
    private fun parseTimeToMillis(timeSlot: String): Long {
        val today = java.time.LocalDate.now()
        val parts = timeSlot.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        
        return today.atTime(hour, minute)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
