package com.alvin.pulselink.domain.repository

import com.alvin.pulselink.domain.model.HealthRecord
import com.alvin.pulselink.domain.model.HealthSummary

/**
 * 健康记录数据仓库接口
 * 
 * 方案C重构：独立管理健康记录
 * Collection: health_records/{recordId}
 */
interface HealthRecordRepository {
    
    // ========== 创建记录 ==========
    
    /**
     * 创建健康记录
     */
    suspend fun createRecord(record: HealthRecord): Result<HealthRecord>
    
    /**
     * 批量创建健康记录
     */
    suspend fun createRecords(records: List<HealthRecord>): Result<List<HealthRecord>>
    
    // ========== 查询记录 ==========
    
    /**
     * 获取老人的所有健康记录（分页）
     */
    suspend fun getRecordsBySenior(
        seniorProfileId: String,
        limit: Int = 50,
        startAfter: Long? = null
    ): Result<List<HealthRecord>>
    
    /**
     * 获取老人特定类型的健康记录
     */
    suspend fun getRecordsByType(
        seniorProfileId: String,
        type: String,
        limit: Int = 50,
        startAfter: Long? = null
    ): Result<List<HealthRecord>>
    
    /**
     * 获取老人最新的健康记录（每种类型取最新一条）
     */
    suspend fun getLatestRecords(seniorProfileId: String): Result<List<HealthRecord>>
    
    /**
     * 获取健康摘要（Dashboard 显示用）
     */
    suspend fun getHealthSummary(seniorProfileId: String): Result<HealthSummary>
    
    /**
     * 获取时间范围内的记录
     */
    suspend fun getRecordsInRange(
        seniorProfileId: String,
        type: String,
        startTime: Long,
        endTime: Long
    ): Result<List<HealthRecord>>
    
    /**
     * 根据 ID 获取记录
     */
    suspend fun getRecordById(recordId: String): Result<HealthRecord>
    
    // ========== 更新和删除 ==========
    
    /**
     * 更新健康记录
     */
    suspend fun updateRecord(record: HealthRecord): Result<Unit>
    
    /**
     * 删除健康记录
     */
    suspend fun deleteRecord(recordId: String): Result<Unit>
    
    /**
     * 删除老人的所有健康记录
     */
    suspend fun deleteAllRecords(seniorProfileId: String): Result<Unit>
    
    // ========== 统计方法 ==========
    
    /**
     * 获取血压平均值（指定时间范围）
     */
    suspend fun getAverageBloodPressure(
        seniorProfileId: String,
        startTime: Long,
        endTime: Long
    ): Result<Pair<Double, Double>?>  // (avgSystolic, avgDiastolic)
    
    /**
     * 获取心率平均值（指定时间范围）
     */
    suspend fun getAverageHeartRate(
        seniorProfileId: String,
        startTime: Long,
        endTime: Long
    ): Result<Double?>
}
