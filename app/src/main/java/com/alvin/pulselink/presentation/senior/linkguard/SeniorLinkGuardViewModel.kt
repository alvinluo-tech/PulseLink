package com.alvin.pulselink.presentation.senior.linkguard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.CaregiverRelation
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 老人端 LinkGuard ViewModel
 * 
 * 功能：
 * - 查看收到的链接请求
 * - 批准或拒绝 Caregiver 的绑定申请
 * - 管理已绑定的 Caregivers
 */
@HiltViewModel
class SeniorLinkGuardViewModel @Inject constructor(
    private val caregiverRelationRepository: CaregiverRelationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeniorLinkGuardUiState())
    val uiState: StateFlow<SeniorLinkGuardUiState> = _uiState.asStateFlow()
    
    // Channel for one-time UI events (success snackbar)
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()
    
    // StateFlow for error dialog (must be confirmed by user)
    private val _errorDialog = MutableStateFlow<ErrorDialogState?>(null)
    val errorDialog: StateFlow<ErrorDialogState?> = _errorDialog.asStateFlow()

    private val TAG = "SeniorLinkGuardVM"
    
    /**
     * UI事件（一次性消息：成功提示）
     * 老人端：Snackbar 大字体、长显示时间
     */
    sealed class UiEvent {
        data class ShowSuccessSnackbar(val message: String) : UiEvent()
    }
    
    /**
     * 错误对话框状态（必须用户确认）
     * 老人端：用 AlertDialog 确保老人看清楚错误信息
     */
    data class ErrorDialogState(
        val title: String,
        val message: String
    )
    
    fun dismissErrorDialog() {
        _errorDialog.value = null
    }

    init {
        loadPendingRequests()
        loadBoundCaregivers()
    }

    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 清除成功消息
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * 加载已绑定的 Caregivers
     */
    fun loadBoundCaregivers() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                val seniorId = currentUser?.id ?: return@launch

                Log.d(TAG, "Loading bound caregivers for senior: $seniorId")

                caregiverRelationRepository.getActiveRelationsBySenior(seniorId)
                    .onSuccess { relations ->
                        val boundCaregivers = relations.map { relation ->
                            BoundCaregiver(
                                caregiverId = relation.caregiverId,
                                caregiverName = relation.caregiverName,
                                relationship = relation.relationship,
                                nickname = relation.nickname,
                                linkedAt = relation.createdAt,
                                approvedBy = relation.approvedBy ?: "",
                                canViewHealthData = relation.canViewHealthData,
                                canEditHealthData = relation.canEditHealthData,
                                canViewReminders = relation.canViewReminders,
                                canEditReminders = relation.canEditReminders,
                                canApproveRequests = relation.canApproveRequests
                            )
                        }
                        
                        Log.d(TAG, "Loaded ${boundCaregivers.size} bound caregivers")
                        _uiState.update { it.copy(boundCaregivers = boundCaregivers) }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to load bound caregivers", error)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading bound caregivers", e)
            }
        }
    }

    /**
     * 加载待处理的链接请求
     */
    fun loadPendingRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val currentUser = authRepository.getCurrentUser()
                val seniorId = currentUser?.id ?: run {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "无法获取用户信息"
                        )
                    }
                    return@launch
                }

                Log.d(TAG, "Loading pending requests for senior: $seniorId")

                caregiverRelationRepository.getPendingRelationsBySenior(seniorId)
                    .onSuccess { pendingRelations ->
                        Log.d(TAG, "Loaded ${pendingRelations.size} pending requests")
                        _uiState.update {
                            it.copy(
                                pendingRequests = pendingRelations,
                                isLoading = false
                            )
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to load requests", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "加载请求失败: ${error.message}"
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "发生错误: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 批准链接请求
     */
    fun approveRequest(relation: CaregiverRelation) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

            try {
                val currentAuthUid = authRepository.getCurrentUid() ?: run {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "无法获取认证信息"
                        )
                    }
                    return@launch
                }

                Log.d(TAG, "Approving request: ${relation.id} by authUid: $currentAuthUid")

                caregiverRelationRepository.approveRelation(relation.id, currentAuthUid)
                    .onSuccess {
                        Log.d(TAG, "Request approved successfully")
                        _uiState.update { it.copy(isProcessing = false) }
                        _uiEvent.send(UiEvent.ShowSuccessSnackbar("Link request approved"))
                        loadPendingRequests()
                        loadBoundCaregivers()
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to approve request", error)
                        _uiState.update { it.copy(isProcessing = false) }
                        _errorDialog.value = ErrorDialogState(
                            title = "Approval Failed",
                            message = error.message ?: "Unable to approve the link request. Please try again."
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in approveRequest", e)
                _uiState.update { it.copy(isProcessing = false) }
                _errorDialog.value = ErrorDialogState(
                    title = "Error",
                    message = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * 更新护理者权限
     */
    fun updatePermissions(
        caregiverId: String,
        canViewHealthData: Boolean,
        canEditHealthData: Boolean,
        canViewReminders: Boolean,
        canEditReminders: Boolean,
        canApproveRequests: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

            try {
                val currentUser = authRepository.getCurrentUser()
                val seniorId = currentUser?.id ?: run {
                    _uiState.update { it.copy(isProcessing = false) }
                    _errorDialog.value = ErrorDialogState(
                        title = "Error",
                        message = "Unable to get user information"
                    )
                    return@launch
                }

                Log.d(TAG, "Updating permissions for caregiver: $caregiverId")

                val relationId = CaregiverRelation.generateId(caregiverId, seniorId)
                
                caregiverRelationRepository.updatePermissions(
                    relationId = relationId,
                    canViewHealthData = canViewHealthData,
                    canEditHealthData = canEditHealthData,
                    canViewReminders = canViewReminders,
                    canEditReminders = canEditReminders,
                    canApproveRequests = canApproveRequests
                ).onSuccess {
                    Log.d(TAG, "Permissions updated successfully")
                    _uiState.update { it.copy(isProcessing = false) }
                    _uiEvent.send(UiEvent.ShowSuccessSnackbar("Permissions updated"))
                    loadBoundCaregivers()
                }.onFailure { error ->
                    Log.e(TAG, "Failed to update permissions", error)
                    _uiState.update { it.copy(isProcessing = false) }
                    _errorDialog.value = ErrorDialogState(
                        title = "Update Failed",
                        message = error.message ?: "Unable to update permissions. Please try again."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating permissions", e)
                _uiState.update { it.copy(isProcessing = false) }
                _errorDialog.value = ErrorDialogState(
                    title = "Error",
                    message = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * 拒绝链接请求
     */
    fun rejectRequest(relation: CaregiverRelation) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            try {
                val currentAuthUid = authRepository.getCurrentUid() ?: run {
                    _uiState.update { it.copy(isProcessing = false) }
                    _errorDialog.value = ErrorDialogState(
                        title = "Error",
                        message = "Unable to get authentication information"
                    )
                    return@launch
                }

                Log.d(TAG, "Rejecting request: ${relation.id} by authUid: $currentAuthUid")

                caregiverRelationRepository.rejectRelation(relation.id, currentAuthUid)
                    .onSuccess {
                        Log.d(TAG, "Request rejected successfully")
                        _uiState.update { it.copy(isProcessing = false) }
                        _uiEvent.send(UiEvent.ShowSuccessSnackbar("Link request rejected"))
                        loadPendingRequests()
                        loadBoundCaregivers()
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to reject request", error)
                        _uiState.update { it.copy(isProcessing = false) }
                        _errorDialog.value = ErrorDialogState(
                            title = "Rejection Failed",
                            message = error.message ?: "Unable to reject the link request. Please try again."
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in rejectRequest", e)
                _uiState.update { it.copy(isProcessing = false) }
                _errorDialog.value = ErrorDialogState(
                    title = "Error",
                    message = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }
}

/**
 * 老人端 LinkGuard UI 状态
 */
data class SeniorLinkGuardUiState(
    val pendingRequests: List<CaregiverRelation> = emptyList(),
    val boundCaregivers: List<BoundCaregiver> = emptyList(),
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * 已绑定的 Caregiver 信息
 */
data class BoundCaregiver(
    val caregiverId: String,
    val caregiverName: String,          // 护理者的真实姓名
    val relationship: String,
    val nickname: String,
    val linkedAt: Long,
    val approvedBy: String,
    val canViewHealthData: Boolean,
    val canEditHealthData: Boolean,
    val canViewReminders: Boolean,
    val canEditReminders: Boolean,
    val canApproveRequests: Boolean
)
