package com.alvin.pulselink.presentation.caregiver.seniordetail.components

import androidx.compose.animation.core.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvin.pulselink.presentation.caregiver.seniordetail.viewmodels.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale

/**
 * Period Summary View
 * 显示一段时间的健康趋势和AI分析
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSummaryView(
    uiState: ReportsUiState,
    onRefresh: () -> Unit,
    onPeriodSelected: (Date, Date) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    var showPeriodPicker by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (!uiState.hasPeriodSelected || uiState.periodSummary == null) {
            // 空状态 - 未选择周期
            EmptyPeriodStateView(
                onSelectPeriod = { showPeriodPicker = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val summary = uiState.periodSummary!!
                
                // Period Info Header
                item {
                    PeriodInfoCard(
                        startDate = dateFormatter.format(summary.startDate),
                        endDate = dateFormatter.format(summary.endDate),
                        daysAnalyzed = summary.daysAnalyzed,
                        onChangePeriod = { showPeriodPicker = true }
                    )
                }
            
            // Blood Pressure Trend
            item {
                TrendCard(
                    icon = Icons.Default.Favorite,
                    iconColor = Color(0xFFEF4444),
                    title = "Blood Pressure Trend",
                    trendData = summary.bloodPressureTrend
                )
            }
            
            // Heart Rate Analysis
            item {
                TrendCard(
                    icon = Icons.Default.MonitorHeart,
                    iconColor = Color(0xFF3B82F6),
                    title = "Heart Rate Analysis",
                    trendData = summary.heartRateTrend
                )
            }
            
            // Medication Adherence
            item {
                MedicationAdherenceCard(
                    adherenceRate = summary.medicationAdherence,
                    note = summary.medicationNote
                )
            }
            
            // Activity Summary
            item {
                ActivitySummaryCard(activitySummary = summary.activitySummary)
            }
            
            // Alerts
            item {
                KeyObservationsCard(observations = summary.keyObservations)
            }
            
            item {
                Spacer(Modifier.height(16.dp))
            }
        }
        }
        
        // Period Picker Dialog
        if (showPeriodPicker) {
            PeriodPickerDialog(
                onDismiss = { showPeriodPicker = false },
                onPeriodSelected = { startDate, endDate ->
                    onPeriodSelected(startDate, endDate)
                    showPeriodPicker = false
                }
            )
        }
    }
}

@Composable
private fun PeriodInfoCard(
    startDate: String,
    endDate: String,
    daysAnalyzed: Int,
    onChangePeriod: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF8B5CF6)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Period Summary",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "$startDate - $endDate",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$daysAnalyzed days",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            
            IconButton(
                onClick = onChangePeriod,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Change Period",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun OverallHealthScoreCard(
    score: Int,
    status: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Overall Health Score",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score Circle
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF8B5CF6),
                        strokeWidth = 8.dp,
                        trackColor = Color(0xFFF3F4F6),
                    )
                    Text(
                        text = score.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5CF6)
                    )
                }
                
                Text(
                    text = status,
                    fontSize = 14.sp,
                    color = Color(0xFF374151),
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TrendCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    trendData: TrendData
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Average Reading",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = trendData.average,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }
            
            if (trendData.range != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Range",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = trendData.range,
                        fontSize = 14.sp,
                        color = Color(0xFF374151)
                    )
                }
            }
            
            if (trendData.trend != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Trend",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = trendData.trend,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
        }
    }
}

@Composable
private fun MedicationAdherenceCard(
    adherenceRate: Float,
    note: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Medication Adherence",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Adherence Rate",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = "${adherenceRate.toInt()}%",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { adherenceRate / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFF10B981),
                trackColor = Color(0xFFF3F4F6),
            )
            
            Spacer(Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = note,
                    fontSize = 13.sp,
                    color = Color(0xFF166534),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun ActivitySummaryCard(activitySummary: ActivitySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Activity Summary",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ActivityMetric(
                    label = "Daily Average Steps",
                    value = activitySummary.dailyAverage.toString()
                )
                ActivityMetric(
                    label = "Total Steps",
                    value = activitySummary.totalSteps.toString()
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ActivityMetric(
                    label = "Avg. Active Time",
                    value = activitySummary.avgActiveTime
                )
                ActivityMetric(
                    label = "Active Days",
                    value = activitySummary.activeDays
                )
            }
        }
    }
}

@Composable
private fun ActivityMetric(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF6B7280)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
    }
}

@Composable
private fun KeyObservationsCard(observations: List<Observation>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Alerts",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                observations.forEach { observation ->
                    ObservationItem(observation = observation)
                }
            }
        }
    }
}

@Composable
private fun ObservationItem(observation: Observation) {
    val (bgColor, iconColor, icon) = when (observation.type) {
        ObservationType.WARNING -> Triple(
            Color(0xFFFEF3C7),
            Color(0xFFD97706),
            Icons.Default.Warning
        )
        ObservationType.POSITIVE -> Triple(
            Color(0xFFD1FAE5),
            Color(0xFF10B981),
            Icons.Default.CheckCircle
        )
        ObservationType.INFO -> Triple(
            Color(0xFFDEDBFF),
            Color(0xFF8B5CF6),
            Icons.Default.Info
        )
    }
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(bgColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AIRecommendationsCard(recommendations: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "AI Recommendations",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            recommendations.forEachIndexed { index, recommendation ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "•",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5CF6)
                            )
                        }
                    }
                    Text(
                        text = recommendation,
                        fontSize = 13.sp,
                        color = Color(0xFF374151),
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 空状态 - 未选择周期
 */
