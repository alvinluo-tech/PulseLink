package com.alvin.pulselink.presentation.senior.linkguard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.alvin.pulselink.domain.model.CaregiverRelation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 老人端 LinkGuard 页面
 * 
 * 功能：
 * - 显示待审批的 Caregiver 绑定请求
 * - 批准或拒绝请求
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeniorLinkGuardScreen(
    onBackClick: () -> Unit,
    viewModel: SeniorLinkGuardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Handle success snackbar (Channel - one-time event)
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is SeniorLinkGuardViewModel.UiEvent.ShowSuccessSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long  // 老人端使用长时间显示
                    )
                }
            }
        }
    }
    
    // Error dialog state (StateFlow - must be confirmed)
    val errorDialogState by viewModel.errorDialog.collectAsState()
    
    // Show error dialog for seniors (clear, must confirm)
    errorDialogState?.let { errorState ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissErrorDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(64.dp)  // 老人端图标更大
                )
            },
            title = {
                Text(
                    text = errorState.title,
                    fontSize = 24.sp,  // 老人端字体更大
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = errorState.message,
                    fontSize = 18.sp,  // 老人端字体更大
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissErrorDialog() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),  // 老人端按钮更高
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF448AFF)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "OK",
                        fontSize = 18.sp,  // 老人端字体更大
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Connection Requests",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Review caregiver link requests",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF448AFF),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    // 老人端自定义 Snackbar：大字体、长时间显示
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = data.visuals.message,
                            fontSize = 18.sp,  // 老人端字体更大
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F9FF))
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ⭐ Tab 切换
                var selectedTab by remember { mutableStateOf(0) }
                
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Color(0xFF448AFF)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { 
                            Text(
                                "Pending (${uiState.pendingRequests.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            ) 
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { 
                            Text(
                                "Linked (${uiState.boundCaregivers.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            ) 
                        }
                    )
                }
                
                // ⭐ Tab 内容
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        uiState.isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = Color(0xFF448AFF)
                            )
                        }
                        
                        selectedTab == 0 -> {
                            // 待处理请求
                            if (uiState.pendingRequests.isEmpty()) {
                                EmptyRequestsState(Modifier.align(Alignment.Center))
                            } else {
                                LinkRequestsList(
                                    requests = uiState.pendingRequests,
                                    isProcessing = uiState.isProcessing,
                                    onApprove = { viewModel.approveRequest(it) },
                                    onReject = { viewModel.rejectRequest(it) }
                                )
                            }
                        }
                        
                        else -> {
                            // 已绑定的 caregivers
                            if (uiState.boundCaregivers.isEmpty()) {
                                EmptyBoundCaregiversState(Modifier.align(Alignment.Center))
                            } else {
                                BoundCaregiversList(
                                    caregivers = uiState.boundCaregivers,
                                    isProcessing = uiState.isProcessing,
                                    onEditPermissions = { caregiver, canViewHealthData, canEditHealthData, canViewReminders, canEditReminders, canApproveRequests ->
                                        viewModel.updatePermissions(
                                            caregiverId = caregiver.caregiverId,
                                            canViewHealthData = canViewHealthData,
                                            canEditHealthData = canEditHealthData,
                                            canViewReminders = canViewReminders,
                                            canEditReminders = canEditReminders,
                                            canApproveRequests = canApproveRequests
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRequestsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFFE3F2FD), RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                Modifier.size(60.dp),
                tint = Color(0xFF448AFF)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "No Pending Requests",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "When caregivers send link requests, they will appear here",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyBoundCaregiversState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFFE3F2FD), RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.People,
                null,
                Modifier.size(60.dp),
                tint = Color(0xFF448AFF)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "No Linked Caregivers",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "After approving requests, caregivers will appear here",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
    }
}


@Composable
private fun LinkRequestsList(
    requests: List<CaregiverRelation>,
    isProcessing: Boolean,
    onApprove: (CaregiverRelation) -> Unit,
    onReject: (CaregiverRelation) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests) { request ->
            LinkRequestCard(
                request = request,
                isProcessing = isProcessing,
                onApprove = { onApprove(request) },
                onReject = { onReject(request) }
            )
        }
        
        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LinkRequestCard(
    request: CaregiverRelation,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 申请者头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Caregiver Request",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Text(
                        text = "From: ${request.caregiverName}",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }
                
                // 时间指示器
                Icon(
                    Icons.Default.AccessTime,
                    null,
                    Modifier.size(24.dp),
                    tint = Color(0xFFFF9800)
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Relationship info card (blue background)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(Color(0xFFE3F2FD))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Relationship:",
                        fontSize = 14.sp,
                        color = Color(0xFF448AFF),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = request.relationship,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Request message
            if (request.message.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Message:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "\"${request.message}\"",
                        fontSize = 15.sp,
                        color = Color(0xFF2C3E50),
                        lineHeight = 22.sp
                    )
                }
                
                Spacer(Modifier.height(12.dp))
            }
            
            // Request time
            Text(
                text = "Requested: ${dateFormat.format(Date(request.createdAt))}",
                fontSize = 13.sp,
                color = Color(0xFF9CA3AF)
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Safety warning banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(Color(0xFFFFF4E6)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        null,
                        Modifier.size(20.dp),
                        tint = Color(0xFFF57C00)
                    )
                    Text(
                        text = "Only approve if you know this person",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE65100)
                    )
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        Color(0xFFEF4444)
                    )
                ) {
                    Icon(
                        Icons.Default.PersonRemove,
                        null,
                        Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Reject",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Button(
                    onClick = onApprove,
                    enabled = !isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.PersonAdd,
                            null,
                            Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Approve",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}


/**
 * 已绑定的 Caregivers 列表
 */
@Composable
private fun BoundCaregiversList(
    caregivers: List<BoundCaregiver>,
    isProcessing: Boolean,
    onEditPermissions: (BoundCaregiver, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
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
                colors = CardDefaults.cardColors(Color(0xFFF0F9FF))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF448AFF),
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Manage Permissions",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You can customize access permissions for each caregiver. Click the Edit button to modify their permissions.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
        
        items(caregivers) { caregiver ->
            BoundCaregiverCard(
                caregiver = caregiver,
                isProcessing = isProcessing,
                onEditPermissions = onEditPermissions
            )
        }
        
        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * 已绑定的 Caregiver 卡片
 */
@Composable
private fun BoundCaregiverCard(
    caregiver: BoundCaregiver,
    isProcessing: Boolean,
    onEditPermissions: (BoundCaregiver, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 护理者头部信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // 头像
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF448AFF),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        // 显示caregiver的真实姓名
                        Text(
                            text = if (caregiver.caregiverName.isNotBlank()) caregiver.caregiverName else "Caregiver",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        Text(
                            text = caregiver.relationship,
                            fontSize = 14.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                
                // 编辑按钮
                IconButton(
                    onClick = { showEditDialog = true },
                    enabled = !isProcessing
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Permissions",
                        tint = Color(0xFF448AFF)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 绑定时间卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(Color(0xFFF5F9FF))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        Modifier.size(20.dp),
                        tint = Color(0xFF448AFF)
                    )
                    Column {
                        Text(
                            text = "Linked At",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = dateFormat.format(Date(caregiver.linkedAt)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2C3E50)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Permission settings - ⭐ 支持滚动
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Permissions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(Color(0xFFF8F9FA))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .heightIn(max = 200.dp), // ⭐ 限制最大高度
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PermissionItem("View Health Data", caregiver.canViewHealthData)
                        PermissionItem("Edit Health Data", caregiver.canEditHealthData)
                        PermissionItem("View Reminders", caregiver.canViewReminders)
                        PermissionItem("Edit Reminders", caregiver.canEditReminders)
                        PermissionItem("Approve Link Requests", caregiver.canApproveRequests)
                    }
                }
            }
        }
    }
    
    // ⭐ 编辑权限对话框
    if (showEditDialog) {
        EditPermissionsDialog(
            caregiver = caregiver,
            isProcessing = isProcessing,
            onDismiss = { showEditDialog = false },
            onSave = { canViewHealthData, canEditHealthData, canViewReminders, canEditReminders, canApproveRequests ->
                onEditPermissions(caregiver, canViewHealthData, canEditHealthData, canViewReminders, canEditReminders, canApproveRequests)
                showEditDialog = false
            }
        )
    }
}

/**
 * 权限项显示
 */
@Composable
private fun PermissionItem(
    label: String,
    enabled: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = if (enabled) Color(0xFF10B981) else Color(0xFFE5E7EB),
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
            color = if (enabled) Color(0xFF2C3E50) else Color(0xFF9CA3AF),
            fontWeight = if (enabled) FontWeight.Medium else FontWeight.Normal
        )
    }
}

