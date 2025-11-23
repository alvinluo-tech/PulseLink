package com.alvin.pulselink.presentation.caregiver.dashboard

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.Senior
import com.alvin.pulselink.domain.model.getDisplayNameFor
import com.alvin.pulselink.domain.model.getRelationshipStringFor
import com.alvin.pulselink.domain.repository.AuthRepository
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
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CareDashboardUiState())
    val uiState: StateFlow<CareDashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadSeniors()
    }
    
    fun loadSeniors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val currentUserId = authRepository.getCurrentUid() ?: ""
            
            seniorRepository.getSeniorsByCaregiver(currentUserId)
                .onSuccess { seniors ->
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
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load seniors"
                        )
                    }
                }
        }
    }
    
    private fun Senior.toLovedOne(currentUserId: String): LovedOne {
        // 获取称呼（nickname 或默认称呼）
        val addressTitle = getDisplayNameFor(currentUserId)
        
        // 生成显示名称：称呼（真实名字）
        val displayName = "$addressTitle ($name)"
        
        // 获取关系
        val relationshipText = getRelationshipStringFor(currentUserId)
        
        // 获取表情符号（基于头像类型）
        val emoji = getEmojiForAvatarType(avatarType)
        
        // 分析健康状态（这里可以根据实际健康数据判断）
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
        return when (avatarType) {
            "ELDERLY_MALE" -> "👴"
            "ELDERLY_FEMALE" -> "👵"
            "SENIOR_MALE" -> "👨"
            "SENIOR_FEMALE" -> "👩"
            "MIDDLE_AGED_MALE" -> "👨"
            "MIDDLE_AGED_FEMALE" -> "👩"
            "ADULT_MALE" -> "👨"
            "ADULT_FEMALE" -> "👩"
            else -> "👤"
        }
    }
    
    private fun analyzeHealthStatus(senior: Senior): HealthStatusInfo {
        // 根据实际健康数据分析状态
        val healthHistory = senior.healthHistory
        
        // 检查血压
        val bpStatus = healthHistory.bloodPressure?.let { bp ->
            when {
                bp.systolic > 140 || bp.diastolic > 90 -> HealthStatus.ATTENTION
                bp.systolic > 160 || bp.diastolic > 100 -> HealthStatus.URGENT
                else -> HealthStatus.GOOD
            }
        } ?: HealthStatus.GOOD
        
        // 检查心率
        val hrStatus = healthHistory.heartRate?.let { hr ->
            when {
                hr > 100 || hr < 60 -> HealthStatus.ATTENTION
                hr > 120 || hr < 50 -> HealthStatus.URGENT
                else -> HealthStatus.GOOD
            }
        } ?: HealthStatus.GOOD
        
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
                    healthHistory.bloodPressure?.systolic ?: 0 > 160 -> "Blood pressure critically high!"
                    healthHistory.heartRate ?: 0 > 120 -> "Heart rate critically high!"
                    healthHistory.heartRate ?: 0 < 50 -> "Heart rate critically low!"
                    else -> "Health metrics need urgent attention"
                }
            }
            HealthStatus.ATTENTION -> {
                when {
                    healthHistory.bloodPressure?.systolic ?: 0 > 140 -> "Blood pressure elevated"
                    healthHistory.heartRate ?: 0 > 100 -> "Heart rate elevated"
                    healthHistory.heartRate ?: 0 < 60 -> "Heart rate low"
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
        
        return HealthStatusInfo(finalStatus, message, color)
    }
    
    private data class HealthStatusInfo(
        val status: HealthStatus,
        val message: String,
        val color: Color
    )
}
