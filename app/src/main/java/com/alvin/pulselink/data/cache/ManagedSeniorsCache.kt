package com.alvin.pulselink.data.cache

import android.util.Log
import com.alvin.pulselink.domain.model.SeniorProfile
import com.alvin.pulselink.domain.model.CaregiverRelation
import com.alvin.pulselink.domain.model.HealthSummary
import com.alvin.pulselink.domain.usecase.profile.ManagedSeniorInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 管理的老人数据缓存
 * 
 * 用于在 Dashboard、Chat、Profile 页面之间共享数据，
 * 避免重复查询数据库
 */
@Singleton
class ManagedSeniorsCache @Inject constructor() {
    
    companion object {
        private const val TAG = "ManagedSeniorsCache"
        private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5分钟缓存
    }
    
    private val _managedSeniors = MutableStateFlow<List<ManagedSeniorInfo>>(emptyList())
    val managedSeniors: StateFlow<List<ManagedSeniorInfo>> = _managedSeniors.asStateFlow()
    
    private val _healthSummaries = MutableStateFlow<Map<String, HealthSummary>>(emptyMap())
    val healthSummaries: StateFlow<Map<String, HealthSummary>> = _healthSummaries.asStateFlow()
    
    private var lastUpdateTime: Long = 0L
    private var currentCaregiverId: String? = null
    
    /**
     * 检查缓存是否有效
     */
    fun isCacheValid(caregiverId: String): Boolean {
        val isValid = currentCaregiverId == caregiverId && 
                      _managedSeniors.value.isNotEmpty() &&
                      (System.currentTimeMillis() - lastUpdateTime) < CACHE_DURATION_MS
        
        Log.d(TAG, "Cache validity check: $isValid (age: ${System.currentTimeMillis() - lastUpdateTime}ms)")
        return isValid
    }
    
    /**
     * 更新缓存
     */
    fun updateCache(caregiverId: String, seniors: List<ManagedSeniorInfo>) {
        Log.d(TAG, "Updating cache with ${seniors.size} seniors for caregiver: $caregiverId")
        currentCaregiverId = caregiverId
        _managedSeniors.value = seniors
        lastUpdateTime = System.currentTimeMillis()
    }
    
    /**
     * 更新健康摘要缓存
     */
    fun updateHealthSummary(seniorId: String, summary: HealthSummary) {
        _healthSummaries.value = _healthSummaries.value + (seniorId to summary)
        Log.d(TAG, "Updated health summary for senior: $seniorId")
    }
    
    /**
     * 批量更新健康摘要
     */
    fun updateHealthSummaries(summaries: Map<String, HealthSummary>) {
        _healthSummaries.value = summaries
        Log.d(TAG, "Batch updated ${summaries.size} health summaries")
    }
    
    /**
     * 获取缓存的健康摘要
     */
    fun getHealthSummary(seniorId: String): HealthSummary? {
        return _healthSummaries.value[seniorId]
    }
    
    /**
     * 清除缓存
     */
    fun clear() {
        Log.d(TAG, "Clearing cache")
        _managedSeniors.value = emptyList()
        _healthSummaries.value = emptyMap()
        lastUpdateTime = 0L
        currentCaregiverId = null
    }
    
    /**
     * 手动刷新（用户下拉刷新）
     */
    fun invalidate() {
        Log.d(TAG, "Invalidating cache")
        lastUpdateTime = 0L
    }
}