@Composable
private fun EmptyPeriodStateView(onSelectPeriod: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            tint = Color(0xFF8B5CF6),
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = "Select a Time Period",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
        
        Spacer(Modifier.height(12.dp))
        
        Text(
            text = "Choose a date range to view health trends and AI-powered insights",
            fontSize = 15.sp,
            color = Color(0xFF6B7280),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 22.sp
        )
        
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = onSelectPeriod,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8B5CF6)
            ),
            shape = RoundedCornerShape(25.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Select Period",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * 周期选择器对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodPickerDialog(
    onDismiss: () -> Unit,
    onPeriodSelected: (Date, Date) -> Unit
) {
    var selectedPreset by remember { mutableStateOf<PeriodPreset?>(null) }
    var showCustomPicker by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf<Date?>(null) }
    var customEndDate by remember { mutableStateOf<Date?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Time Period",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quick Select:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                )
                
                // 预设周期选项
                PeriodPreset.values().forEach { preset ->
                    OutlinedButton(
                        onClick = { selectedPreset = preset },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedPreset == preset) 
                                Color(0xFFF5F3FF) else Color.Transparent,
                            contentColor = if (selectedPreset == preset)
                                Color(0xFF8B5CF6) else Color(0xFF6B7280)
                        ),
                        border = BorderStroke(
                            width = if (selectedPreset == preset) 2.dp else 1.dp,
                            color = if (selectedPreset == preset) 
                                Color(0xFF8B5CF6) else Color(0xFFE5E7EB)
                        )
                    ) {
                        Text(
                            text = preset.label,
                            fontSize = 14.sp,
                            fontWeight = if (selectedPreset == preset) 
                                FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = { showCustomPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Custom Range", fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val (start, end) = when {
                        selectedPreset != null -> selectedPreset!!.getDateRange()
                        customStartDate != null && customEndDate != null -> Pair(customStartDate!!, customEndDate!!)
                        else -> return@Button
                    }
                    onPeriodSelected(start, end)
                },
                enabled = selectedPreset != null || (customStartDate != null && customEndDate != null),
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
                onClick = onDismiss,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    "Cancel",
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
    
    // Custom Date Range Picker - Using Material 3 DateRangePicker
    if (showCustomPicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        
        DatePickerDialog(
            onDismissRequest = {
                showCustomPicker = false
            },
            confirmButton = {
                Button(
                    onClick = {
                        val startMillis = dateRangePickerState.selectedStartDateMillis
                        val endMillis = dateRangePickerState.selectedEndDateMillis
                        
                        if (startMillis != null && endMillis != null) {
                            customStartDate = Date(startMillis)
                            customEndDate = Date(endMillis)
                            showCustomPicker = false
                        }
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null && 
                             dateRangePickerState.selectedEndDateMillis != null,
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
                    onClick = {
                        showCustomPicker = false
                    },
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
                containerColor = Color.White
            )
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Row(
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Select Date Range",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    }
                },
                headline = {
                    val startDate = dateRangePickerState.selectedStartDateMillis?.let { 
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "Start"
                    val endDate = dateRangePickerState.selectedEndDateMillis?.let { 
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "End"
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Start",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                startDate,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dateRangePickerState.selectedStartDateMillis != null) 
                                    Color(0xFF8B5CF6) else Color(0xFF9CA3AF)
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFFD1D5DB),
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Column {
                            Text(
                                "End",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                endDate,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dateRangePickerState.selectedEndDateMillis != null) 
                                    Color(0xFF8B5CF6) else Color(0xFF9CA3AF)
                            )
                        }
                    }
                },
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = Color(0xFF8B5CF6),
                    selectedDayContentColor = Color.White,
                    todayContentColor = Color(0xFF8B5CF6),
                    todayDateBorderColor = Color(0xFF8B5CF6),
                    selectedYearContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                    selectedYearContentColor = Color(0xFF8B5CF6),
                    currentYearContentColor = Color(0xFF8B5CF6),
                    dayInSelectionRangeContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.1f),
                    dayInSelectionRangeContentColor = Color(0xFF1F2937)
                )
            )
        }
    }
}

/**
 * 预设周期枚举
 */
enum class PeriodPreset(val label: String, val days: Int) {
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_14_DAYS("Last 14 Days", 14),
    LAST_30_DAYS("Last 30 Days", 30),
    LAST_3_MONTHS("Last 3 Months", 90);
    
    fun getDateRange(): Pair<Date, Date> {
        val end = Date()
        val start = Date(end.time - days * 24 * 60 * 60 * 1000L)
        return Pair(start, end)
    }
}
