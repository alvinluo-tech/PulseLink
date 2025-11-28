package com.alvin.pulselink.presentation.caregiver.seniordetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvin.pulselink.presentation.caregiver.seniordetail.viewmodels.DailyHealthReport
import com.alvin.pulselink.presentation.caregiver.seniordetail.viewmodels.HealthMetric
import com.alvin.pulselink.presentation.caregiver.seniordetail.viewmodels.MetricStatus
import com.alvin.pulselink.presentation.caregiver.seniordetail.viewmodels.ReportsUiState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Daily Report View
 * 显示某天的详细健康数据
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportView(
    uiState: ReportsUiState,
    selectedDate: Date,
    onDateChanged: (Date) -> Unit,
    onRefresh: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Picker Card
        item {
            DatePickerCard(
                selectedDate = selectedDate,
                dateFormatter = dateFormatter,
                onPreviousDay = {
                    val cal = Calendar.getInstance()
                    cal.time = selectedDate
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    onDateChanged(cal.time)
                },
                onNextDay = {
                    val cal = Calendar.getInstance()
                    cal.time = selectedDate
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    onDateChanged(cal.time)
                },
                onJumpToToday = {
                    onDateChanged(Date())
                },
                onDateClick = {
                    showDatePicker = true
                }
            )
        }
        
        // Health Metrics
        if (uiState.dailyReport != null) {
            item {
                Text(
                    text = "Today",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Text(
                    text = dateFormatter.format(selectedDate),
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            }
            
            item {
                HealthMetricCard(
                    icon = Icons.Default.Favorite,
                    iconColor = Color(0xFFEF4444),
                    title = "Blood Pressure (Avg)",
                    metric = uiState.dailyReport.bloodPressure
                )
            }
            
            item {
                HealthMetricCard(
                    icon = Icons.Default.MonitorHeart,
                    iconColor = Color(0xFF3B82F6),
                    title = "Heart Rate (Avg)",
                    metric = uiState.dailyReport.heartRate
                )
            }
            
            item {
                HealthMetricCard(
                    icon = Icons.Default.Medication,
                    iconColor = Color(0xFF10B981),
                    title = "Medication (Taken/Total)",
                    metric = uiState.dailyReport.medication
                )
            }
        } else {
            item {
                EmptyDailyReportView()
            }
        }
        
        item {
            Spacer(Modifier.height(16.dp))
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.time
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateChanged(Date(millis))
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B5CF6)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Confirm",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        "Cancel",
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = DatePickerDefaults.colors(
                containerColor = Color.White,
                selectedDayContainerColor = Color(0xFF8B5CF6),
                selectedDayContentColor = Color.White,
                todayContentColor = Color(0xFF8B5CF6),
                todayDateBorderColor = Color(0xFF8B5CF6)
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = Color(0xFF8B5CF6),
                    selectedDayContentColor = Color.White,
                    todayContentColor = Color(0xFF8B5CF6),
                    todayDateBorderColor = Color(0xFF8B5CF6),
                    selectedYearContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                    selectedYearContentColor = Color(0xFF8B5CF6),
                    currentYearContentColor = Color(0xFF8B5CF6)
                )
            )
        }
    }
}

@Composable
private fun DatePickerCard(
    selectedDate: Date,
    dateFormatter: SimpleDateFormat,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onJumpToToday: () -> Unit,
    onDateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousDay,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF3F4F6), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous day",
                        tint = Color(0xFF6B7280)
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onDateClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Today",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = dateFormatter.format(selectedDate),
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                
                IconButton(
                    onClick = onNextDay,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF3F4F6), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next day",
                        tint = Color(0xFF6B7280)
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDateClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF8B5CF6)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Select Date", fontSize = 13.sp)
                }
                
                OutlinedButton(
                    onClick = onJumpToToday,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF8B5CF6)
                    )
                ) {
                    Text("Today", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun HealthMetricCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    metric: HealthMetric
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(iconColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = metric.value,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                }
            }
            
            StatusBadge(status = metric.status)
        }
    }
}

@Composable
private fun StatusBadge(status: MetricStatus) {
    val (color, text) = when (status) {
        MetricStatus.GOOD -> Color(0xFF10B981) to "Good"
        MetricStatus.NORMAL -> Color(0xFF3B82F6) to "Normal"
        MetricStatus.WARNING -> Color(0xFFF59E0B) to "Warning"
        MetricStatus.CRITICAL -> Color(0xFFEF4444) to "Critical"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
private fun EmptyDailyReportView() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = null,
                tint = Color(0xFFD1D5DB),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "No Data for This Date",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7280)
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "The senior hasn't reported health data for this date",
                fontSize = 13.sp,
                color = Color(0xFF9CA3AF),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
