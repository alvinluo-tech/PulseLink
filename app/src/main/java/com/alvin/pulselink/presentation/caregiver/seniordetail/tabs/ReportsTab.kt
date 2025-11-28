package com.alvin.pulselink.presentation.caregiver.seniordetail.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.pulselink.presentation.caregiver.seniordetail.components.DailyReportView
import com.alvin.pulselink.presentation.caregiver.seniordetail.components.PeriodSummaryView
import com.alvin.pulselink.presentation.caregiver.seniordetail.viewmodels.ReportsViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reports Tab - 健康报告
 * 
 * 包含两个视图:
 * - Daily Report: 查看某天的详细健康数据
 * - Period Summary: 查看一段时间的健康趋势和AI分析
 */
@Composable
fun ReportsTab(
    seniorId: String,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedView by remember { mutableStateOf(ReportView.DAILY) }
    var selectedDate by remember { mutableStateOf(Date()) }
    
    LaunchedEffect(seniorId) {
        viewModel.loadDailyReport(seniorId, selectedDate)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        // View Toggle Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ViewToggleButton(
                text = "Daily Report",
                icon = Icons.Default.CalendarToday,
                selected = selectedView == ReportView.DAILY,
                onClick = { selectedView = ReportView.DAILY },
                modifier = Modifier.weight(1f)
            )
            
            ViewToggleButton(
                text = "Period Summary",
                icon = Icons.Default.Timeline,
                selected = selectedView == ReportView.PERIOD,
                onClick = { 
                    selectedView = ReportView.PERIOD
                    viewModel.loadPeriodSummary(seniorId)
                },
                modifier = Modifier.weight(1f)
            )
        }
        
        // Content based on selected view
        when (selectedView) {
            ReportView.DAILY -> {
                DailyReportView(
                    uiState = uiState,
                    selectedDate = selectedDate,
                    onDateChanged = { newDate ->
                        selectedDate = newDate
                        viewModel.loadDailyReport(seniorId, newDate)
                    },
                    onRefresh = { viewModel.loadDailyReport(seniorId, selectedDate) }
                )
            }
            ReportView.PERIOD -> {
                PeriodSummaryView(
                    uiState = uiState,
                    onRefresh = { viewModel.loadPeriodSummary(seniorId) },
                    onPeriodSelected = { startDate, endDate ->
                        viewModel.loadPeriodSummary(seniorId, startDate, endDate)
                    }
                )
            }
        }
    }
}

@Composable
private fun ViewToggleButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF8B5CF6) else Color.White,
            contentColor = if (selected) Color.White else Color(0xFF64748B)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (selected) 4.dp else 2.dp
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

enum class ReportView {
    DAILY,
    PERIOD
}
