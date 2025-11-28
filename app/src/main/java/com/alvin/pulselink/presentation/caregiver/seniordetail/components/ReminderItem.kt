package com.alvin.pulselink.presentation.caregiver.seniordetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvin.pulselink.domain.model.MedicationReminder
import com.alvin.pulselink.domain.model.FrequencyType
import com.alvin.pulselink.domain.model.ReminderStatus
import java.time.format.DateTimeFormatter

/**
 * Reminder Item Component
 * 显示单个用药提醒
 */
@Composable
fun ReminderItem(
    reminder: MedicationReminder,
    canEdit: Boolean,  // 当前用户是否有编辑权限
    isOwnReminder: Boolean,  // 是否是当前用户创建的
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val isActive = reminder.status == ReminderStatus.ACTIVE
    val canModify = canEdit && isOwnReminder  // 只能修改自己创建且有编辑权限的
    val cardColor = try {
        Color(android.graphics.Color.parseColor(reminder.colorHex))
    } catch (e: Exception) {
        Color(0xFF8B5CF6)
    }
    
    val isLowStock = reminder.enableStockAlert && 
                     reminder.currentStock <= reminder.lowStockThreshold
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color.White else Color(0xFFF9FAFB)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isActive) cardColor.copy(alpha = 0.1f) else Color(0xFFE5E7EB),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = if (isActive) cardColor else Color(0xFF9CA3AF),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = reminder.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) Color(0xFF111827) else Color(0xFF6B7280)
                    )
                    
                    if (!reminder.nickname.isNullOrBlank()) {
                        Surface(
                            color = Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = reminder.nickname,
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                // Dosage
                Text(
                    text = "${reminder.dosage} ${reminder.unit}",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
                
                // Time slots and frequency
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = reminder.timeSlots.joinToString(", "),
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                    
                    Surface(
                        color = if (isActive) Color(0xFFEFF6FF) else Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = getFrequencyText(reminder),
                            fontSize = 11.sp,
                            color = if (isActive) Color(0xFF1E40AF) else Color(0xFF6B7280),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                
                // Stock warning
                if (isLowStock) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "库存不足: ${reminder.currentStock} ${reminder.unit}",
                            fontSize = 12.sp,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Toggle Button - 只有自己创建且有编辑权限才能切换
                IconButton(
                    onClick = {
                        if (canModify) {
                            onToggle()
                        } else {
                            onPermissionDenied()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isActive && canModify) Color(0xFFD1FAE5) 
                            else if (!canModify) Color(0xFFF3F4F6)
                            else Color(0xFFF3F4F6),
                            CircleShape
                        ),
                    enabled = canModify
                ) {
                    Icon(
                        imageVector = if (isActive) {
                            Icons.Default.Notifications
                        } else {
                            Icons.Default.NotificationsOff
                        },
                        contentDescription = if (isActive) "Disable" else "Enable",
                        tint = if (canModify && isActive) Color(0xFF10B981) else Color(0xFF9CA3AF),
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // Delete Button - 只有自己创建且有编辑权限才能删除
                IconButton(
                    onClick = {
                        if (canModify) {
                            onDelete()
                        } else {
                            onPermissionDenied()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (canModify) Color(0xFFFEE2E2) else Color(0xFFF3F4F6),
                            CircleShape
                        ),
                    enabled = canModify
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = if (canModify) Color(0xFFEF4444) else Color(0xFF9CA3AF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 获取频率文本
 */
private fun getFrequencyText(reminder: MedicationReminder): String {
    return when (reminder.frequency) {
        FrequencyType.DAILY -> "每天"
        FrequencyType.SPECIFIC_DAYS -> {
            val days = reminder.specificWeekDays.sorted().joinToString(",") { 
                when(it) {
                    1 -> "周一"
                    2 -> "周二"
                    3 -> "周三"
                    4 -> "周四"
                    5 -> "周五"
                    6 -> "周六"
                    7 -> "周日"
                    else -> ""
                }
            }
            days
        }
        FrequencyType.INTERVAL -> "每 ${reminder.intervalDays} 天"
    }
}
