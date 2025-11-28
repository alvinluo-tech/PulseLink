package com.alvin.pulselink.presentation.caregiver.seniordetail.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvin.pulselink.presentation.caregiver.seniordetail.viewmodels.AlertStatus
import com.alvin.pulselink.presentation.caregiver.seniordetail.viewmodels.AlertType
import com.alvin.pulselink.presentation.caregiver.seniordetail.viewmodels.HealthAlert
import java.text.SimpleDateFormat
import java.util.*

/**
 * Alert Item Component
 * 显示单条健康历史记录
 */
@Composable
fun AlertItem(alert: HealthAlert) {
    val timeFormatter = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        getAlertIconColor(alert.type).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getAlertIcon(alert.type),
                    contentDescription = null,
                    tint = getAlertIconColor(alert.type),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alert.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827)
                    )
                    
                    StatusChip(status = alert.status)
                }
                
                Text(
                    text = alert.value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
                
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
                        text = timeFormatter.format(alert.timestamp),
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: AlertStatus) {
    val (bgColor, textColor, text) = when (status) {
        AlertStatus.NORMAL -> Triple(
            Color(0xFFDCFCE7),
            Color(0xFF166534),
            "Normal"
        )
        AlertStatus.WARNING -> Triple(
            Color(0xFFFEF3C7),
            Color(0xFFB45309),
            "Warning"
        )
        AlertStatus.CRITICAL -> Triple(
            Color(0xFFFEE2E2),
            Color(0xFF991B1B),
            "Critical"
        )
    }
    
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun getAlertIcon(type: AlertType): ImageVector {
    return when (type) {
        AlertType.BLOOD_PRESSURE -> Icons.Default.Favorite
        AlertType.HEART_RATE -> Icons.Default.MonitorHeart
        AlertType.MEDICATION -> Icons.Default.Medication
        AlertType.ACTIVITY -> Icons.Default.DirectionsWalk
    }
}

private fun getAlertIconColor(type: AlertType): Color {
    return when (type) {
        AlertType.BLOOD_PRESSURE -> Color(0xFFEF4444)
        AlertType.HEART_RATE -> Color(0xFF3B82F6)
        AlertType.MEDICATION -> Color(0xFF10B981)
        AlertType.ACTIVITY -> Color(0xFF8B5CF6)
    }
}
