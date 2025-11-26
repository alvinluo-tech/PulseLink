package com.alvin.pulselink.presentation.caregiver.dashboard

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.HealthSummary
import com.alvin.pulselink.domain.model.SeniorProfile
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
 * Caregiver Dashboard ViewModel (新架构 V2)
 * 
 * 使用新的独立集合架构:
 * - senior_profiles: 老人资料
 * - caregiver_relations: 关系管理（包含权限）
 * - health_records: 健康记录
 */
@HiltViewModel
class CareDashboardV2ViewModel @Inject constructor(
    private val getManagedSeniorsUseCase: GetManagedSeniorsUseCase,
    private val getHealthRecordsUseCase: GetHealthRecordsUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardV2VM"
    }

    private val _uiState = MutableStateFlow(CareDashboardV2UiState())
    val uiState: StateFlow<CareDashboardV2UiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "========== CareDashboardV2ViewModel INITIALIZED ==========")
        loadDashboard()
    }

    /**
     * 加载仪表盘数据
     */
    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val currentUserId = authRepository.getCurrentUid()
            if (currentUserId.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "未登录") }
                return@launch
            }
            
            Log.d(TAG, "========== loadDashboard() for caregiver: $currentUserId ==========")
            
            // 1. 获取管理的老人列表
            getManagedSeniorsUseCase(currentUserId)
                .onSuccess { managedSeniors ->
                    Log.d(TAG, "Found ${managedSeniors.size} managed seniors")
                    
                    // 2. 为每个老人获取健康摘要并转换为 LovedOneV2
                    val lovedOnes = managedSeniors.map { info ->
                        convertToLovedOne(info, currentUserId)
                    }
                    
                    _uiState.update {
                        it.copy(
                            lovedOnes = lovedOnes,
                            goodCount = lovedOnes.count { it.status == HealthStatusV2.GOOD },
                            attentionCount = lovedOnes.count { it.status == HealthStatusV2.ATTENTION },
                            urgentCount = lovedOnes.count { it.status == HealthStatusV2.URGENT },
                            isLoading = false
                        )
                    }
                    
                    Log.d(TAG, "Dashboard stats - Good: ${lovedOnes.count { it.status == HealthStatusV2.GOOD }}, " +
                            "Attention: ${lovedOnes.count { it.status == HealthStatusV2.ATTENTION }}, " +
                            "Urgent: ${lovedOnes.count { it.status == HealthStatusV2.URGENT }}")
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
     * 将 ManagedSeniorInfo 转换为 LovedOneV2
     */
    private suspend fun convertToLovedOne(
        info: ManagedSeniorInfo,
        currentUserId: String
    ): LovedOneV2 {
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
            getHealthStatusForSenior(profile.id, currentUserId)
        } else {
            HealthStatusInfoV2(
                status = HealthStatusV2.UNKNOWN,
                message = "无权限查看健康数据",
                color = Color.Gray
            )
        }
        
        return LovedOneV2(
            id = profile.id,
            name = profile.name,
            relationship = relationshipText,
            emoji = emoji,
            status = healthStatus.status,
            statusMessage = healthStatus.message,
            statusColor = healthStatus.color,
            borderColor = healthStatus.color,
            canViewHealth = info.canViewHealthData,
            canEditHealth = info.canEditHealthData
        )
    }

    /**
     * 获取老人的健康状态
     */
    private suspend fun getHealthStatusForSenior(
        seniorProfileId: String,
        requesterId: String
    ): HealthStatusInfoV2 {
        Log.d(TAG, "Getting health status for senior: $seniorProfileId")
        
        // 获取健康摘要
        val summaryResult = getHealthRecordsUseCase.getHealthSummary(seniorProfileId, requesterId)
        
        return summaryResult.fold(
            onSuccess = { summary ->
                analyzeHealthSummary(summary)
            },
            onFailure = { error ->
                Log.w(TAG, "Failed to get health summary: ${error.message}")
                HealthStatusInfoV2(
                    status = HealthStatusV2.GOOD,
                    message = "暂无健康数据",
                    color = Color(0xFF10B981)
                )
            }
        )
    }

    /**
     * 分析健康摘要，确定状态
     */
    private fun analyzeHealthSummary(summary: HealthSummary): HealthStatusInfoV2 {
        val systolic = summary.latestSystolic
        val diastolic = summary.latestDiastolic
        val heartRate = summary.latestHeartRateValue
        
        if (systolic == null && heartRate == null) {
            return HealthStatusInfoV2(
                status = HealthStatusV2.GOOD,
                message = "暂无健康数据",
                color = Color(0xFF10B981)
            )
        }
        
        // 分析血压
        val bpStatus = when {
            systolic != null && diastolic != null -> when {
                systolic > 160 || diastolic > 100 -> HealthStatusV2.URGENT
                systolic > 140 || diastolic > 90 -> HealthStatusV2.ATTENTION
                else -> HealthStatusV2.GOOD
            }
            else -> HealthStatusV2.GOOD
        }
        
        // 分析心率
        val hrStatus = when {
            heartRate != null -> when {
                heartRate > 120 || heartRate < 50 -> HealthStatusV2.URGENT
                heartRate > 100 || heartRate < 60 -> HealthStatusV2.ATTENTION
                else -> HealthStatusV2.GOOD
            }
            else -> HealthStatusV2.GOOD
        }
        
        // 取最严重的状态
        val finalStatus = when {
            bpStatus == HealthStatusV2.URGENT || hrStatus == HealthStatusV2.URGENT -> HealthStatusV2.URGENT
            bpStatus == HealthStatusV2.ATTENTION || hrStatus == HealthStatusV2.ATTENTION -> HealthStatusV2.ATTENTION
            else -> HealthStatusV2.GOOD
        }
        
        // 生成状态消息
        val message = when (finalStatus) {
            HealthStatusV2.URGENT -> when {
                systolic != null && systolic > 160 -> "血压过高，需紧急关注！"
                heartRate != null && heartRate > 120 -> "心率过快，需紧急关注！"
                heartRate != null && heartRate < 50 -> "心率过慢，需紧急关注！"
                else -> "健康指标需紧急关注"
            }
            HealthStatusV2.ATTENTION -> when {
                systolic != null && systolic > 140 -> "血压偏高"
                heartRate != null && heartRate > 100 -> "心率偏快"
                heartRate != null && heartRate < 60 -> "心率偏慢"
                else -> "健康指标需关注"
            }
            HealthStatusV2.GOOD -> "各项指标正常"
            HealthStatusV2.UNKNOWN -> "无数据"
        }
        
        val color = when (finalStatus) {
            HealthStatusV2.GOOD -> Color(0xFF10B981)
            HealthStatusV2.ATTENTION -> Color(0xFFF59E0B)
            HealthStatusV2.URGENT -> Color(0xFFEF4444)
            HealthStatusV2.UNKNOWN -> Color.Gray
        }
        
        Log.d(TAG, "Health analysis - BP: $systolic/$diastolic, HR: $heartRate → Status: $finalStatus")
        
        return HealthStatusInfoV2(finalStatus, message, color)
    }
}

/**
 * Dashboard UI 状态 (V2)
 */
data class CareDashboardV2UiState(
    val lovedOnes: List<LovedOneV2> = emptyList(),
    val goodCount: Int = 0,
    val attentionCount: Int = 0,
    val urgentCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 老人卡片数据 (V2)
 */
data class LovedOneV2(
    val id: String,
    val name: String,
    val relationship: String,
    val emoji: String,
    val status: HealthStatusV2,
    val statusMessage: String,
    val statusColor: Color,
    val borderColor: Color,
    val canViewHealth: Boolean,
    val canEditHealth: Boolean
)

/**
 * 健康状态枚举 (V2)
 */
enum class HealthStatusV2 {
    GOOD,
    ATTENTION,
    URGENT,
    UNKNOWN
}

/**
 * 健康状态信息 (V2)
 */
private data class HealthStatusInfoV2(
    val status: HealthStatusV2,
    val message: String,
    val color: Color
)
