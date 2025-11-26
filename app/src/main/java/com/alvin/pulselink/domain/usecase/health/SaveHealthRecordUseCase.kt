package com.alvin.pulselink.domain.usecase.health

import android.util.Log
import com.alvin.pulselink.domain.model.HealthRecord
import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import com.alvin.pulselink.domain.repository.HealthRecordRepository
import javax.inject.Inject

/**
 * 保存健康记录用例 (新架构)
 */
class SaveHealthRecordUseCase @Inject constructor(
    private val healthRecordRepository: HealthRecordRepository,
    private val caregiverRelationRepository: CaregiverRelationRepository
) {
    companion object {
        private const val TAG = "SaveHealthRecordUseCase"
    }

    /**
     * 保存血压记录
     * 
     * @param seniorProfileId 老人资料 ID
     * @param recordedBy 记录者 ID
     * @param systolic 收缩压
     * @param diastolic 舒张压
     * @param heartRate 心率（可选）
     * @param notes 备注
     * @return 保存的记录
     */
    suspend fun saveBloodPressure(
        seniorProfileId: String,
        recordedBy: String,
        systolic: Int,
        diastolic: Int,
        heartRate: Int? = null,
        notes: String? = null
    ): Result<HealthRecord> {
        // 验证输入
        if (seniorProfileId.isBlank()) {
            return Result.failure(Exception("老人资料 ID 不能为空"))
        }
        
        if (systolic <= 0 || systolic > 300) {
            return Result.failure(Exception("收缩压应在 1-300 之间"))
        }
        
        if (diastolic <= 0 || diastolic > 200) {
            return Result.failure(Exception("舒张压应在 1-200 之间"))
        }
        
        if (systolic <= diastolic) {
            return Result.failure(Exception("收缩压应大于舒张压"))
        }
        
        if (heartRate != null && (heartRate <= 0 || heartRate > 250)) {
            return Result.failure(Exception("心率应在 1-250 之间"))
        }

        // 验证权限
        val hasPermission = checkEditPermission(seniorProfileId, recordedBy)
        if (!hasPermission) {
            return Result.failure(Exception("没有编辑健康数据的权限"))
        }

        return try {
            val record = HealthRecord(
                id = "", // 由 Repository 生成
                seniorProfileId = seniorProfileId,
                type = HealthRecord.TYPE_BLOOD_PRESSURE,
                systolic = systolic,
                diastolic = diastolic,
                heartRate = heartRate,
                bloodSugar = null,
                weight = null,
                recordedAt = System.currentTimeMillis(),
                recordedBy = recordedBy,
                notes = notes ?: ""
            )
            
            healthRecordRepository.createRecord(record)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save blood pressure", e)
            Result.failure(e)
        }
    }

    /**
     * 保存心率记录
     */
    suspend fun saveHeartRate(
        seniorProfileId: String,
        recordedBy: String,
        heartRate: Int,
        notes: String? = null
    ): Result<HealthRecord> {
        if (seniorProfileId.isBlank()) {
            return Result.failure(Exception("老人资料 ID 不能为空"))
        }
        
        if (heartRate <= 0 || heartRate > 250) {
            return Result.failure(Exception("心率应在 1-250 之间"))
        }

        val hasPermission = checkEditPermission(seniorProfileId, recordedBy)
        if (!hasPermission) {
            return Result.failure(Exception("没有编辑健康数据的权限"))
        }

        return try {
            val record = HealthRecord(
                id = "",
                seniorProfileId = seniorProfileId,
                type = HealthRecord.TYPE_HEART_RATE,
                systolic = null,
                diastolic = null,
                heartRate = heartRate,
                bloodSugar = null,
                weight = null,
                recordedAt = System.currentTimeMillis(),
                recordedBy = recordedBy,
                notes = notes ?: ""
            )
            
            healthRecordRepository.createRecord(record)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save heart rate", e)
            Result.failure(e)
        }
    }

    /**
     * 保存血糖记录
     */
    suspend fun saveBloodSugar(
        seniorProfileId: String,
        recordedBy: String,
        bloodSugar: Double,
        notes: String? = null
    ): Result<HealthRecord> {
        if (seniorProfileId.isBlank()) {
            return Result.failure(Exception("老人资料 ID 不能为空"))
        }
        
        if (bloodSugar <= 0 || bloodSugar > 50) {
            return Result.failure(Exception("血糖值应在 0-50 之间"))
        }

        val hasPermission = checkEditPermission(seniorProfileId, recordedBy)
        if (!hasPermission) {
            return Result.failure(Exception("没有编辑健康数据的权限"))
        }

        return try {
            val record = HealthRecord(
                id = "",
                seniorProfileId = seniorProfileId,
                type = HealthRecord.TYPE_BLOOD_SUGAR,
                systolic = null,
                diastolic = null,
                heartRate = null,
                bloodSugar = bloodSugar,
                weight = null,
                recordedAt = System.currentTimeMillis(),
                recordedBy = recordedBy,
                notes = notes ?: ""
            )
            
            healthRecordRepository.createRecord(record)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save blood sugar", e)
            Result.failure(e)
        }
    }

    /**
     * 保存体重记录
     */
    suspend fun saveWeight(
        seniorProfileId: String,
        recordedBy: String,
        weight: Double,
        notes: String? = null
    ): Result<HealthRecord> {
        if (seniorProfileId.isBlank()) {
            return Result.failure(Exception("老人资料 ID 不能为空"))
        }
        
        if (weight <= 0 || weight > 500) {
            return Result.failure(Exception("体重应在 0-500 kg 之间"))
        }

        val hasPermission = checkEditPermission(seniorProfileId, recordedBy)
        if (!hasPermission) {
            return Result.failure(Exception("没有编辑健康数据的权限"))
        }

        return try {
            val record = HealthRecord(
                id = "",
                seniorProfileId = seniorProfileId,
                type = HealthRecord.TYPE_WEIGHT,
                systolic = null,
                diastolic = null,
                heartRate = null,
                bloodSugar = null,
                weight = weight,
                recordedAt = System.currentTimeMillis(),
                recordedBy = recordedBy,
                notes = notes ?: ""
            )
            
            healthRecordRepository.createRecord(record)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save weight", e)
            Result.failure(e)
        }
    }

    /**
     * 检查编辑权限
     */
    private suspend fun checkEditPermission(
        seniorProfileId: String,
        requesterId: String
    ): Boolean {
        val relation = caregiverRelationRepository
            .getRelation(requesterId, seniorProfileId)
            .getOrNull()
        
        return relation?.canEditHealthData == true
    }
}
