package com.alvin.pulselink.presentation.senior.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.HealthRepository
import com.alvin.pulselink.domain.repository.SeniorProfileRepository
import com.alvin.pulselink.util.AvatarHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val localDataSource: LocalDataSource,
    private val seniorProfileRepository: SeniorProfileRepository,
    private val healthRepository: HealthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "ProfileViewModel"
    }
    
    init {
        loadProfileData()
    }
    
    private fun loadProfileData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // 1️⃣ 获取当前用户的 senior ID (从本地缓存)
                val cachedUser = localDataSource.getUser()
                val seniorId = cachedUser?.first
                val userName = cachedUser?.second
                
                Log.d(TAG, "Cached user: id=$seniorId, name=$userName, role=${cachedUser?.third}")
                
                if (seniorId.isNullOrBlank()) {
                    Log.e(TAG, "❌ No senior ID found in local cache")
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "User not logged in"
                        )
                    }
                    return@launch
                }
                
                Log.d(TAG, "✅ Loading profile for senior: $seniorId")
                
                // 2️⃣ 从 Firestore 获取 SeniorProfile 数据（使用新架构）
                val profileResult = seniorProfileRepository.getProfileById(seniorId)
                
                if (profileResult.isFailure) {
                    val error = profileResult.exceptionOrNull()
                    Log.e(TAG, "❌ Failed to load senior data: ${error?.message}", error)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Failed to load profile: ${error?.message}"
                        )
                    }
                    return@launch
                }
                
                val profile = profileResult.getOrNull()!!
                Log.d(TAG, "✅ Senior data loaded: name=${profile.name}, age=${profile.age}, gender=${profile.gender}, avatarType=${profile.avatarType}")
                
                // 3️⃣ 计算使用天数 (从 createdAt 到现在)
                val currentTimeMillis = System.currentTimeMillis()
                val daysDiff = TimeUnit.MILLISECONDS.toDays(currentTimeMillis - profile.createdAt)
                // 如果是 0 天（注册当天），则显示为 1 天
                val daysUsed = (daysDiff.toInt().coerceAtLeast(0) + 1)
                
                Log.d(TAG, "📅 Days used: $daysUsed (created: ${profile.createdAt}, now: $currentTimeMillis, diff: $daysDiff)")
                
                // 4️⃣ 根据 avatarType 获取 emoji
                val avatarEmoji = if (profile.avatarType.isNotBlank()) {
                    AvatarHelper.getAvatarEmoji(profile.avatarType)
                } else {
                    // 如果没有 avatarType，根据年龄和性别生成
                    Log.w(TAG, "⚠️ No avatarType found, generating from age and gender")
                    AvatarHelper.getAvatarEmojiByAgeGender(profile.age, profile.gender)
                }
                Log.d(TAG, "👤 Avatar emoji: $avatarEmoji (type: ${profile.avatarType})")
                
                // 5️⃣ 获取最新的健康数据 - 从 health_data 集合读取
                Log.d(TAG, "🔍 Fetching latest health data...")
                val healthDataResult = healthRepository.getHealthHistory()
                val healthHistoryList = healthDataResult.getOrNull() ?: emptyList()
                
                Log.d(TAG, "📊 Health history size: ${healthHistoryList.size}")
                
                val latestHealthData = healthHistoryList.firstOrNull()
                
                if (latestHealthData != null) {
                    Log.d(TAG, "✅ Latest health data: BP=${latestHealthData.systolic}/${latestHealthData.diastolic}, HR=${latestHealthData.heartRate}")
                    
                    // 分析血压状态
                    val bpStatus = analyzeBloodPressure(latestHealthData.systolic, latestHealthData.diastolic)
                    
                    _uiState.update { 
                        it.copy(
                            userName = profile.name,
                            age = profile.age,
                            gender = profile.gender,
                            avatarType = profile.avatarType,
                            avatarEmoji = avatarEmoji,
                            daysUsed = daysUsed,
                            seniorId = seniorId,
                            bloodPressure = "${latestHealthData.systolic}/${latestHealthData.diastolic}",
                            bloodPressureStatus = bpStatus,
                            heartRate = latestHealthData.heartRate,
                            isLoading = false
                        )
                    }
                } else {
                    Log.w(TAG, "⚠️ No health data found in history")
                    
                    // 没有健康数据时仍然显示基本信息
                    _uiState.update { 
                        it.copy(
                            userName = profile.name,
                            age = profile.age,
                            gender = profile.gender,
                            avatarType = profile.avatarType,
                            avatarEmoji = avatarEmoji,
                            daysUsed = daysUsed,
                            seniorId = seniorId,
                            bloodPressure = "--/--",
                            bloodPressureStatus = "No Data",
                            heartRate = 0,
                            isLoading = false
                        )
                    }
                }
                
                Log.d(TAG, "✅ Profile loaded successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading profile data: ${e.message}", e)
                e.printStackTrace()
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }
    
    /**
     * 分析血压状态
     */
    private fun analyzeBloodPressure(systolic: Int, diastolic: Int): String {
        return when {
            systolic >= 180 || diastolic >= 120 -> "High Risk"
            systolic >= 140 || diastolic >= 90 -> "High BP"
            systolic >= 130 || diastolic >= 80 -> "Elevated"
            systolic >= 120 && diastolic < 80 -> "Slightly High"
            systolic < 90 || diastolic < 60 -> "Low BP"
            else -> "Normal"
        }
    }
}
