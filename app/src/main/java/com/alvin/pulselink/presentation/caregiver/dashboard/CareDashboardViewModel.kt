package com.alvin.pulselink.presentation.caregiver.dashboard

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.HealthSummary
import com.alvin.pulselink.domain.model.SeniorProfile
import com.alvin.pulselink.data.cache.ManagedSeniorsCache
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.usecase.health.GetHealthRecordsUseCase
import com.alvin.pulselink.domain.usecase.profile.GetManagedSeniorsUseCase
import com.alvin.pulselink.domain.usecase.profile.ManagedSeniorInfo
import com.alvin.pulselink.util.AvatarHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Caregiver Dashboard ViewModel
 * 
 * 使用新的独立集合架构:
 * - senior_profiles: 老人资料
 * - caregiver_relations: 关系管理（包含权限）
 * - health_records: 健康记录
 */
@HiltViewModel
class CareDashboardViewModel @Inject constructor(
    private val getManagedSeniorsUseCase: GetManagedSeniorsUseCase,
    private val getHealthRecordsUseCase: GetHealthRecordsUseCase,
    private val authRepository: AuthRepository,
    private val managedSeniorsCache: ManagedSeniorsCache
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardVM"
    }

    private val _uiState = MutableStateFlow(CareDashboardUiState())
    val uiState: StateFlow<CareDashboardUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "========== CareDashboardViewModel INITIALIZED ==========")
        loadDashboard()
    }

    /**
     * 加载仪表盘数据
     */
    fun loadDashboard(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUid()
            if (currentUserId.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "未登录") }
                return@launch
            }
            
            Log.d(TAG, "========== loadDashboard() for caregiver: $currentUserId (forceRefresh: $forceRefresh) ==========")
            
            // 检查缓存是否有效
            if (!forceRefresh && managedSeniorsCache.isCacheValid(currentUserId)) {
                Log.d(TAG, "Using cached data")
                val cachedSeniors = managedSeniorsCache.managedSeniors.value
                updateUIFromCache(cachedSeniors, currentUserId)
                return@launch
            }
            
            // 缓存无效或强制刷新，从数据库加载
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // 1. 获取管理的老人列表
            getManagedSeniorsUseCase(currentUserId)
                .onSuccess { managedSeniors ->
                    Log.d(TAG, "Found ${managedSeniors.size} managed seniors")
                    
                    // 更新缓存
                    managedSeniorsCache.updateCache(currentUserId, managedSeniors)
                    
                    // 2. 为每个老人获取健康摘要并转换为 LovedOne
                    val healthSummaries = mutableMapOf<String, HealthSummary>()
                    val lovedOnes = managedSeniors.map { info ->
                        convertToLovedOne(info, currentUserId, healthSummaries)
                    }
                    
                    // 批量更新健康摘要缓存
                    managedSeniorsCache.updateHealthSummaries(healthSummaries)
                    
                    _uiState.update {
                        it.copy(
                            lovedOnes = lovedOnes,
                            goodCount = lovedOnes.count { it.status == HealthStatus.GOOD },
                            attentionCount = lovedOnes.count { it.status == HealthStatus.ATTENTION },
                            urgentCount = lovedOnes.count { it.status == HealthStatus.URGENT },
                            isLoading = false
                        )
                    }
                    
                    Log.d(TAG, "Dashboard stats - Good: ${lovedOnes.count { it.status == HealthStatus.GOOD }}, " +
                            "Attention: ${lovedOnes.count { it.status == HealthStatus.ATTENTION }}, " +
                            "Urgent: ${lovedOnes.count { it.status == HealthStatus.URGENT }}")
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load managed seniors: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "加载失败"
                        )
                    }
                }
        }
    }
    
    /**
     * 从缓存更新UI
     */
    private suspend fun updateUIFromCache(cachedSeniors: List<ManagedSeniorInfo>, currentUserId: String) {
        val lovedOnes = cachedSeniors.map { info ->
            val cachedSummary = managedSeniorsCache.getHealthSummary(info.profile.id)
            convertToLovedOneWithCachedSummary(info, currentUserId, cachedSummary)
        }
        
        _uiState.update {
            it.copy(
                lovedOnes = lovedOnes,
                goodCount = lovedOnes.count { it.status == HealthStatus.GOOD },
                attentionCount = lovedOnes.count { it.status == HealthStatus.ATTENTION },
                urgentCount = lovedOnes.count { it.status == HealthStatus.URGENT },
                isLoading = false
            )
        }
    }

    /**
     * 将 ManagedSeniorInfo 转换为 LovedOne（从数据库获取健康数据）
     */
    private suspend fun convertToLovedOne(
        info: ManagedSeniorInfo,
        currentUserId: String,
        healthSummaries: MutableMap<String, HealthSummary>
    ): LovedOne {
        val profile = info.profile
        val relation = info.relation
        
        // 获取头像 emoji
        val emoji = AvatarHelper.getAvatarEmoji(profile.avatarType)
        
        // 获取关系类型
        val relationshipText = when (relation.relationship) {
            "PRIMARY_CAREGIVER" -> "主要监护人"
            "CAREGIVER" -> "监护人"
            "FAMILY" -> "家属"
            else -> "监护人"
        }
        
        // 获取健康状态（如果有权限）
        val healthStatus = if (info.canViewHealthData) {
            // 获取健康摘要
            val summaryResult = getHealthRecordsUseCase.getHealthSummary(profile.id, currentUserId)
            summaryResult.fold(
                onSuccess = { summary ->
                    // 保存到批量更新Map
                    healthSummaries[profile.id] = summary
                    analyzeHealthSummary(summary)
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed to get health summary for ${profile.id}: ${error.message}")
                    HealthStatusInfo(
                        status = HealthStatus.GOOD,
                        message = "No health data available",
                        color = Color(0xFF10B981)
                    )
                }
            )
        } else {
            HealthStatusInfo(
                status = HealthStatus.GOOD,
                message = "No permission to view health data",
                color = Color.Gray
            )
        }
        
        return LovedOne(
            id = profile.id,
            name = profile.name,
            nickname = relation.nickname,
            relationship = relationshipText,
            emoji = emoji,
            status = healthStatus.status,
            statusMessage = healthStatus.message,
            statusColor = healthStatus.color,
            borderColor = healthStatus.color
        )
    }
    
    /**
     * 将 ManagedSeniorInfo 转换为 LovedOne（使用缓存的健康数据）
     */
    private suspend fun convertToLovedOneWithCachedSummary(
        info: ManagedSeniorInfo,
        currentUserId: String,
        cachedSummary: HealthSummary?
    ): LovedOne {
        val profile = info.profile
        val relation = info.relation
        
        // 获取头像 emoji
        val emoji = AvatarHelper.getAvatarEmoji(profile.avatarType)
        
        // 获取关系类型
        val relationshipText = when (relation.relationship) {
            "PRIMARY_CAREGIVER" -> "主要监护人"
            "CAREGIVER" -> "监护人"
            "FAMILY" -> "家属"
            else -> "监护人"
        }
        
        // 获取健康状态
        val healthStatus = if (info.canViewHealthData) {
            if (cachedSummary != null) {
                analyzeHealthSummary(cachedSummary)
            } else {
                // 缓存没有，重新获取
                getHealthStatusForSenior(profile.id, currentUserId).also {
                    // 这里我们只有HealthStatusInfo，无法更新缓存的HealthSummary
                    // 实际使用中，缓存应该总是有效的
                }
            }
        } else {
            HealthStatusInfo(
                status = HealthStatus.GOOD,
                message = "No permission to view health data",
                color = Color.Gray
            )
        }
        
        return LovedOne(
            id = profile.id,
            name = profile.name,
            nickname = relation.nickname,
            relationship = relationshipText,
            emoji = emoji,
            status = healthStatus.status,
            statusMessage = healthStatus.message,
            statusColor = healthStatus.color,
            borderColor = healthStatus.color
        )
    }

    /**
     * 获取老人的健康状态
     */
    private suspend fun getHealthStatusForSenior(
        seniorProfileId: String,
        requesterId: String
    ): HealthStatusInfo {
        Log.d(TAG, "Getting health status for senior: $seniorProfileId")
        
        // 获取健康摘要
        val summaryResult = getHealthRecordsUseCase.getHealthSummary(seniorProfileId, requesterId)
        
        return summaryResult.fold(
            onSuccess = { summary ->
                Log.d(TAG, "Health summary received - BP record: ${summary.latestBloodPressure}, HR record: ${summary.latestHeartRate}")
                Log.d(TAG, "  Systolic: ${summary.latestSystolic}, Diastolic: ${summary.latestDiastolic}, HR: ${summary.latestHeartRateValue}")
                analyzeHealthSummary(summary)
            },
            onFailure = { error ->
                Log.w(TAG, "Failed to get health summary: ${error.message}", error)
                HealthStatusInfo(
                    status = HealthStatus.GOOD,
                    message = "No health data available",
                    color = Color(0xFF10B981)
                )
            }
        )
    }

    /**
     * 分析健康摘要，确定状态
     */
    private fun analyzeHealthSummary(summary: HealthSummary): HealthStatusInfo {
        val systolic = summary.latestSystolic
        val diastolic = summary.latestDiastolic
        val heartRate = summary.latestHeartRateValue
        
        Log.d(TAG, "Analyzing health summary - systolic: $systolic, diastolic: $diastolic, heartRate: $heartRate")
        
        if (systolic == null && heartRate == null) {
            Log.d(TAG, "No health data available")
            return HealthStatusInfo(
                status = HealthStatus.GOOD,
                message = "No health data available",
                color = Color(0xFF10B981)
            )
        }
        
        // 分析血压
        val bpStatus = when {
            systolic != null && diastolic != null -> when {
                systolic > 160 || diastolic > 100 -> {
                    Log.d(TAG, "BP Status: URGENT (systolic=$systolic, diastolic=$diastolic)")
                    HealthStatus.URGENT
                }
                systolic > 140 || diastolic > 90 -> {
                    Log.d(TAG, "BP Status: ATTENTION (systolic=$systolic, diastolic=$diastolic)")
                    HealthStatus.ATTENTION
                }
                else -> {
                    Log.d(TAG, "BP Status: GOOD (systolic=$systolic, diastolic=$diastolic)")
                    HealthStatus.GOOD
                }
            }
            else -> HealthStatus.GOOD
        }
        
        // 分析心率
        val hrStatus = when {
            heartRate != null -> when {
                heartRate > 120 || heartRate < 50 -> {
                    Log.d(TAG, "HR Status: URGENT (heartRate=$heartRate)")
                    HealthStatus.URGENT
                }
                heartRate > 100 || heartRate < 60 -> {
                    Log.d(TAG, "HR Status: ATTENTION (heartRate=$heartRate)")
                    HealthStatus.ATTENTION
                }
                else -> {
                    Log.d(TAG, "HR Status: GOOD (heartRate=$heartRate)")
                    HealthStatus.GOOD
                }
            }
            else -> HealthStatus.GOOD
        }
        
        // 取最严重的状态
        val finalStatus = when {
            bpStatus == HealthStatus.URGENT || hrStatus == HealthStatus.URGENT -> HealthStatus.URGENT
            bpStatus == HealthStatus.ATTENTION || hrStatus == HealthStatus.ATTENTION -> HealthStatus.ATTENTION
            else -> HealthStatus.GOOD
        }
        
        Log.d(TAG, "Final health status: $finalStatus (BP: $bpStatus, HR: $hrStatus)")
        
        // 生成状态消息
        val message = when (finalStatus) {
            HealthStatus.URGENT -> when {
                systolic != null && systolic > 160 -> "High blood pressure, needs urgent attention!"
                heartRate != null && heartRate > 120 -> "Heart rate too fast, needs urgent attention!"
                heartRate != null && heartRate < 50 -> "Heart rate too slow, needs urgent attention!"
                else -> "Health metrics need urgent attention"
            }
            HealthStatus.ATTENTION -> when {
                systolic != null && systolic > 140 -> "Blood pressure elevated"
                heartRate != null && heartRate > 100 -> "Heart rate elevated"
                heartRate != null && heartRate < 60 -> "Heart rate low"
                else -> "Health metrics need attention"
            }
            HealthStatus.GOOD -> "All metrics normal"
        }
        
        val color = when (finalStatus) {
            HealthStatus.GOOD -> Color(0xFF10B981)
            HealthStatus.ATTENTION -> Color(0xFFF59E0B)
            HealthStatus.URGENT -> Color(0xFFEF4444)
        }
        
        Log.d(TAG, "Health analysis - BP: $systolic/$diastolic, HR: $heartRate → Status: $finalStatus")
        
        return HealthStatusInfo(finalStatus, message, color)
    }
}

/**
 * Dashboard UI 状态
 */
data class CareDashboardUiState(
    val lovedOnes: List<LovedOne> = emptyList(),
    val goodCount: Int = 0,
    val attentionCount: Int = 0,
    val urgentCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 老人卡片数据
 */
data class LovedOne(
    val id: String,
    val name: String,
    val nickname: String,
    val relationship: String,
    val emoji: String,
    val status: HealthStatus,
    val statusMessage: String,
    val statusColor: Color,
    val borderColor: Color
)

/**
 * 健康状态枚举
 */
enum class HealthStatus {
    GOOD,
    ATTENTION,
    URGENT
}

/**
 * 健康状态信息
 */
private data class HealthStatusInfo(
    val status: HealthStatus,
    val message: String,
    val color: Color
)
