package com.alvin.pulselink.presentation.caregiver.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.HealthRepository
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
 * 健康状态枚举
 */
enum class HealthStatus {
    GOOD,       // 健康状况良好
    ATTENTION,  // 需要关注
    URGENT      // 紧急情况
}

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
    private val healthRepository: HealthRepository,
    private val linkRequestRepository: LinkRequestRepository,
    private val localDataSource: LocalDataSource
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CaregiverProfileUiState())
    val uiState: StateFlow<CaregiverProfileUiState> = _uiState.asStateFlow()
    
    init {
        Log.d("ProfileVM", "========== CaregiverProfileViewModel INITIALIZED ==========")
        loadProfileData()
    }
    
    private fun loadProfileData() {
        Log.d("ProfileVM", "========== loadProfileData() STARTED ==========")
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
            
            // 4️⃣ 加载管理的老人数据并统计健康状态
            loadManagedSeniorsData()
        }
    }
    
    /**
     * 加载待审批的 Link 请求数量
     */
    private suspend fun loadPendingRequestsCount() {
        try {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                val result = linkRequestRepository.getPendingRequestsForCreator(currentUser.id)
                result.fold(
                    onSuccess = { linkRequests ->
                        // 计算 pending 状态的请求总数
                        val pendingCount = linkRequests.count { it.status == "pending" }
                        
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
     * 加载管理的老人数据并统计健康状态
     */
    private suspend fun loadManagedSeniorsData() {
        try {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                Log.d("ProfileVM", "Loading seniors for caregiver: ${currentUser.id}")
                val result = seniorRepository.getSeniorsByCaregiver(currentUser.id)
                result.fold(
                    onSuccess = { seniors ->
                        Log.d("ProfileVM", "Loaded ${seniors.size} seniors")
                        
                        // 计算管理的老人总数
                        val managedCount = seniors.size
                        
                        // 分析每个老人的健康状态
                        var goodCount = 0
                        var attentionCount = 0
                        var urgentCount = 0
                        var alertsCount = 0
                        
                        seniors.forEachIndexed { index, senior ->
                            Log.d("ProfileVM", "Analyzing senior ${index + 1}: ${senior.name} (ID: ${senior.id})")
                            
                            // 获取老人对应的 Firebase Auth UID
                            val uidResult = seniorRepository.getSeniorAuthUid(senior.id)
                            val seniorUid = uidResult.getOrNull()
                            
                            if (seniorUid != null) {
                                // 从 health_data 集合读取最新健康数据
                                val healthDataResult = healthRepository.getLatestHealthDataBySeniorUid(seniorUid)
                                val healthData = healthDataResult.getOrNull()
                                
                                if (healthData != null) {
                                    Log.d("ProfileVM", "  - Blood Pressure: ${healthData.systolic}/${healthData.diastolic}")
                                    Log.d("ProfileVM", "  - Heart Rate: ${healthData.heartRate}")
                                    
                                    val healthStatus = analyzeHealthStatusFromData(
                                        systolic = healthData.systolic,
                                        diastolic = healthData.diastolic,
                                        heartRate = healthData.heartRate
                                    )
                                    Log.d("ProfileVM", "  - Health Status: $healthStatus")
                                    
                                    when (healthStatus) {
                                        HealthStatus.GOOD -> goodCount++
                                        HealthStatus.ATTENTION -> {
                                            attentionCount++
                                            alertsCount++
                                        }
                                        HealthStatus.URGENT -> {
                                            urgentCount++
                                            alertsCount++
                                        }
                                    }
                                } else {
                                    Log.d("ProfileVM", "  - No health data found, marking as GOOD")
                                    goodCount++  // 没有数据时默认为 GOOD
                                }
                            } else {
                                Log.w("ProfileVM", "  - Could not find Auth UID for senior ${senior.id}")
                                goodCount++  // 找不到 UID 时默认为 GOOD
                            }
                        }
                        
                        Log.d("ProfileVM", "Summary: Good=$goodCount, Attention=$attentionCount, Urgent=$urgentCount, Alerts=$alertsCount")
                        
                        _uiState.update {
                            it.copy(
                                managedMembersCount = managedCount,
                                goodStatusCount = goodCount,
                                attentionCount = attentionCount,
                                urgentCount = urgentCount,
                                activeAlertsCount = alertsCount
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e("ProfileVM", "Failed to load seniors: ${error.message}", error)
                        // 失败时保持默认值
                        _uiState.update {
                            it.copy(
                                managedMembersCount = 0,
                                goodStatusCount = 0,
                                attentionCount = 0,
                                urgentCount = 0,
                                activeAlertsCount = 0
                            )
                        }
                    }
                )
            } else {
                Log.w("ProfileVM", "Current user is null")
            }
        } catch (e: Exception) {
            Log.e("ProfileVM", "Exception loading seniors: ${e.message}", e)
            // 异常时保持默认值
            _uiState.update {
                it.copy(
                    managedMembersCount = 0,
                    goodStatusCount = 0,
                    attentionCount = 0,
                    urgentCount = 0,
                    activeAlertsCount = 0
                )
            }
        }
    }
    
    /**
     * 分析健康状态（从健康数据直接分析）
     */
    private fun analyzeHealthStatusFromData(systolic: Int, diastolic: Int, heartRate: Int): HealthStatus {
        Log.d("ProfileVM", "    BP Analysis: systolic=$systolic, diastolic=$diastolic")
        Log.d("ProfileVM", "    HR Analysis: $heartRate bpm")
        
        // 检查血压
        val bpStatus = when {
            systolic > 160 || diastolic > 100 -> {
                Log.d("ProfileVM", "    BP Status: URGENT")
                HealthStatus.URGENT
            }
            systolic > 140 || diastolic > 90 -> {
                Log.d("ProfileVM", "    BP Status: ATTENTION")
                HealthStatus.ATTENTION
            }
            else -> {
                Log.d("ProfileVM", "    BP Status: GOOD")
                HealthStatus.GOOD
            }
        }
        
        // 检查心率
        val hrStatus = when {
            heartRate > 120 || heartRate < 50 -> {
                Log.d("ProfileVM", "    HR Status: URGENT")
                HealthStatus.URGENT
            }
            heartRate > 100 || heartRate < 60 -> {
                Log.d("ProfileVM", "    HR Status: ATTENTION")
                HealthStatus.ATTENTION
            }
            else -> {
                Log.d("ProfileVM", "    HR Status: GOOD")
                HealthStatus.GOOD
            }
        }
        
        // 取最严重的状态
        val finalStatus = when {
            bpStatus == HealthStatus.URGENT || hrStatus == HealthStatus.URGENT -> HealthStatus.URGENT
            bpStatus == HealthStatus.ATTENTION || hrStatus == HealthStatus.ATTENTION -> HealthStatus.ATTENTION
            else -> HealthStatus.GOOD
        }
        
        Log.d("ProfileVM", "    Final Status: $finalStatus")
        return finalStatus
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
     * 刷新管理的老人数据（供外部调用，例如从其他页面返回后）
     */
    fun refreshManagedSeniorsData() {
        viewModelScope.launch {
            loadManagedSeniorsData()
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
