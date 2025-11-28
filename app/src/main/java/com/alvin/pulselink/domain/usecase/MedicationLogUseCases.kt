package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.model.MedicationLog
import com.alvin.pulselink.domain.repository.MedicationReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Create Medication Log Use Case
 * 创建用药记录
 */
class CreateMedicationLogUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    suspend operator fun invoke(log: MedicationLog): Result<String> {
        return repository.createLog(log)
    }
}

/**
 * Mark Medication As Taken Use Case
 * 标记已服药
 */
class MarkMedicationAsTakenUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    suspend operator fun invoke(logId: String, takenTime: Long = System.currentTimeMillis()): Result<Unit> {
        return repository.markAsTaken(logId, takenTime)
    }
}

/**
 * Mark Medication As Skipped Use Case
 * 标记已跳过
 */
class MarkMedicationAsSkippedUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    suspend operator fun invoke(logId: String): Result<Unit> {
        return repository.markAsSkipped(logId)
    }
}

/**
 * Get Today Pending Logs Use Case
 * 获取今日待服用记录
 */
class GetTodayPendingLogsUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    operator fun invoke(seniorId: String): Flow<List<MedicationLog>> {
        return repository.getTodayPendingLogs(seniorId)
    }
}

/**
 * Get Logs For Date Use Case
 * 获取某天的用药记录
 */
class GetLogsForDateUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    suspend operator fun invoke(seniorId: String, startOfDay: Long, endOfDay: Long): Result<List<MedicationLog>> {
        return repository.getLogsForDate(seniorId, startOfDay, endOfDay)
    }
}

/**
 * Get Logs For Reminder Use Case
 * 获取某个提醒的历史记录
 */
class GetLogsForReminderUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    operator fun invoke(reminderId: String, limit: Int = 30): Flow<List<MedicationLog>> {
        return repository.getLogsForReminder(reminderId, limit)
    }
}

/**
 * Get Logs For Date Range Use Case
 * 获取日期范围的用药记录
 */
class GetLogsForDateRangeUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    operator fun invoke(seniorId: String, startDate: java.time.LocalDate, endDate: java.time.LocalDate): Flow<List<MedicationLog>> {
        val startTimestamp = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTimestamp = endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        // For now, return empty flow since we don't have a direct repository method
        return kotlinx.coroutines.flow.flow {
            val result = repository.getLogsForDate(seniorId, startTimestamp, endTimestamp)
            result.fold(
                onSuccess = { emit(it) },
                onFailure = { emit(emptyList()) }
            )
        }
    }
}

/**
 * Get Medication Log By Id Use Case
 * 获取单个用药记录详情
 * TODO: 需要在 Repository 中实现 getLog 方法
 */
class GetMedicationLogByIdUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    suspend operator fun invoke(logId: String): Result<MedicationLog?> {
        // 暂时返回 null，需要实现 repository.getLog 方法
        return Result.success(null)
    }
}
