package com.alvin.pulselink.presentation.caregiver.seniordetail.viewmodels

import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.MedicationLog
import com.alvin.pulselink.domain.model.MedicationLogStatus
import com.alvin.pulselink.domain.usecase.*
import com.alvin.pulselink.presentation.common.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Medication Log ViewModel
 * 管理用药记录
 */
@HiltViewModel
class MedicationLogViewModel @Inject constructor(
    private val createMedicationLogUseCase: CreateMedicationLogUseCase,
    private val markMedicationAsTakenUseCase: MarkMedicationAsTakenUseCase,
    private val markMedicationAsSkippedUseCase: MarkMedicationAsSkippedUseCase,
    private val getTodayPendingLogsUseCase: GetTodayPendingLogsUseCase,
    private val getLogsForDateRangeUseCase: GetLogsForDateRangeUseCase,
    private val getMedicationLogByIdUseCase: GetMedicationLogByIdUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(MedicationLogUiState())
    val uiState: StateFlow<MedicationLogUiState> = _uiState.asStateFlow()

    private var currentSeniorId: String? = null

    /**
     * 加载今日待服用药物
     */
    fun loadTodayPendingMedications(seniorId: String) {
        currentSeniorId = seniorId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                getTodayPendingLogsUseCase(seniorId).collect { logs ->
                    _uiState.update { 
                        it.copy(
                            todayLogs = logs,
                            isLoading = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                showError("加载今日用药记录失败: ${e.message}")
            }
        }
    }

    /**
     * 加载指定日期范围的用药记录
     */
    fun loadLogsForDateRange(seniorId: String, startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                getLogsForDateRangeUseCase(seniorId, startDate, endDate).collect { logs ->
                    _uiState.update { 
                        it.copy(
                            historyLogs = logs,
                            isLoading = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                showError("加载历史记录失败: ${e.message}")
            }
        }
    }

    /**
     * 创建用药记录
     */
    fun createLog(log: MedicationLog) {
        viewModelScope.launch {
            createMedicationLogUseCase(log)
                .onSuccess {
                    showSuccess("用药记录创建成功")
                }
                .onFailure { e ->
                    showError("创建用药记录失败: ${e.message}")
                }
        }
    }

    /**
     * 标记为已服用
     */
    fun markAsTaken(logId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            
            markMedicationAsTakenUseCase(logId)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false) }
                    showSuccess("已标记为已服用")
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    showError("标记失败: ${e.message}")
                }
        }
    }

    /**
     * 标记为跳过
     */
    fun markAsSkipped(logId: String, reason: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            
            markMedicationAsSkippedUseCase(logId)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false) }
                    showSuccess("已标记为跳过")
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    showError("标记失败: ${e.message}")
                }
        }
    }

    /**
     * 获取单个用药记录详情
     */
    fun getLogById(logId: String) {
        viewModelScope.launch {
            getMedicationLogByIdUseCase(logId)
                .onSuccess { log ->
                    _uiState.update { it.copy(selectedLog = log) }
                }
                .onFailure { e ->
                    showError("加载记录详情失败: ${e.message}")
                }
        }
    }

    /**
     * 清除选中的记录
     */
    fun clearSelectedLog() {
        _uiState.update { it.copy(selectedLog = null) }
    }

    /**
     * 获取今日统计
     */
    fun getTodayStatistics(): MedicationStatistics {
        val logs = _uiState.value.todayLogs
        val total = logs.size
        val taken = logs.count { it.status == MedicationLogStatus.TAKEN }
        val skipped = logs.count { it.status == MedicationLogStatus.SKIPPED }
        val pending = logs.count { it.status == MedicationLogStatus.PENDING }
        
        val adherenceRate = if (total > 0) {
            (taken.toFloat() / (total - pending)) * 100
        } else {
            0f
        }
        
        return MedicationStatistics(
            total = total,
            taken = taken,
            skipped = skipped,
            pending = pending,
            adherenceRate = adherenceRate
        )
    }

    /**
     * 获取逾期未服用的药物
     */
    fun getOverdueMedications(): List<MedicationLog> {
        val now = System.currentTimeMillis()
        return _uiState.value.todayLogs.filter { log ->
            log.status == MedicationLogStatus.PENDING &&
            log.scheduledTime < now
        }
    }
}

/**
 * Medication Log UI State
 */
data class MedicationLogUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val todayLogs: List<MedicationLog> = emptyList(),
    val historyLogs: List<MedicationLog> = emptyList(),
    val selectedLog: MedicationLog? = null
)

/**
 * Medication Statistics
 */
data class MedicationStatistics(
    val total: Int,
    val taken: Int,
    val skipped: Int,
    val pending: Int,
    val adherenceRate: Float
)
