package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.model.FrequencyType
import com.alvin.pulselink.domain.model.MedicationLog
import com.alvin.pulselink.domain.model.MedicationLogStatus
import com.alvin.pulselink.domain.model.MedicationReminder
import com.alvin.pulselink.domain.model.ReminderStatus
import com.alvin.pulselink.domain.repository.MedicationReminderRepository
import com.alvin.pulselink.presentation.senior.reminder.ReminderItem
import com.alvin.pulselink.presentation.senior.reminder.ReminderStatus as UiReminderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.inject.Inject

/**
 * UseCase for getting reminders
 * 统一的提醒数据源，供 HomeViewModel 和 ReminderListViewModel 共享
 */
class GetRemindersUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    
    /**
     * 获取今日用药提醒
     * 从 MedicationReminder 计算今天应该服用的药物，然后查询对应的 Log 状态
     */
    operator fun invoke(seniorId: String): Flow<List<ReminderItem>> {
        val today = LocalDate.now()
        val todayEpochDay = today.toEpochDay()
        val dayOfWeek = today.dayOfWeek.value // 1=Monday, 7=Sunday
        
        return combine(
            repository.getActiveRemindersForSenior(seniorId),
            repository.getTodayAllLogs(seniorId)  // ← 改为获取所有状态的 logs
        ) { reminders, logs ->
            val logMap = logs.associateBy { "${it.reminderId}_${it.scheduledTime}" }
            
            val filteredReminders = reminders
                .filter { it.status == ReminderStatus.ACTIVE }
                .filter { shouldTakeToday(it, todayEpochDay, dayOfWeek) }
            
            filteredReminders
                .flatMap { reminder ->
                    // 为每个时间段生成一个 ReminderItem
                    reminder.timeSlots.map { timeSlot ->
                        val scheduledTime = parseTimeToMillis(today, timeSlot)
                        val logKey = "${reminder.id}_$scheduledTime"
                        val log = logMap[logKey]
                        
                        val status = when {
                            log?.status == MedicationLogStatus.TAKEN -> UiReminderStatus.TAKEN
                            log?.status == MedicationLogStatus.SKIPPED -> UiReminderStatus.SKIPPED
                            log?.status == MedicationLogStatus.MISSED -> UiReminderStatus.MISSED
                            scheduledTime < System.currentTimeMillis() -> UiReminderStatus.MISSED
                            else -> UiReminderStatus.PENDING
                        }
                        
                        ReminderItem(
                            id = "${reminder.id}_$timeSlot".hashCode(),
                            time = timeSlot,
                            medicationName = reminder.name,
                            dosage = "${reminder.dosage} ${reminder.unit}",
                            status = status,
                            logId = log?.id,
                            reminderId = reminder.id
                        )
                    }
                }
                .sortedBy { it.time }
        }
    }
    
    /**
     * 判断今天是否应该服用
     */
    private fun shouldTakeToday(reminder: MedicationReminder, todayEpochDay: Long, dayOfWeek: Int): Boolean {
        // 将 startDate (毫秒) 转换为天数
        val startDateEpochDay = reminder.startDate / (24 * 60 * 60 * 1000)
        val endDateEpochDay = reminder.endDate?.let { it / (24 * 60 * 60 * 1000) }
        
        // 检查是否在开始和结束日期范围内
        if (todayEpochDay < startDateEpochDay) return false
        if (endDateEpochDay != null && todayEpochDay > endDateEpochDay) return false
        
        return when (reminder.frequency) {
            FrequencyType.DAILY -> true
            FrequencyType.SPECIFIC_DAYS -> reminder.specificWeekDays.contains(dayOfWeek)
            FrequencyType.INTERVAL -> {
                val daysSinceStart = todayEpochDay - startDateEpochDay
                daysSinceStart % reminder.intervalDays == 0L
            }
        }
    }
    
    /**
     * 将时间字符串转换为今天的时间戳
     */
    private fun parseTimeToMillis(date: LocalDate, timeSlot: String): Long {
        val parts = timeSlot.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        
        return date.atTime(hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
