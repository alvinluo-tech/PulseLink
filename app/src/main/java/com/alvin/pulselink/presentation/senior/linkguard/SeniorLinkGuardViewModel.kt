package com.alvin.pulselink.presentation.senior.linkguard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.CaregiverRelation
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val TAG = "SeniorLinkGuardVM"

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
                                relationship = relation.relationship,
                                nickname = relation.nickname,
                                linkedAt = relation.createdAt,
                                approvedBy = relation.approvedBy ?: "",
                                canViewHealthData = relation.canViewHealthData,
                                canEditHealthData = relation.canEditHealthData,
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
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                successMessage = "已批准绑定申请"
                            )
                        }
                        loadPendingRequests()
                        loadBoundCaregivers()
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to approve request", error)
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                errorMessage = "批准失败: ${error.message}"
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "发生错误: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 拒绝链接请求
     */
    fun rejectRequest(relation: CaregiverRelation) {
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

                Log.d(TAG, "Rejecting request: ${relation.id} by authUid: $currentAuthUid")

                caregiverRelationRepository.rejectRelation(relation.id, currentAuthUid)
                    .onSuccess {
                        Log.d(TAG, "Request rejected successfully")
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                successMessage = "已拒绝绑定申请"
                            )
                        }
                        loadPendingRequests()
                        loadBoundCaregivers()
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to reject request", error)
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                errorMessage = "拒绝失败: ${error.message}"
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "发生错误: ${e.message}"
                    )
                }
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
    val relationship: String,
    val nickname: String,
    val linkedAt: Long,
    val approvedBy: String,
    val canViewHealthData: Boolean,
    val canEditHealthData: Boolean,
    val canApproveRequests: Boolean
)
