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
import com.alvin.pulselink.domain.usecase.profile.ManagedSeniorInfo
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF8B5CF6)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 显示所有创建与链接的老人（合并列表）
                    val list = remember(uiState.createdSeniors, uiState.linkedSeniors) {
                        uiState.createdSeniors + uiState.linkedSeniors
                    }

                    // 统计信息
                    val createdCount = uiState.createdSeniors.size
                    val linkedCount = uiState.linkedSeniors.size
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
                            items(list) { seniorInfo ->
                                SeniorCard(
                                    seniorInfo = seniorInfo,
                                    currentUserId = uiState.currentUserId,
                                    onEdit = { onEditSenior(it) },
                                    onUnlink = { viewModel.unlinkSenior(it) },
                                    onDelete = { viewModel.showDeleteConfirmation(seniorInfo) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 删除确认对话框
    uiState.seniorToDelete?.let { senior ->
        DeleteConfirmDialog(
            seniorName = senior.profile.name,
            onConfirm = { viewModel.executeDeleteSenior(senior.profile.id) },
            onDismiss = { viewModel.cancelDelete() }
        )
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
    seniorInfo: ManagedSeniorInfo,
    currentUserId: String,
    onEdit: (String) -> Unit,
    onUnlink: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val profile = seniorInfo.profile
    val isCreator = profile.creatorId == currentUserId
    // 检查是否是pending状态
    val isPending = seniorInfo.relation.status == "pending"
    
    // Get avatar emoji based on avatarType
    val avatarEmoji = com.alvin.pulselink.util.AvatarHelper.getAvatarEmoji(profile.avatarType)

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar emoji
                    Text(
                        text = avatarEmoji,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = Color.White,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .padding(8.dp)
                    )
                    
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Text(
                                text = profile.name,
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Basic Info
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                InfoChip(
                    icon = Icons.Default.Person,
                    label = "Age",
                    value = "${profile.age} years"
                )
                InfoChip(
                    icon = if (profile.gender == "Male") Icons.Default.Male else Icons.Default.Female,
                    label = "Gender",
                    value = profile.gender
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            Text(
                text = "Created: ${dateFormat.format(Date(profile.createdAt))}",
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
                        onClick = { onEdit(profile.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { onDelete(profile.id) },
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
                        onClick = { onUnlink(profile.id) },
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
            tint = Color(0xFF6366F1),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = label, fontSize = 10.sp, color = Color(0xFF9CA3AF))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
        }
    }
}

/**
 * 删除确认对话框
 */
@Composable
private fun DeleteConfirmDialog(
    seniorName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Delete Senior Account?",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete the account for \"$seniorName\"?",
                    fontSize = 16.sp,
                    color = Color(0xFF374151)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚠️ This action cannot be undone",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All data including health records and relationships will be permanently deleted.",
                            fontSize = 13.sp,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444)
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF6B7280))
            }
        },
        containerColor = Color.White
    )
}
