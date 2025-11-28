package com.alvin.pulselink.presentation.caregiver.seniordetail.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.alvin.pulselink.presentation.caregiver.seniordetail.components.AlertItem
import com.alvin.pulselink.presentation.caregiver.seniordetail.viewmodels.AlertsViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Alerts Tab - 健康历史记录
 * 
 * 显示老人的所有健康上报历史，包括:
 * - 血压记录
 * - 心率记录
 * - 用药记录
 * - 活动记录
 */
@Composable
fun AlertsTab(
    seniorId: String,
    viewModel: AlertsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var filterType by remember { mutableStateOf(AlertFilterType.ALL) }
    
    LaunchedEffect(seniorId) {
        viewModel.loadAlerts(seniorId)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        // Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = filterType == AlertFilterType.ALL,
                    onClick = { 
                        filterType = AlertFilterType.ALL
                        viewModel.filterAlerts(AlertFilterType.ALL)
                    },
                    label = { Text("All", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF8B5CF6),
                        selectedLabelColor = Color.White
                    )
                )
            }
            
            item {
                FilterChip(
                    selected = filterType == AlertFilterType.BLOOD_PRESSURE,
                    onClick = { 
                        filterType = AlertFilterType.BLOOD_PRESSURE
                        viewModel.filterAlerts(AlertFilterType.BLOOD_PRESSURE)
                    },
                    label = { Text("Blood Pressure", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEF4444),
                        selectedLabelColor = Color.White
                    )
                )
            }
            
            item {
                FilterChip(
                    selected = filterType == AlertFilterType.HEART_RATE,
                    onClick = { 
                        filterType = AlertFilterType.HEART_RATE
                        viewModel.filterAlerts(AlertFilterType.HEART_RATE)
                    },
                    label = { Text("Heart Rate", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MonitorHeart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF3B82F6),
                        selectedLabelColor = Color.White
                    )
                )
            }
            
            item {
                FilterChip(
                    selected = filterType == AlertFilterType.MEDICATION,
                    onClick = { 
                        filterType = AlertFilterType.MEDICATION
                        viewModel.filterAlerts(AlertFilterType.MEDICATION)
                    },
                    label = { Text("Medication", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF10B981),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        
        // Alerts List
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF8B5CF6))
            }
        } else if (uiState.filteredAlerts.isEmpty()) {
            EmptyAlertsView()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.filteredAlerts) { alert ->
                    AlertItem(alert = alert)
                }
                
                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyAlertsView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsNone,
            contentDescription = null,
            tint = Color(0xFFD1D5DB),
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "No Health Records Yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7280)
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = "Health reports will appear here once the senior starts logging data",
            fontSize = 14.sp,
            color = Color(0xFF9CA3AF),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

enum class AlertFilterType {
    ALL,
    BLOOD_PRESSURE,
    HEART_RATE,
    MEDICATION,
    ACTIVITY
}
