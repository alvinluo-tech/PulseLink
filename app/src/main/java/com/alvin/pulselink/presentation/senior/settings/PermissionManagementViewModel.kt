package com.alvin.pulselink.presentation.senior.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.CaregiverPermissions
import com.alvin.pulselink.domain.model.CaregiverRelationship
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.SeniorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 权限管理 ViewModel
 * 
 * 功能：
 * - 加载已绑定的 caregivers
 * - 更新 caregiver 权限
 * - 管理 linkRequestApprovers 列表
 */
@HiltViewModel
class PermissionManagementViewModel @Inject constructor(
    private val seniorRepository: SeniorRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionManagementUiState())
    val uiState: StateFlow<PermissionManagementUiState> = _uiState.asStateFlow()

    private val TAG = "PermissionManagementVM"

    init {
        loadBoundCaregivers()
    }

    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 加载已绑定的 Caregivers
     */
    private fun loadBoundCaregivers() {
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

                Log.d(TAG, "Loading bound caregivers for senior: $seniorId")

                seniorRepository.getSeniorById(seniorId)
                    .onSuccess { senior ->
                        // 过滤出已激活的 caregivers
                        val boundCaregivers = senior.caregiverRelationships
                            .filter { it.value.status == "active" }
                            .map { (caregiverId, relationship) ->
                                BoundCaregiverWithPermissions(
                                    caregiverId = caregiverId,
                                    relationship = relationship.relationship,
                                    nickname = relationship.nickname,
                                    permissions = relationship.permissions,
                                    canApprove = relationship.permissions.canApproveLinkRequests // ⭐ 从 permissions 字段读取
                                )
                            }

                        Log.d(TAG, "Loaded ${boundCaregivers.size} bound caregivers")
                        _uiState.update {
                            it.copy(
                                boundCaregivers = boundCaregivers,
                                seniorId = seniorId,
                                isLoading = false
                            )
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to load bound caregivers", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "加载失败: ${error.message}"
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading bound caregivers", e)
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
     * 更新护理者权限
     */
    fun updatePermissions(
        caregiverId: String,
        permissions: CaregiverPermissions,
        canApprove: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val seniorId = _uiState.value.seniorId
                if (seniorId.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "无法获取老人信息"
                        )
                    }
                    return@launch
                }

                Log.d(TAG, "Updating permissions for caregiver: $caregiverId")

                // 获取当前 senior 数据
                seniorRepository.getSeniorById(seniorId)
                    .onSuccess { senior ->
                        // 更新 caregiverRelationships 中的权限
                        val updatedRelationships = senior.caregiverRelationships.toMutableMap()
                        val currentRelationship = updatedRelationships[caregiverId]

                        if (currentRelationship != null) {
                            updatedRelationships[caregiverId] = currentRelationship.copy(
                                permissions = permissions.copy(
                                    canApproveLinkRequests = canApprove // ⭐ 同步更新这个字段
                                )
                            )
                        }

                        // 保存更新
                        val updatedSenior = senior.copy(
                            caregiverRelationships = updatedRelationships
                        )

                        seniorRepository.updateSenior(updatedSenior)
                            .onSuccess {
                                Log.d(TAG, "Permissions updated successfully")
                                // 重新加载数据
                                loadBoundCaregivers()
                            }
                            .onFailure { error ->
                                Log.e(TAG, "Failed to update permissions", error)
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        errorMessage = "更新失败: ${error.message}"
                                    )
                                }
                            }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to get senior data", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "获取数据失败: ${error.message}"
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating permissions", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "发生错误: ${e.message}"
                    )
                }
            }
        }
    }
}

/**
 * 权限管理 UI 状态
 */
data class PermissionManagementUiState(
    val boundCaregivers: List<BoundCaregiverWithPermissions> = emptyList(),
    val seniorId: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 带权限的已绑定 Caregiver
 */
data class BoundCaregiverWithPermissions(
    val caregiverId: String,
    val relationship: String,
    val nickname: String,
    val permissions: CaregiverPermissions,
    val canApprove: Boolean // ⭐ 是否在 linkRequestApprovers 列表中
)
