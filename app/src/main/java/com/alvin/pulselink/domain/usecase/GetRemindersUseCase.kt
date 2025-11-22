package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.presentation.senior.reminder.ReminderItem
import com.alvin.pulselink.presentation.senior.reminder.ReminderStatus
import javax.inject.Inject

/**
 * UseCase for getting reminders
 * 统一的提醒数据源，供 HomeViewModel 和 ReminderListViewModel 共享
 */
class GetRemindersUseCase @Inject constructor() {
    
    operator fun invoke(): List<ReminderItem> {
        // Mock 数据 - 未来可以从 Repository 获取真实数据
        return listOf(
            ReminderItem(
                id = 1,
                time = "08:00 AM",
                medicationName = "Blood Pressure Medicine",
                dosage = "1 tablet",
                status = ReminderStatus.TAKEN
            ),
            ReminderItem(
                id = 2,
                time = "12:00 PM",
                medicationName = "Vitamin D",
                dosage = "2 capsules",
                status = ReminderStatus.MISSED
            ),
            ReminderItem(
                id = 3,
                time = "02:00 PM",
                medicationName = "Aspirin",
                dosage = "1 tablet",
                status = ReminderStatus.PENDING
            ),
            ReminderItem(
                id = 4,
                time = "06:00 PM",
                medicationName = "Blood Pressure Medicine",
                dosage = "1 tablet",
                status = ReminderStatus.PENDING
            ),
            ReminderItem(
                id = 5,
                time = "09:00 PM",
                medicationName = "Calcium",
                dosage = "1 tablet",
                status = ReminderStatus.PENDING
            )
        )
    }
}
