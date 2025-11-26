package com.alvin.pulselink.presentation.caregiver.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.pulselink.presentation.caregiver.dashboard.CareNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverProfileScreen(
    viewModel: CaregiverProfileViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToManageSeniors: () -> Unit,
    onNavigateToCreateSenior: () -> Unit,
    onNavigateToManageFamily: () -> Unit,
    onNavigateToLinkGuard: () -> Unit,
    onNavigateToPrivacySecurity: () -> Unit,
    onNavigateToHelpCenter: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // 监听生命周期，当从其他页面返回时刷新数据
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPendingRequestsCount()
                viewModel.refreshManagedSeniorsData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Scaffold(
        bottomBar = {
            CareNavigationBar(
                selectedTab = 2,
                onHomeClick = onNavigateToHome,
                onChatClick = onNavigateToChat,
                onProfileClick = { }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF9333EA),
                                Color(0xFFA855F7)
                            )
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color(0xFF9333EA),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = uiState.userName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = if (uiState.isLoading) {
                            "Caregiver Account · Loading..."
                        } else {
                            "Caregiver Account · Managing\n${uiState.managedMembersCount} members"
                        },
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Care Overview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Care Overview",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2C3E50)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OverviewItem(
                            count = uiState.goodStatusCount,
                            label = "Good Status",
                            color = Color(0xFF10B981),
                            icon = Icons.Default.Check,
                            isLoading = uiState.isLoading
                        )
                        OverviewItem(
                            count = uiState.attentionCount,
                            label = "Need Attention",
                            color = Color(0xFFF59E0B),
                            icon = Icons.Default.Warning,
                            isLoading = uiState.isLoading
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OverviewItem(
                            count = uiState.urgentCount,
                            label = "Urgent",
                            color = Color(0xFFEF4444),
                            icon = Icons.Default.Warning,
                            isLoading = uiState.isLoading
                        )
                        OverviewItem(
                            count = uiState.activeAlertsCount,
                            label = "Active Alerts",
                            color = Color(0xFF8B5CF6),
                            icon = Icons.Default.Notifications,
                            isLoading = uiState.isLoading
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Menu Items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    onClick = onNavigateToSettings
                )
                
                // 新增：管理老人（包含我创建与我已绑定）
                ProfileMenuItem(
                    icon = Icons.Default.People,
                    title = "Manage Seniors",
                    onClick = onNavigateToManageSeniors
                )
                
                ProfileMenuItem(
                    icon = Icons.Default.PersonAdd,
                    title = "Create Senior Account",
                    onClick = onNavigateToCreateSenior
                )
                
                ProfileMenuItem(
                    icon = Icons.Default.Link,
                    title = "Link Senior Account",
                    onClick = onNavigateToManageFamily
                )
                
                ProfileMenuItem(
                    icon = Icons.Default.VerifiedUser,
                    title = "Link Guard",
                    subtitle = if (uiState.pendingRequestsCount > 0) "${ uiState.pendingRequestsCount} pending requests" else null,
                    showBadge = uiState.pendingRequestsCount > 0,
                    badgeCount = uiState.pendingRequestsCount,
                    onClick = onNavigateToLinkGuard
                )
                
                ProfileMenuItem(
                    icon = Icons.Default.Security,
                    title = "Privacy & Security",
                    onClick = onNavigateToPrivacySecurity
                )
                
                ProfileMenuItem(
                    icon = Icons.Default.Help,
                    title = "Help Center",
                    onClick = onNavigateToHelpCenter
                )
                
                // Logout Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onLogout),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Log Out",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Log Out",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OverviewItem(
    count: Int,
    label: String,
    color: Color,
    icon: ImageVector,
    isLoading: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isLoading) "-" else count.toString(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF6B7280),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    showBadge: Boolean = false,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(24.dp)
                )
                if (showBadge && badgeCount > 0) {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp)
                    ) {
                        Text(
                            text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50)
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = Color(0xFF8B5CF6)
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
