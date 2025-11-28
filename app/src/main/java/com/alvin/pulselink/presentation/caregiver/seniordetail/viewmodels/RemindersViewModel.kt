package com.alvin.pulselink.presentation.caregiver.seniordetail.viewmodels

import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.MedicationReminder
import com.alvin.pulselink.domain.model.ReminderStatus
import com.alvin.pulselink.domain.usecase.*
import com.alvin.pulselink.presentation.common.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Medication Reminders ViewModel
 * 管理用药提醒
 */
@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val getRemindersForSeniorUseCase: GetRemindersForSeniorUseCase,
    private val createMedicationReminderUseCase: CreateMedicationReminderUseCase,
    private val updateMedicationReminderUseCase: UpdateMedicationReminderUseCase,
    private val deleteMedicationReminderUseCase: DeleteMedicationReminderUseCase,
    private val updateMedicationStockUseCase: UpdateMedicationStockUseCase,
    private val toggleReminderStatusUseCase: ToggleReminderStatusUseCase,
    private val getReminderByIdUseCase: GetReminderByIdUseCase,
    private val getCaregiverPermissionsUseCase: GetCaregiverPermissionsUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

    private var currentSeniorId: String? = null

    /**
     * 加载提醒列表和权限
     */
    fun loadReminders(seniorId: String) {
        currentSeniorId = seniorId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // 加载权限
                getCaregiverPermissionsUseCase(seniorId)
                    .onSuccess { permissions ->
                        _uiState.update { it.copy(permissions = permissions) }
                    }
                    .onFailure { e ->
                        showError("加载权限失败: ${e.message}")
                    }
                
                // 加载提醒列表
                getRemindersForSeniorUseCase(seniorId).collect { reminders ->
                    _uiState.update { 
                        it.copy(
                            reminders = reminders,
                            isLoading = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                showError("加载用药提醒失败: ${e.message}")
            }
        }
    }

    /**
     * 创建新提醒
     */
    fun createReminder(reminder: MedicationReminder) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            
            createMedicationReminderUseCase(reminder)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false) }
                    showSuccess("用药提醒创建成功")
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    showError("创建用药提醒失败: ${e.message}")
                }
        }
    }

    /**
     * 更新提醒
     */
    fun updateReminder(reminder: MedicationReminder) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            
            updateMedicationReminderUseCase(reminder)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false) }
                    showSuccess("用药提醒更新成功")
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    showError("更新用药提醒失败: ${e.message}")
                }
        }
    }

    /**
     * 删除提醒
     */
    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            deleteMedicationReminderUseCase(reminderId)
                .onSuccess {
                    showSuccess("用药提醒已删除")
                }
                .onFailure { e ->
                    showError("删除用药提醒失败: ${e.message}")
                }
        }
    }

    /**
     * 更新库存
     */
    fun updateStock(reminderId: String, newStock: Int) {
        viewModelScope.launch {
            updateMedicationStockUseCase(reminderId, newStock)
                .onSuccess {
                    showSuccess("库存更新成功")
                }
                .onFailure { e ->
                    showError("更新库存失败: ${e.message}")
                }
        }
    }

    /**
     * 切换提醒启用状态
     */
    fun toggleReminderStatus(reminderId: String) {
        viewModelScope.launch {
            toggleReminderStatusUseCase(reminderId)
                .onSuccess {
                    // Status toggled successfully
                }
                .onFailure { e ->
                    showError("切换状态失败: ${e.message}")
                }
        }
    }

    /**
     * 获取单个提醒详情
     */
    fun getReminderById(reminderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            getReminderByIdUseCase(reminderId)
                .onSuccess { reminder ->
                    _uiState.update { 
                        it.copy(
                            selectedReminder = reminder,
                            isLoading = false
                        ) 
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    showError("加载提醒详情失败: ${e.message}")
                }
        }
    }

    /**
     * 清除选中的提醒
     */
    fun clearSelectedReminder() {
        _uiState.update { it.copy(selectedReminder = null) }
    }

    /**
     * 检查库存警告
     */
    fun checkLowStockReminders(): List<MedicationReminder> {
        return _uiState.value.reminders.filter { reminder ->
            reminder.enableStockAlert && 
            reminder.currentStock <= reminder.lowStockThreshold &&
            reminder.status == ReminderStatus.ACTIVE
        }
    }
    
    /**
     * 显示权限错误消息 (public 方法)
     */
    fun showPermissionError(message: String) {
        showError(message)
    }
}

/**
 * Reminders UI State
 */
data class RemindersUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val reminders: List<MedicationReminder> = emptyList(),
    val selectedReminder: MedicationReminder? = null,
    val permissions: GetCaregiverPermissionsUseCase.Permissions = GetCaregiverPermissionsUseCase.Permissions()
)
