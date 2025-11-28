package com.alvin.pulselink.presentation.caregiver.seniordetail.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.alvin.pulselink.presentation.caregiver.seniordetail.components.ReminderItem
import com.alvin.pulselink.presentation.caregiver.seniordetail.viewmodels.RemindersViewModel
import com.alvin.pulselink.domain.model.ReminderStatus
import com.alvin.pulselink.presentation.common.components.PulseLinkScaffold

/**
 * Reminders Tab - 用药提醒管理
 * 
 * 功能:
 * - 查看所有设定的提醒
 * - 添加新提醒
 * - 编辑/删除提醒
 * - 启用/禁用提醒
 */
@Composable
fun RemindersTab(
    seniorId: String,
    onNavigateToAddEdit: (reminderId: String?) -> Unit,
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(seniorId) {
        viewModel.loadReminders(seniorId)
    }
    
    PulseLinkScaffold(
        uiEventFlow = viewModel.uiEvent,
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header with Add Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Medication Reminders",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = if (uiState.permissions.canEditHealthData) {
                                "Manage reminders for the senior"
                            } else {
                                "View-only mode (no edit permission)"
                            },
                            fontSize = 13.sp,
                            color = if (uiState.permissions.canEditHealthData) {
                                Color(0xFF6B7280)
                            } else {
                                Color(0xFFEF4444)
                            }
                        )
                    }
                    
                    Button(
                        onClick = {
                            if (uiState.permissions.canEditHealthData) {
                                onNavigateToAddEdit(null)
                            } else {
                                viewModel.showPermissionError("你没有编辑权限，无法创建用药提醒")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.permissions.canEditHealthData) {
                                Color(0xFF8B5CF6)
                            } else {
                                Color(0xFFD1D5DB)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        enabled = uiState.permissions.canEditHealthData
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Add", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            
            // Reminders List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6))
                }
            } else if (uiState.reminders.isEmpty()) {
                EmptyRemindersView(onAddClick = { onNavigateToAddEdit(null) })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.reminders) { reminder ->
                        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        val isOwnReminder = reminder.createdBy == currentUserId
                        
                        ReminderItem(
                            reminder = reminder,
                            canEdit = uiState.permissions.canEditHealthData,
                            isOwnReminder = isOwnReminder,
                            onToggle = { viewModel.toggleReminderStatus(reminder.id) },
                            onClick = {
                                if (isOwnReminder && uiState.permissions.canEditHealthData) {
                                    onNavigateToAddEdit(reminder.id)
                                } else if (!uiState.permissions.canEditHealthData) {
                                    viewModel.showPermissionError("你没有编辑权限")
                                } else {
                                    viewModel.showPermissionError("你只能编辑自己创建的提醒")
                                }
                            },
                            onDelete = { viewModel.deleteReminder(reminder.id) },
                            onPermissionDenied = {
                                if (!uiState.permissions.canEditHealthData) {
                                    viewModel.showPermissionError("你没有编辑权限")
                                } else {
                                    viewModel.showPermissionError("你只能修改自己创建的提醒")
                                }
                            }
                        )
                    }
                    
                    // Info Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFEFF6FF)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Reminders will be sent to the senior's device at the scheduled times. You will also receive notifications if medications are not confirmed.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E40AF),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                    
                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRemindersView(onAddClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color(0xFFF3F4F6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Medication,
                contentDescription = null,
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = "No Reminders Set",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = "Add medication reminders to help the senior stay on track",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8B5CF6)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Add First Reminder", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}


