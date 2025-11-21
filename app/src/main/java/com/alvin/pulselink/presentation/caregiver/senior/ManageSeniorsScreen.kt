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
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
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
                    containerColor = Color.White
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
                                SeniorCard(
                                    senior = senior,
                                    currentUserId = manageSeniorsState.currentUserId,
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
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "请使用“创建老人账户”功能添加新账户",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun SeniorCard(
    senior: Senior,
    currentUserId: String,
    onEdit: (String) -> Unit,
    onUnlink: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val isCreator = senior.creatorId == currentUserId

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
