package com.alvin.pulselink.presentation.caregiver.seniordetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.pulselink.presentation.caregiver.seniordetail.tabs.ReportsTab
import com.alvin.pulselink.presentation.caregiver.seniordetail.tabs.AlertsTab
import com.alvin.pulselink.presentation.caregiver.seniordetail.tabs.RemindersTab
import com.alvin.pulselink.presentation.common.components.PulseLinkScaffold

/**
 * Senior Detail Screen - Caregiver 端查看老人详情
 * 
 * 包含三个 Tab:
 * - Reports: 健康报告（每日 + 周期）
 * - Alerts: 健康状态历史
 * - Reminders: 用药提醒管理
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeniorDetailScreen(
    seniorId: String,
    seniorName: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddEditMedication: (reminderId: String?) -> Unit = {},
    viewModel: SeniorDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    
    LaunchedEffect(seniorId) {
        viewModel.loadSeniorDetails(seniorId)
    }
    
    PulseLinkScaffold(
        uiEventFlow = viewModel.uiEvent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF8B5CF6))
            ) {
                // Top Bar with back button
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = seniorName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Health Overview",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF8B5CF6)
                    )
                )
                
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xFF8B5CF6),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = Color.White,
                                height = 3.dp
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Reports", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        },
                        selectedContentColor = Color.White,
                        unselectedContentColor = Color.White.copy(alpha = 0.7f)
                    )
                    
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Alerts", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        },
                        selectedContentColor = Color.White,
                        unselectedContentColor = Color.White.copy(alpha = 0.7f)
                    )
                    
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Reminders", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        },
                        selectedContentColor = Color.White,
                        unselectedContentColor = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(paddingValues)
        ) {
            when (selectedTabIndex) {
                0 -> ReportsTab(seniorId = seniorId)
                1 -> AlertsTab(seniorId = seniorId)
                2 -> RemindersTab(
                    seniorId = seniorId,
                    onNavigateToAddEdit = onNavigateToAddEditMedication
                )
            }
        }
    }
}
