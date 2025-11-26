package com.alvin.pulselink.presentation.senior.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.pulselink.presentation.common.components.SeniorBottomNavigationBar
import com.alvin.pulselink.presentation.common.components.LoadingScreen
import com.alvin.pulselink.ui.theme.PulseLinkTheme

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToAssistant: () -> Unit = {},
    onNavigateToReminderList: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToLinkGuard: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(2) } // Profile tab selected
    
    Scaffold(
        containerColor = Color(0xFFE8EDF2),
        bottomBar = {
            SeniorBottomNavigationBar(
                selectedItem = selectedTab,
                onItemSelected = { 
                    selectedTab = it
                    when (it) {
                        0 -> onNavigateToHome()
                        1 -> onNavigateToAssistant()
                        2 -> { /* Current screen */ }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingScreen()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // User Profile Card
                UserProfileCard(uiState = uiState)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Today's Health Summary
                HealthSummaryCard(uiState = uiState)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Menu Items
                MenuSection(
                    onReminderClick = onNavigateToReminderList,
                    onSettingsClick = onNavigateToSettings,
                    onLinkGuardClick = onNavigateToLinkGuard,
                    onLogout = onLogout
                )
                
                // 添加底部间距，确保内容不被底部导航栏遮挡
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun UserProfileCard(uiState: ProfileUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF5B9FFF),
                            Color(0xFF4A8EEE)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // User Avatar - Display Emoji instead of Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.avatarEmoji,
                        fontSize = 48.sp,
                        color = Color.Unspecified
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // User Name
                Text(
                    text = uiState.userName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Age and Usage Info - Display real data
                Text(
                    text = if (uiState.age > 0 && uiState.daysUsed >= 0) {
                        "Age ${uiState.age} · Used ${uiState.daysUsed} ${if (uiState.daysUsed == 1) "day" else "days"}"
                    } else {
                        "Loading..."
                    },
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun HealthSummaryCard(uiState: ProfileUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Today's Health Summary",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Blood Pressure Card
                HealthMetricCard(
                    icon = Icons.Default.Favorite,
                    iconColor = Color(0xFF4CAF50),
                    backgroundColor = Color(0xFFE8F5E9),
                    value = uiState.bloodPressure,
                    label = uiState.bloodPressureStatus,
                    modifier = Modifier.weight(1f)
                )
                
                // Heart Rate Card
                HealthMetricCard(
                    icon = Icons.Default.LocalHospital,
                    iconColor = Color(0xFF2196F3),
                    backgroundColor = Color(0xFFE3F2FD),
                    value = uiState.heartRate.toString(),
                    label = "Heart Rate/min",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HealthMetricCard(
    icon: ImageVector,
    iconColor: Color,
    backgroundColor: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF5F6F7E),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MenuSection(
    onReminderClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLinkGuardClick: () -> Unit = {},
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MenuItemCard(
            icon = Icons.Default.Settings,
            title = "Settings",
            onClick = onSettingsClick
        )
        
        MenuItemCard(
            icon = Icons.Outlined.Notifications,
            title = "Reminders",
            onClick = onReminderClick
        )
        
        // Link Guard 菜单项
        MenuItemCard(
            icon = Icons.Outlined.Shield,
            title = "Link Guard",
            subtitle = "Manage caregiver requests",
            onClick = onLinkGuardClick
        )
        
        MenuItemCard(
            icon = Icons.AutoMirrored.Outlined.Help,
            title = "Help Center",
            onClick = { /* TODO: Navigate to Help */ }
        )
        
        // Log Out Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFEBEE)
            ),
            onClick = onLogout
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Log Out",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "Log Out",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE53935)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF5F6F7E),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2C3E50)
                    )
                    
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = Color(0xFF5F6F7E)
                        )
                    }
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Local BottomNavigationBar removed in favor of shared SeniorBottomNavigationBar

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    PulseLinkTheme {
        ProfileScreen()
    }
}
