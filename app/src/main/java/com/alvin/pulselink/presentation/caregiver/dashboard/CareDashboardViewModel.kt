package com.alvin.pulselink.presentation.caregiver.dashboard

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.Senior
import com.alvin.pulselink.domain.model.getDisplayNameFor
import com.alvin.pulselink.domain.model.getRelationshipStringFor
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.HealthRepository
import com.alvin.pulselink.domain.repository.SeniorRepository
import com.alvin.pulselink.util.AvatarHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CareDashboardUiState(
    val lovedOnes: List<LovedOne> = emptyList(),
    val goodCount: Int = 0,
    val attentionCount: Int = 0,
    val urgentCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CareDashboardViewModel @Inject constructor(
    private val seniorRepository: SeniorRepository,
    private val authRepository: AuthRepository,
    private val healthRepository: HealthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardVM"
    }

    init {
        Log.d(TAG, "========== CareDashboardViewModel INITIALIZED ==========")
    }
    
    private val _uiState = MutableStateFlow(CareDashboardUiState())
    val uiState: StateFlow<CareDashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadSeniors()
    }
    
    fun loadSeniors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val currentUserId = authRepository.getCurrentUid() ?: ""
            Log.d(TAG, "========== loadSeniors() STARTED for caregiver: $currentUserId ==========")
            
            seniorRepository.getSeniorsByCaregiver(currentUserId)
                .onSuccess { seniors ->
                    Log.d(TAG, "Found ${seniors.size} seniors to load")
                    
                    val lovedOnes = seniors.map { senior ->
                        senior.toLovedOne(currentUserId)
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
                    Log.d(TAG, "Dashboard stats - Good: ${lovedOnes.count { it.status == HealthStatus.GOOD }}, " +
                            "Attention: ${lovedOnes.count { it.status == HealthStatus.ATTENTION }}, " +
                            "Urgent: ${lovedOnes.count { it.status == HealthStatus.URGENT }}")
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load seniors: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load seniors"
                        )
                    }
                }
        }
    }
    
    private suspend fun Senior.toLovedOne(currentUserId: String): LovedOne {
        // 获取称呼（nickname 或默认称呼）
        val addressTitle = getDisplayNameFor(currentUserId)
        
        // 生成显示名称：称呼（真实名字）
        val displayName = "$addressTitle ($name)"
        
        // 获取关系
        val relationshipText = getRelationshipStringFor(currentUserId)
        
        // 获取表情符号（基于头像类型）
        val emoji = getEmojiForAvatarType(avatarType)
        
        // 分析健康状态（从 health_data 集合读取真实数据）
        val healthStatus = analyzeHealthStatus(this)
        
        return LovedOne(
            id = id,
            name = displayName,
            actualName = name, // 保留真实姓名
            relationship = relationshipText,
            emoji = emoji,
            status = healthStatus.status,
            statusMessage = healthStatus.message,
            statusColor = healthStatus.color,
            borderColor = healthStatus.color
        )
    }
    
    private fun getEmojiForAvatarType(avatarType: String): String {
        // 使用 AvatarHelper 统一获取 emoji
        return AvatarHelper.getAvatarEmoji(avatarType)
    }
    
    private suspend fun analyzeHealthStatus(senior: Senior): HealthStatusInfo {
        Log.d(TAG, "Analyzing health status for senior: ${senior.id}")
        
        // 1. 获取 senior 的 Firebase Auth UID
        val seniorUidResult = seniorRepository.getSeniorAuthUid(senior.id)
        val seniorUid = seniorUidResult.getOrNull()
        
        if (seniorUid == null) {
            Log.w(TAG, "Could not get UID for senior ${senior.id}")
            return HealthStatusInfo(
                status = HealthStatus.GOOD,
                message = "No health data available",
                color = Color(0xFF10B981)
            )
        }
        
        Log.d(TAG, "Senior ${senior.id} has UID: $seniorUid")
        
        // 2. 获取最新的健康数据
        val healthDataResult = healthRepository.getLatestHealthDataBySeniorUid(seniorUid)
        val healthData = healthDataResult.getOrNull()
        
        if (healthData == null) {
            Log.w(TAG, "No health data found for senior ${senior.id}")
            return HealthStatusInfo(
                status = HealthStatus.GOOD,
                message = "No health data available",
                color = Color(0xFF10B981)
            )
        }
        
        Log.d(TAG, "Senior ${senior.id} health data - BP: ${healthData.systolic}/${healthData.diastolic}, HR: ${healthData.heartRate}")
        
        // 3. 分析健康数据
        return analyzeHealthStatusFromData(
            systolic = healthData.systolic,
            diastolic = healthData.diastolic,
            heartRate = healthData.heartRate
        )
    }
    
    private fun analyzeHealthStatusFromData(
        systolic: Int,
        diastolic: Int,
        heartRate: Int
    ): HealthStatusInfo {
        // 检查血压
        val bpStatus = when {
            systolic > 160 || diastolic > 100 -> HealthStatus.URGENT
            systolic > 140 || diastolic > 90 -> HealthStatus.ATTENTION
            else -> HealthStatus.GOOD
        }
        
        // 检查心率
        val hrStatus = when {
            heartRate > 120 || heartRate < 50 -> HealthStatus.URGENT
            heartRate > 100 || heartRate < 60 -> HealthStatus.ATTENTION
            else -> HealthStatus.GOOD
        }
        
        // 取最严重的状态
        val finalStatus = when {
            bpStatus == HealthStatus.URGENT || hrStatus == HealthStatus.URGENT -> HealthStatus.URGENT
            bpStatus == HealthStatus.ATTENTION || hrStatus == HealthStatus.ATTENTION -> HealthStatus.ATTENTION
            else -> HealthStatus.GOOD
        }
        
        // 生成状态消息
        val message = when (finalStatus) {
            HealthStatus.URGENT -> {
                when {
                    systolic > 160 -> "Blood pressure critically high!"
                    heartRate > 120 -> "Heart rate critically high!"
                    heartRate < 50 -> "Heart rate critically low!"
                    else -> "Health metrics need urgent attention"
                }
            }
            HealthStatus.ATTENTION -> {
                when {
                    systolic > 140 -> "Blood pressure elevated"
                    heartRate > 100 -> "Heart rate elevated"
                    heartRate < 60 -> "Heart rate low"
                    else -> "Health metrics need attention"
                }
            }
            HealthStatus.GOOD -> "All metrics normal"
        }
        
        val color = when (finalStatus) {
            HealthStatus.GOOD -> Color(0xFF10B981)
            HealthStatus.ATTENTION -> Color(0xFFF59E0B)
            HealthStatus.URGENT -> Color(0xFFEF4444)
        }
        
        Log.d(TAG, "Health analysis - BP: $systolic/$diastolic, HR: $heartRate → Status: $finalStatus ($message)")
        
        return HealthStatusInfo(finalStatus, message, color)
    }
    
    private data class HealthStatusInfo(
        val status: HealthStatus,
        val message: String,
        val color: Color
    )
}
