package com.alvin.pulselink.domain.repository

import com.alvin.pulselink.domain.model.MedicationLog
import com.alvin.pulselink.domain.model.MedicationReminder
import kotlinx.coroutines.flow.Flow

/**
 * Medication Reminder Repository
 * 用药提醒数据仓库接口
 */
interface MedicationReminderRepository {
    
    // --- Reminder CRUD ---
    
    /**
     * 创建用药提醒
     */
    suspend fun createReminder(reminder: MedicationReminder): Result<String>
    
    /**
     * 更新用药提醒
     */
    suspend fun updateReminder(reminder: MedicationReminder): Result<Unit>
    
    /**
     * 删除用药提醒
     */
    suspend fun deleteReminder(reminderId: String): Result<Unit>
    
    /**
     * 获取单个提醒
     */
    suspend fun getReminder(reminderId: String): Result<MedicationReminder?>
    
    /**
     * 获取老人的所有提醒（实时监听）
     */
    fun getRemindersForSenior(seniorId: String): Flow<List<MedicationReminder>>
    
    /**
     * 获取老人的激活状态提醒
     */
    fun getActiveRemindersForSenior(seniorId: String): Flow<List<MedicationReminder>>
    
    /**
     * 更新库存
     */
    suspend fun updateStock(reminderId: String, newStock: Int): Result<Unit>
    
    /**
     * 暂停/恢复提醒
     */
    suspend fun toggleReminderStatus(reminderId: String, isPaused: Boolean): Result<Unit>
    
    // --- Medication Logs ---
    
    /**
     * 创建用药记录
     */
    suspend fun createLog(log: MedicationLog): Result<String>
    
    /**
     * 标记为已服用
     */
    suspend fun markAsTaken(logId: String, takenTime: Long): Result<Unit>
    
    /**
     * 标记为已跳过
     */
    suspend fun markAsSkipped(logId: String): Result<Unit>
    
    /**
     * 获取某天的用药记录
     */
    suspend fun getLogsForDate(seniorId: String, startOfDay: Long, endOfDay: Long): Result<List<MedicationLog>>
    
    /**
     * 获取提醒的历史记录
     */
    fun getLogsForReminder(reminderId: String, limit: Int = 30): Flow<List<MedicationLog>>
    
    /**
     * 获取今日待服用记录（实时）
     */
    fun getTodayPendingLogs(seniorId: String): Flow<List<MedicationLog>>
    
    /**
     * 获取今日所有用药记录（实时，不限状态）
     */
    fun getTodayAllLogs(seniorId: String): Flow<List<MedicationLog>>
}
