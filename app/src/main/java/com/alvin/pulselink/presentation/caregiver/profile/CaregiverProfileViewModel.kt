package com.alvin.pulselink.presentation.caregiver.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.domain.model.SeniorProfile
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.HealthRepository
import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import com.alvin.pulselink.domain.repository.SeniorProfileRepository
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
    private val seniorProfileRepository: SeniorProfileRepository,
    private val healthRepository: HealthRepository,
    private val caregiverRelationRepository: CaregiverRelationRepository,
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
                // 获取当前用户创建的所有老人资料
                val profilesResult = seniorProfileRepository.getProfilesByCreator(currentUser.id)
                val createdProfiles = profilesResult.getOrNull() ?: emptyList()
                
                var pendingCount = 0
                
                // 对于每个老人资料，统计 pending 状态的关系请求
                createdProfiles.forEach { profile ->
                    val relationsResult = caregiverRelationRepository.getPendingRelationsBySenior(profile.id)
                    relationsResult.getOrNull()?.size?.let { pendingCount += it }
                }
                
                _uiState.update { 
                    it.copy(pendingRequestsCount = pendingCount)
                }
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
                
                // 获取当前用户关联的所有老人资料（通过 caregiver_relations）
                val relationsResult = caregiverRelationRepository.getActiveRelationsByCaregiver(currentUser.id)
                val relations = relationsResult.getOrNull() ?: emptyList()
                
                Log.d("ProfileVM", "Found ${relations.size} active relations")
                
                // 获取所有关联老人的 profile - 使用 buildList 和 suspend 函数
                val seniorProfiles = mutableListOf<SeniorProfile>()
                for (relation in relations) {
                    val profileResult = seniorProfileRepository.getProfileById(relation.seniorId)
                    profileResult.getOrNull()?.let { seniorProfiles.add(it) }
                }
                
                Log.d("ProfileVM", "Loaded ${seniorProfiles.size} senior profiles")
                
                // 计算管理的老人总数
                val managedCount = seniorProfiles.size
                
                // 分析每个老人的健康状态
                var goodCount = 0
                var attentionCount = 0
                var urgentCount = 0
                var alertsCount = 0
                
                for ((index, profile) in seniorProfiles.withIndex()) {
                    Log.d("ProfileVM", "Analyzing senior ${index + 1}: ${profile.name} (ID: ${profile.id})")
                    
                    // 从 health_records 集合读取最新健康数据（使用 seniorId）
                    val seniorId = profile.id
                    val healthDataResult = healthRepository.getLatestHealthDataBySeniorUid(seniorId)
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