/**
 * 编辑权限对话框
 */
@Composable
private fun EditPermissionsDialog(
    caregiver: BoundCaregiver,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onSave: (canViewHealthData: Boolean, canEditHealthData: Boolean, canViewReminders: Boolean, canEditReminders: Boolean, canApproveRequests: Boolean) -> Unit
) {
    var canViewHealthData by remember { mutableStateOf(caregiver.canViewHealthData) }
    var canEditHealthData by remember { mutableStateOf(caregiver.canEditHealthData) }
    var canViewReminders by remember { mutableStateOf(caregiver.canViewReminders) }
    var canEditReminders by remember { mutableStateOf(caregiver.canEditReminders) }
    var canApproveRequests by remember { mutableStateOf(caregiver.canApproveRequests) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xFF448AFF),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Column {
                Text(
                    "Edit Permissions",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (caregiver.caregiverName.isNotBlank()) caregiver.caregiverName else "Caregiver",
                    fontSize = 16.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Normal
                )
                Text(
                    caregiver.relationship,
                    fontSize = 14.sp,
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Normal
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()), // ⭐ 支持滚动
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HorizontalDivider()
                
                // 权限开关列表
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PermissionSwitchItem(
                        icon = Icons.Default.Visibility,
                        label = "View Health Data",
                        description = "Allow viewing blood pressure, heart rate, and other health metrics",
                        checked = canViewHealthData,
                        onCheckedChange = { canViewHealthData = it },
                        tint = Color(0xFF10B981)
                    )
                    
                    HorizontalDivider()
                    
                    PermissionSwitchItem(
                        icon = Icons.Default.Edit,
                        label = "Edit Health Data",
                        description = "Allow adding or modifying health records",
                        checked = canEditHealthData,
                        onCheckedChange = { canEditHealthData = it },
                        tint = Color(0xFF3B82F6)
                    )
                    
                    HorizontalDivider()
                    
                    PermissionSwitchItem(
                        icon = Icons.Default.Notifications,
                        label = "View Reminders",
                        description = "Allow viewing medication reminders and schedules",
                        checked = canViewReminders,
                        onCheckedChange = { canViewReminders = it },
                        tint = Color(0xFF8B5CF6)
                    )
                    
                    HorizontalDivider()
                    
                    PermissionSwitchItem(
                        icon = Icons.Default.EditNotifications,
                        label = "Edit Reminders",
                        description = "Allow creating, modifying, or deleting medication reminders",
                        checked = canEditReminders,
                        onCheckedChange = { canEditReminders = it },
                        tint = Color(0xFF6366F1)
                    )
                    
                    HorizontalDivider()
                    
                    // 审批权限（高亮显示）
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(Color(0xFFFFF4E6)),
                        border = BorderStroke(1.dp, Color(0xFFFF9800))
                    ) {
                        PermissionSwitchItem(
                            icon = Icons.Default.AdminPanelSettings,
                            label = "Approve Link Requests",
                            description = "Allow approving or rejecting connection requests from other caregivers",
                            checked = canApproveRequests,
                            onCheckedChange = { canApproveRequests = it },
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                // 安全提示
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(Color(0xFFF0F9FF))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            Modifier.size(20.dp),
                            tint = Color(0xFF448AFF)
                        )
                        Text(
                            text = "Changes take effect immediately. The caregiver will be notified of permission updates.",
                            fontSize = 13.sp,
                            color = Color(0xFF1976D2),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(canViewHealthData, canEditHealthData, canViewReminders, canEditReminders, canApproveRequests)
                },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF448AFF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Save Changes", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isProcessing,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * 权限开关项
 */
@Composable
private fun PermissionSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tint: Color = Color(0xFF448AFF),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2C3E50)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 18.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = tint,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE5E7EB)
            )
        )
    }
}
