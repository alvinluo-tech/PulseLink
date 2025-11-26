package com.alvin.pulselink.presentation.caregiver.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.data.cache.ManagedSeniorsCache
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.domain.model.SeniorProfile
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.HealthRepository
import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import com.alvin.pulselink.domain.repository.SeniorProfileRepository
import com.alvin.pulselink.domain.usecase.profile.ManagedSeniorInfo
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
    val userName: String = "",
    val managedMembersCount: Int = 0,
    val goodStatusCount: Int = 0,
    val attentionCount: Int = 0,
    val urgentCount: Int = 0,
    val activeAlertsCount: Int = 0,
    val pendingRequestsCount: Int = 0,
    val isLoading: Boolean = true  // 初始状态为加载中
)

@HiltViewModel
class CaregiverProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val seniorProfileRepository: SeniorProfileRepository,
    private val healthRepository: HealthRepository,
    private val caregiverRelationRepository: CaregiverRelationRepository,
    private val localDataSource: LocalDataSource,
    private val managedSeniorsCache: ManagedSeniorsCache
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
            // 1️⃣ 立即从本地缓存读取用户名
            val cachedUser = localDataSource.getUser()
            val cachedName = cachedUser?.second ?: "Caregiver"

            // 2️⃣ 加载管理的老人数据并统计健康状态（优先，因为这是主要数据）
            loadManagedSeniorsData()
            
            // 3️⃣ 加载待审批请求数量
            loadPendingRequestsCount()
            
            // 4️⃣ 更新用户名
            _uiState.update {
                it.copy(userName = cachedName)
            }
            
            // 5️⃣ 后台静默同步 Firestore (检查更新)
            syncFromFirestore()
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
            if (currentUser == null) {
                Log.w("ProfileVM", "Current user is null")
                return
            }
            
            val currentUserId = currentUser.id
            Log.d("ProfileVM", "Loading seniors for caregiver: $currentUserId")
            
            // 检查缓存是否有效
            if (managedSeniorsCache.isCacheValid(currentUserId)) {
                Log.d("ProfileVM", "Using cached seniors data")
                val cachedSeniors = managedSeniorsCache.managedSeniors.value
                updateUIFromCache(cachedSeniors)
                return
            }
            
            // 缓存无效，从数据库加载（这种情况很少见，因为Dashboard通常会先加载）
            Log.d("ProfileVM", "Cache invalid, loading from database")
            
            // 获取当前用户关联的所有老人资料（通过 caregiver_relations）
            val relationsResult = caregiverRelationRepository.getActiveRelationsByCaregiver(currentUserId)
            val relations = relationsResult.getOrNull() ?: emptyList()
            
            Log.d("ProfileVM", "Found ${relations.size} active relations")
            
            // 获取所有关联老人的 profile
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
                
                // 尝试从缓存获取健康摘要
                val cachedSummary = managedSeniorsCache.getHealthSummary(profile.id)
                val healthStatus = if (cachedSummary != null) {
                    Log.d("ProfileVM", "  - Using cached health summary")
                    analyzeHealthStatusFromSummary(cachedSummary)
                } else {
                    // 从数据库读取最新健康数据
                    val healthDataResult = healthRepository.getLatestHealthDataBySeniorUid(profile.id)
                    val healthData = healthDataResult.getOrNull()
                    
                    if (healthData != null) {
                        Log.d("ProfileVM", "  - Blood Pressure: ${healthData.systolic}/${healthData.diastolic}")
                        Log.d("ProfileVM", "  - Heart Rate: ${healthData.heartRate}")
                        
                        analyzeHealthStatusFromData(
                            systolic = healthData.systolic,
                            diastolic = healthData.diastolic,
                            heartRate = healthData.heartRate
                        )
                    } else {
                        Log.d("ProfileVM", "  - No health data found, marking as GOOD")
                        HealthStatus.GOOD
                    }
                }
                
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
            }
            
            Log.d("ProfileVM", "Summary: Good=$goodCount, Attention=$attentionCount, Urgent=$urgentCount, Alerts=$alertsCount")
            
            _uiState.update {
                it.copy(
                    managedMembersCount = managedCount,
                    goodStatusCount = goodCount,
                    attentionCount = attentionCount,
                    urgentCount = urgentCount,
                    activeAlertsCount = alertsCount,
                    isLoading = false  // 数据加载完成
                )
            }
        } catch (e: Exception) {
            Log.e("ProfileVM", "Exception loading seniors: ${e.message}", e)
            // 异常时也要设置为非加载状态
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    /**
     * 从缓存更新UI
     */
    private fun updateUIFromCache(cachedSeniors: List<ManagedSeniorInfo>) {
        val managedCount = cachedSeniors.size
        
        var goodCount = 0
        var attentionCount = 0
        var urgentCount = 0
        var alertsCount = 0
        
        for (info in cachedSeniors) {
            val cachedSummary = managedSeniorsCache.getHealthSummary(info.profile.id)
            val healthStatus = if (cachedSummary != null && info.canViewHealthData) {
                analyzeHealthStatusFromSummary(cachedSummary)
            } else {
                HealthStatus.GOOD
            }
            
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
        }
        
        _uiState.update {
            it.copy(
                managedMembersCount = managedCount,
                goodStatusCount = goodCount,
                attentionCount = attentionCount,
                urgentCount = urgentCount,
                activeAlertsCount = alertsCount,
                isLoading = false  // 缓存数据加载完成
            )
        }
    }
    
    /**
     * 从HealthSummary分析健康状态
     */
    private fun analyzeHealthStatusFromSummary(summary: com.alvin.pulselink.domain.model.HealthSummary): HealthStatus {
        val systolic = summary.latestSystolic
        val diastolic = summary.latestDiastolic
        val heartRate = summary.latestHeartRateValue
        
        return analyzeHealthStatusFromData(systolic, diastolic, heartRate)
    }
    
    /**
     * 分析健康状态（从健康数据直接分析）
     */
    private fun analyzeHealthStatusFromData(systolic: Int?, diastolic: Int?, heartRate: Int?): HealthStatus {
        // 如果没有数据，返回GOOD状态
        if (systolic == null && diastolic == null && heartRate == null) {
            return HealthStatus.GOOD
        }
        
        Log.d("ProfileVM", "    BP Analysis: systolic=$systolic, diastolic=$diastolic")
        Log.d("ProfileVM", "    HR Analysis: $heartRate bpm")
        
        // 检查血压
        val bpStatus = when {
            systolic != null && diastolic != null && (systolic > 160 || diastolic > 100) -> {
                Log.d("ProfileVM", "    BP Status: URGENT")
                HealthStatus.URGENT
            }
            systolic != null && diastolic != null && (systolic > 140 || diastolic > 90) -> {
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
            heartRate != null && (heartRate > 120 || heartRate < 50) -> {
                Log.d("ProfileVM", "    HR Status: URGENT")
                HealthStatus.URGENT
            }
            heartRate != null && (heartRate > 100 || heartRate < 60) -> {
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
