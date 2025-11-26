package com.alvin.pulselink.presentation.senior.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 权限管理页面
 * 
 * 功能：
 * - 管理 linkRequestApprovers 列表（添加/移除有审批权的人）
 * - 编辑每个 caregiver 的细粒度权限
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionManagementScreen(
    onBackClick: () -> Unit,
    viewModel: PermissionManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditPermissionsDialog by remember { mutableStateOf<BoundCaregiverWithPermissions?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "权限管理",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.boundCaregivers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "暂无已绑定护理者",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "当有护理者绑定后，可以在这里管理他们的权限",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 说明卡片
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "权限说明",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "您可以为每位护理者设置不同的访问权限，拥有审批权限的护理者可以批准其他人的绑定请求",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }

                        // 护理者列表
                        items(uiState.boundCaregivers) { caregiver ->
                            CaregiverPermissionCard(
                                caregiver = caregiver,
                                onEditClick = { showEditPermissionsDialog = caregiver }
                            )
                        }
                    }
                }
            }
            
            // 错误提示
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearErrorMessage() }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }

    // 编辑权限对话框
    showEditPermissionsDialog?.let { caregiver ->
        EditPermissionsDialog(
            caregiver = caregiver,
            onDismiss = { showEditPermissionsDialog = null },
            onSave = { canViewHealthData, canEditHealthData, canViewReminders, canEditReminders, canApprove ->
                viewModel.updatePermissions(
                    caregiverId = caregiver.caregiverId,
                    canViewHealthData = canViewHealthData,
                    canEditHealthData = canEditHealthData,
                    canViewReminders = canViewReminders,
                    canEditReminders = canEditReminders,
                    canApprove = canApprove
                )
                showEditPermissionsDialog = null
            }
        )
    }
}

/**
 * 护理者权限卡片
 */
@Composable
private fun CaregiverPermissionCard(
    caregiver: BoundCaregiverWithPermissions,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 护理者信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            text = if (caregiver.nickname.isNotBlank()) caregiver.nickname else "护理者",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = caregiver.relationship,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 编辑按钮
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑权限",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 权限列表
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PermissionStatusItem(
                    label = "查看健康数据",
                    enabled = caregiver.canViewHealthData
                )
                PermissionStatusItem(
                    label = "编辑健康数据",
                    enabled = caregiver.canEditHealthData
                )
                PermissionStatusItem(
                    label = "查看用药提醒",
                    enabled = caregiver.canViewReminders
                )
                PermissionStatusItem(
                    label = "编辑用药提醒",
                    enabled = caregiver.canEditReminders
                )
                PermissionStatusItem(
                    label = "审批绑定请求",
                    enabled = caregiver.canApprove,
                    highlight = true // 高亮显示审批权限
                )
            }
        }
    }
}

/**
 * 权限状态显示项
 */
@Composable
private fun PermissionStatusItem(
    label: String,
    enabled: Boolean,
    highlight: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = if (enabled) {
                        if (highlight) Color(0xFFFF6B6B) else Color(0xFF4CAF50)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    },
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (enabled) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            fontWeight = if (highlight && enabled) FontWeight.Medium else FontWeight.Normal
        )
    }
}

/**
 * 编辑权限对话框
 */
@Composable
private fun EditPermissionsDialog(
    caregiver: BoundCaregiverWithPermissions,
    onDismiss: () -> Unit,
    onSave: (canViewHealthData: Boolean, canEditHealthData: Boolean, canViewReminders: Boolean, canEditReminders: Boolean, canApprove: Boolean) -> Unit
) {
    var canViewHealthData by remember { mutableStateOf(caregiver.canViewHealthData) }
    var canEditHealthData by remember { mutableStateOf(caregiver.canEditHealthData) }
    var canViewReminders by remember { mutableStateOf(caregiver.canViewReminders) }
    var canEditReminders by remember { mutableStateOf(caregiver.canEditReminders) }
    var canApprove by remember { mutableStateOf(caregiver.canApprove) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "编辑权限",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "${caregiver.nickname.ifBlank { "护理者" }} (${caregiver.relationship})",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider()
                
                // 权限开关列表
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PermissionSwitchItem(
                        label = "查看健康数据",
                        description = "允许查看血压、心率等健康数据",
                        checked = canViewHealthData,
                        onCheckedChange = { canViewHealthData = it }
                    )
                    
                    PermissionSwitchItem(
                        label = "编辑健康数据",
                        description = "允许添加或修改健康数据",
                        checked = canEditHealthData,
                        onCheckedChange = { canEditHealthData = it }
                    )
                    
                    PermissionSwitchItem(
                        label = "查看用药提醒",
                        description = "允许查看您的用药提醒列表",
                        checked = canViewReminders,
                        onCheckedChange = { canViewReminders = it }
                    )
                    
                    PermissionSwitchItem(
                        label = "编辑用药提醒",
                        description = "允许添加、修改或删除用药提醒",
                        checked = canEditReminders,
                        onCheckedChange = { canEditReminders = it }
                    )
                    
                    HorizontalDivider()
                    
                    // 审批权限（高亮显示）
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ) {
                        PermissionSwitchItem(
                            label = "审批绑定请求",
                            description = "允许批准或拒绝其他人的绑定申请",
                            checked = canApprove,
                            onCheckedChange = { canApprove = it },
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(canViewHealthData, canEditHealthData, canViewReminders, canEditReminders, canApprove)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 权限开关项
 */
@Composable
private fun PermissionSwitchItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}
