package com.alvin.pulselink.presentation.caregiver.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.SeniorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CaregiverProfileUiState(
    val userName: String = "Ms. Li",
    val managedMembersCount: Int = 4,
    val goodStatusCount: Int = 2,
    val attentionCount: Int = 1,
    val urgentCount: Int = 1,
    val activeAlertsCount: Int = 2,
    val pendingRequestsCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class CaregiverProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val seniorRepository: SeniorRepository,
    private val localDataSource: LocalDataSource
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CaregiverProfileUiState())
    val uiState: StateFlow<CaregiverProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadProfileData()
    }
    
    private fun loadProfileData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1️⃣ 立即从本地缓存读取 (快速显示)
            val cachedUser = localDataSource.getUser()
            val cachedName = cachedUser?.second ?: "Caregiver"

            _uiState.update {
                it.copy(
                    userName = cachedName,
                    isLoading = false
                )
            }
            
            // 2️⃣ 后台静默同步 Firestore (检查更新)
            syncFromFirestore()
            
            // 3️⃣ 加载待审批请求数量
            loadPendingRequestsCount()
        }
    }
    
    /**
     * 加载待审批的 Link 请求数量
     */
    private suspend fun loadPendingRequestsCount() {
        try {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                val result = seniorRepository.getPendingLinkRequests(currentUser.id)
                result.fold(
                    onSuccess = { seniors ->
                        // 计算所有 pending 关系的总数
                        val pendingCount = seniors.sumOf { senior ->
                            senior.caregiverRelationships.count { it.value.status == "pending" }
                        }
                        
                        _uiState.update { 
                            it.copy(pendingRequestsCount = pendingCount)
                        }
                    },
                    onFailure = {
                        // 失败时保持默认值 0
                        _uiState.update { 
                            it.copy(pendingRequestsCount = 0)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            // 异常时保持默认值 0
            _uiState.update { 
                it.copy(pendingRequestsCount = 0)
            }
        }
    }
    
    /**
     * 刷新待审批请求数量（供外部调用，例如从 FamilyRequestsScreen 返回后）
     */
    fun refreshPendingRequestsCount() {
        viewModelScope.launch {
            loadPendingRequestsCount()
        }
    }
    
    /**
     * 静默同步 Firestore 数据
     * 如果有变化，更新本地缓存和 UI
     */
    private suspend fun syncFromFirestore() {
        try {
            val firestoreUser = authRepository.getCurrentUser()
            
            if (firestoreUser != null) {
                val cachedUser = localDataSource.getUser()
                
                // 检查是否有变化
                if (cachedUser?.second != firestoreUser.username) {
                    // 有变化,更新本地缓存
                    localDataSource.saveUser(
                        id = firestoreUser.id,
                        username = firestoreUser.username,
                        role = firestoreUser.role.name.lowercase()
                    )
                    
                    // 更新 UI
                    _uiState.update { 
                        it.copy(userName = firestoreUser.username)
                    }
                }
            }
        } catch (e: Exception) {
            // 静默失败,不影响用户体验
            // 用户仍然看到缓存的数据
        }
    }
}
