package com.alvin.pulselink.presentation.senior.linkguard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.CaregiverPermissions
import com.alvin.pulselink.domain.model.CaregiverRelationship
import com.alvin.pulselink.domain.model.LinkRequest
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.LinkRequestRepository
import com.alvin.pulselink.domain.repository.SeniorRepository
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
    private val linkRequestRepository: LinkRequestRepository,
    private val seniorRepository: SeniorRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeniorLinkGuardUiState())
    val uiState: StateFlow<SeniorLinkGuardUiState> = _uiState.asStateFlow()

    private val TAG = "SeniorLinkGuardVM"

    init {
        loadPendingRequests()
        loadBoundCaregivers() // ⭐ 加载已绑定的 caregivers
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
     * 加载已绑定的 Caregivers（包含审批信息）
     */
    fun loadBoundCaregivers() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                val seniorId = currentUser?.id ?: return@launch

                Log.d(TAG, "Loading bound caregivers for senior: $seniorId")

                seniorRepository.getSeniorById(seniorId)
                    .onSuccess { senior ->
                        // 过滤出已激活的 caregivers
                        val boundCaregivers = senior.caregiverRelationships
                            .filter { it.value.status == "active" }
                            .map { (caregiverId, relationship) ->
                                BoundCaregiver(
                                    caregiverId = caregiverId,
                                    relationship = relationship.relationship,
                                    nickname = relationship.nickname,
                                    linkedAt = relationship.linkedAt,
                                    approvedBy = relationship.approvedBy,
                                    permissions = relationship.permissions
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
                // 获取当前老人的 seniorId
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

                // 获取该老人的所有待处理链接请求
                linkRequestRepository.getRequestsBySenior(seniorId)
                    .onSuccess { requests ->
                        // 过滤出状态为 pending 的请求
                        val pendingRequests = requests.filter { it.status == "pending" }
                        Log.d(TAG, "Loaded ${pendingRequests.size} pending requests")
                        _uiState.update {
                            it.copy(
                                pendingRequests = pendingRequests,
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
    fun approveRequest(request: LinkRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

            try {
                val currentUser = authRepository.getCurrentUser()
                val seniorId = currentUser?.id ?: run {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "无法获取用户信息"
                        )
                    }
                    return@launch
                }
                
                // ⭐ 获取 Firebase Auth UID（用于 Firestore 规则验证）
                val currentAuthUid = authRepository.getCurrentUid() ?: run {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "无法获取认证信息"
                        )
                    }
                    return@launch
                }

                Log.d(TAG, "Approving request: ${request.id} by seniorId: $seniorId, authUid: $currentAuthUid")

                // 1. 获取老人信息
                val senior = seniorRepository.getSeniorById(seniorId).getOrNull()
                if (senior == null) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "无法获取老人信息"
                        )
                    }
                    return@launch
                }

                // 2. 批准请求（更新老人的 caregiverIds 和 caregiverRelationships）
                val updatedCaregiverIds = senior.caregiverIds.toMutableList().apply {
                    if (!contains(request.requesterId)) {
                        add(request.requesterId)
                    }
                }

                val updatedRelationships = senior.caregiverRelationships.toMutableMap()
                updatedRelationships[request.requesterId] = CaregiverRelationship(
                    relationship = request.relationship,
                    nickname = request.nickname,
                    linkedAt = System.currentTimeMillis(),
                    status = "active",
                    message = request.message,
                    approvedBy = currentAuthUid, // ⭐ 使用 Firebase Auth UID
                    permissions = CaregiverPermissions() // ⭐ 默认权限
                )

                val updatedSenior = senior.copy(
                    caregiverIds = updatedCaregiverIds,
                    caregiverRelationships = updatedRelationships
                )

                seniorRepository.updateSenior(updatedSenior)
                    .onSuccess {
                        Log.d(TAG, "Senior updated successfully")
                        
                        // 3. 更新 LinkRequest 状态为已批准
                        linkRequestRepository.updateRequestStatus(
                            requestId = request.id,
                            status = "approved",
                            approvedBy = currentAuthUid, // ⭐ 使用 Firebase Auth UID
                            approvedAt = System.currentTimeMillis() // ⭐ 记录时间
                        ).onSuccess {
                            Log.d(TAG, "Request approved successfully")
                            _uiState.update {
                                it.copy(
                                    isProcessing = false,
                                    successMessage = "已批准绑定申请"
                                )
                            }
                            // 重新加载请求列表
                            loadPendingRequests()
                        }.onFailure { error ->
                            Log.e(TAG, "Failed to update request status", error)
                            _uiState.update {
                                it.copy(
                                    isProcessing = false,
                                    errorMessage = "更新请求状态失败: ${error.message}"
                                )
                            }
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to update senior", error)
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                errorMessage = "更新失败: ${error.message}"
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
    fun rejectRequest(request: LinkRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

            try {
                // ⭐ 获取 Firebase Auth UID（用于 Firestore 规则验证）
                val currentAuthUid = authRepository.getCurrentUid() ?: run {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "无法获取认证信息"
                        )
                    }
                    return@launch
                }

                Log.d(TAG, "Rejecting request: ${request.id} by authUid: $currentAuthUid")

                // 更新 LinkRequest 状态为已拒绝
                linkRequestRepository.updateRequestStatus(
                    requestId = request.id,
                    status = "rejected",
                    rejectedBy = currentAuthUid, // ⭐ 使用 Firebase Auth UID
                    rejectedAt = System.currentTimeMillis() // ⭐ 记录时间
                ).onSuccess {
                    Log.d(TAG, "Request rejected successfully")
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            successMessage = "已拒绝绑定申请"
                        )
                    }
                    // 重新加载请求列表
                    loadPendingRequests()
                    loadBoundCaregivers() // ⭐ 重新加载已绑定列表
                }.onFailure { error ->
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
    val pendingRequests: List<LinkRequest> = emptyList(),
    val boundCaregivers: List<BoundCaregiver> = emptyList(), // ⭐ 已绑定的 caregivers
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
    val approvedBy: String, // ⭐ 审批人 UID
    val permissions: CaregiverPermissions
)
