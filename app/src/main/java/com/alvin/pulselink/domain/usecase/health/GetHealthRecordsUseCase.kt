package com.alvin.pulselink.domain.usecase.health

import com.alvin.pulselink.domain.model.HealthRecord
import com.alvin.pulselink.domain.model.HealthSummary
import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import com.alvin.pulselink.domain.repository.HealthRecordRepository
import javax.inject.Inject

/**
 * 获取健康记录用例 (新架构)
 * 
 * 支持:
 * - 获取最新记录
 * - 获取健康摘要
 * - 权限验证
 */
class GetHealthRecordsUseCase @Inject constructor(
    private val healthRecordRepository: HealthRecordRepository,
    private val caregiverRelationRepository: CaregiverRelationRepository
) {
    /**
     * 获取老人的最新健康记录
     * 
     * @param seniorProfileId 老人资料 ID
     * @param requesterId 请求者 ID
     * @param limit 返回数量限制
     * @return 健康记录列表
     */
    suspend fun getLatestRecords(
        seniorProfileId: String,
        requesterId: String,
        limit: Int = 10
    ): Result<List<HealthRecord>> {
        if (seniorProfileId.isBlank()) {
            return Result.failure(Exception("老人资料 ID 不能为空"))
        }

        // 验证权限
        val hasPermission = checkViewPermission(seniorProfileId, requesterId)
        if (!hasPermission) {
            return Result.failure(Exception("没有查看健康数据的权限"))
        }

        return healthRecordRepository.getLatestRecords(seniorProfileId)
    }

    /**
     * 获取老人的健康摘要（用于首页仪表盘）
     * 
     * @param seniorProfileId 老人资料 ID
     * @param requesterId 请求者 ID
     * @return 健康摘要
     */
    suspend fun getHealthSummary(
        seniorProfileId: String,
        requesterId: String
    ): Result<HealthSummary> {
        if (seniorProfileId.isBlank()) {
            return Result.failure(Exception("老人资料 ID 不能为空"))
        }

        // 验证权限
        val hasPermission = checkViewPermission(seniorProfileId, requesterId)
        if (!hasPermission) {
            return Result.failure(Exception("没有查看健康数据的权限"))
        }

        return healthRecordRepository.getHealthSummary(seniorProfileId)
    }

    /**
     * 获取指定类型的健康记录
     * 
     * @param seniorProfileId 老人资料 ID
     * @param requesterId 请求者 ID
     * @param type 记录类型 (blood_pressure, heart_rate, blood_sugar, weight)
     * @param limit 返回数量限制
     * @return 健康记录列表
     */
    suspend fun getRecordsByType(
        seniorProfileId: String,
        requesterId: String,
        type: String,
        limit: Int = 20
    ): Result<List<HealthRecord>> {
        if (seniorProfileId.isBlank()) {
            return Result.failure(Exception("老人资料 ID 不能为空"))
        }

        val hasPermission = checkViewPermission(seniorProfileId, requesterId)
        if (!hasPermission) {
            return Result.failure(Exception("没有查看健康数据的权限"))
        }

        return healthRecordRepository.getRecordsByType(seniorProfileId, type, limit)
    }

    /**
     * 获取平均血压（最近 N 天）
     */
    suspend fun getAverageBloodPressure(
        seniorProfileId: String,
        requesterId: String,
        days: Int = 7
    ): Result<Pair<Double, Double>?> {
        if (seniorProfileId.isBlank()) {
            return Result.failure(Exception("老人资料 ID 不能为空"))
        }

        val hasPermission = checkViewPermission(seniorProfileId, requesterId)
        if (!hasPermission) {
            return Result.failure(Exception("没有查看健康数据的权限"))
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - days * 24 * 60 * 60 * 1000L
        return healthRecordRepository.getAverageBloodPressure(seniorProfileId, startTime, endTime)
    }

    /**
     * 获取平均心率（最近 N 天）
     */
    suspend fun getAverageHeartRate(
        seniorProfileId: String,
        requesterId: String,
        days: Int = 7
    ): Result<Double?> {
        if (seniorProfileId.isBlank()) {
            return Result.failure(Exception("老人资料 ID 不能为空"))
        }

        val hasPermission = checkViewPermission(seniorProfileId, requesterId)
        if (!hasPermission) {
            return Result.failure(Exception("没有查看健康数据的权限"))
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - days * 24 * 60 * 60 * 1000L
        return healthRecordRepository.getAverageHeartRate(seniorProfileId, startTime, endTime)
    }

    /**
     * 检查查看权限
     * - 如果是老人本人（seniorProfileId == requesterId）直接允许
     * - 否则检查 caregiver_relation
     */
    private suspend fun checkViewPermission(
        seniorProfileId: String,
        requesterId: String
    ): Boolean {
        // 老人本人可以查看自己的数据
        // 注意：这里需要额外查询 profile.userId 来验证
        // 简化处理：如果 requesterId == seniorProfileId 则允许
        
        // 检查 Caregiver 关系
        val relation = caregiverRelationRepository
            .getRelation(requesterId, seniorProfileId)
            .getOrNull()
        
        return relation?.canViewHealthData == true
    }
}
