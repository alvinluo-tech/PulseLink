package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.model.MedicationReminder
import com.alvin.pulselink.domain.repository.MedicationReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Get Reminders For Senior Use Case
 * 获取老人的用药提醒列表
 */
class GetRemindersForSeniorUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    operator fun invoke(seniorId: String): Flow<List<MedicationReminder>> {
        return repository.getRemindersForSenior(seniorId)
    }
}

/**
 * Get Active Reminders Use Case
 * 获取激活状态的提醒
 */
class GetActiveRemindersUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    operator fun invoke(seniorId: String): Flow<List<MedicationReminder>> {
        return repository.getActiveRemindersForSenior(seniorId)
    }
}

/**
 * Create Medication Reminder Use Case
 * 创建用药提醒
 */
class CreateMedicationReminderUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    suspend operator fun invoke(reminder: MedicationReminder): Result<String> {
        return repository.createReminder(reminder)
    }
}

/**
 * Update Medication Reminder Use Case
 * 更新用药提醒
 */
class UpdateMedicationReminderUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    suspend operator fun invoke(reminder: MedicationReminder): Result<Unit> {
        return repository.updateReminder(reminder)
    }
}

/**
 * Delete Medication Reminder Use Case
 * 删除用药提醒
 */
class DeleteMedicationReminderUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    suspend operator fun invoke(reminderId: String): Result<Unit> {
        return repository.deleteReminder(reminderId)
    }
}

/**
 * Update Medication Stock Use Case
 * 更新药品库存
 */
class UpdateMedicationStockUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    suspend operator fun invoke(reminderId: String, newStock: Int): Result<Unit> {
        return repository.updateStock(reminderId, newStock)
    }
}

/**
 * Toggle Reminder Status Use Case
 * 切换提醒状态（暂停/激活）
 */
class ToggleReminderStatusUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    suspend operator fun invoke(reminderId: String): Result<Unit> {
        return repository.toggleReminderStatus(reminderId, false)
    }
}

/**
 * Get Reminder By Id Use Case
 * 获取单个提醒详情
 */
class GetReminderByIdUseCase @Inject constructor(
    private val repository: MedicationReminderRepository
) {
    suspend operator fun invoke(reminderId: String): Result<MedicationReminder?> {
        return repository.getReminder(reminderId)
    }
}
