package com.alvin.pulselink.presentation.caregiver.senior

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.pulselink.domain.model.Senior
import java.text.SimpleDateFormat
import java.util.*

/**
 * 管理老人账户页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSeniorsScreen(
    viewModel: ManageSeniorsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onCreateSenior: () -> Unit,
    onEditSenior: (String) -> Unit
) {
    val manageSeniorsState by viewModel.manageSeniorsState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Senior Accounts",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF8B5CF6)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF111827),
                    navigationIconContentColor = Color(0xFF8B5CF6)
                )
            )
        },
        // snackbarHost 已移除
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(paddingValues)
        ) {
            if (manageSeniorsState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF8B5CF6)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 显示所有创建与链接的老人（合并列表）
                    val list = remember(manageSeniorsState.createdSeniors, manageSeniorsState.linkedSeniors) {
                        manageSeniorsState.createdSeniors + manageSeniorsState.linkedSeniors
                    }

                    // 统计信息
                    val createdCount = manageSeniorsState.createdSeniors.size
                    val linkedCount = manageSeniorsState.linkedSeniors.size
                    val totalCount = createdCount + linkedCount

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(label = "Total", value = totalCount)
                        StatCard(label = "Created", value = createdCount)
                        StatCard(label = "Linked", value = linkedCount)
                    }

                    if (list.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) { EmptyState() }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(list) { senior ->
                                val pendingRequest = manageSeniorsState.pendingRequestsMap[senior.id]
                                SeniorCard(
                                    senior = senior,
                                    currentUserId = manageSeniorsState.currentUserId,
                                    pendingRequest = pendingRequest,
                                    onEdit = { onEditSenior(it) },
                                    onUnlink = { viewModel.unlinkSenior(it) },
                                    onDelete = { viewModel.deleteSenior(it) }
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
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.People,
            contentDescription = null,
            tint = Color(0xFFD1D5DB),
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Senior Accounts Yet",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7280)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create a new senior account or link to an existing one to get started",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun SeniorCard(
    senior: Senior,
    currentUserId: String,
    pendingRequest: com.alvin.pulselink.domain.model.LinkRequest?,
    onEdit: (String) -> Unit,
    onUnlink: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val isCreator = senior.creatorId == currentUserId
    // 检查是否是pending状态
    val isPending = pendingRequest != null && pendingRequest.status == "pending"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
        onClick = {}
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Text(
                            text = senior.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // 显示状态标签
                        if (isPending) {
                            // Pending状态标签
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = "Pending Approval",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            // Created/Linked状态标签
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isCreator) Color(0xFFDCFCE7) else Color(0xFFDEEDFF)
                            ) {
                                Text(
                                    text = if (isCreator) "Created" else "Linked",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCreator) Color(0xFF16A34A) else Color(0xFF2563EB),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Basic Info
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                InfoChip(
                    icon = Icons.Default.Person,
                    label = "Age",
                    value = "${senior.age} years"
                )
                InfoChip(
                    icon = if (senior.gender == "Male") Icons.Default.Male else Icons.Default.Female,
                    label = "Gender",
                    value = senior.gender
                )
            }

            // Health Snapshot
            senior.healthHistory.bloodPressure?.let { bp ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Health Snapshot",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    HealthMetricCard(
                        label = "Blood Pressure",
                        value = "${bp.systolic}/${bp.diastolic}",
                        unit = "mmHg",
                        modifier = Modifier.weight(1f)
                    )
                    senior.healthHistory.heartRate?.let { hr ->
                        HealthMetricCard(
                            label = "Heart Rate",
                            value = hr.toString(),
                            unit = "bpm",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            Text(
                text = "Created: ${dateFormat.format(Date(senior.createdAt))}",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF)
            )
            
            // 显示pending状态的提示信息
            if (isPending) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFBBF24))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Your link request is awaiting approval from the account creator.",
                            fontSize = 13.sp,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isCreator) {
                    Button(
                        onClick = { onEdit(senior.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { onDelete(senior.id) },
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Account")
                    }
                } else if (isPending) {
                    // Pending状态下不显示操作按钮
                    Text(
                        text = "Waiting for approval",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    OutlinedButton(
                        onClick = { onUnlink(senior.id) },
                        border = BorderStroke(1.dp, Color(0xFF2563EB)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unlink")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF9FAFB),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = label, fontSize = 12.sp, color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF8B5CF6),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, fontSize = 12.sp, color = Color(0xFF9CA3AF))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2C3E50))
        }
    }
}

@Composable
private fun HealthMetricCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF9FAFB),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 11.sp, color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = unit, fontSize = 11.sp, color = Color(0xFF9CA3AF))
            }
        }
    }
}
