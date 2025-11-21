package com.alvin.pulselink.presentation.caregiver.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class LovedOne(
    val id: String,
    val name: String,
    val relationship: String,
    val emoji: String,
    val status: HealthStatus,
    val statusMessage: String,
    val statusColor: Color,
    val borderColor: Color
)

enum class HealthStatus {
    GOOD, ATTENTION, URGENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareDashboardScreen(
    viewModel: CareDashboardViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLovedOneClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        bottomBar = {
            CareNavigationBar(
                selectedTab = 0,
                onHomeClick = { },
                onChatClick = onNavigateToChat,
                onProfileClick = onNavigateToProfile
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp)
        ) {
            // Header
            Text(
                text = "Care Dashboard",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Text(
                text = "Managing ${uiState.lovedOnes.size} loved ones",
                fontSize = 16.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Status Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusCard(
                    count = uiState.goodCount,
                    label = "Good",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                StatusCard(
                    count = uiState.attentionCount,
                    label = "Attention",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                StatusCard(
                    count = uiState.urgentCount,
                    label = "Urgent",
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Loved Ones List
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = Color(0xFF9333EA),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your Loved Ones",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2C3E50)
                )
            }
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.lovedOnes) { lovedOne ->
                    LovedOneCard(
                        lovedOne = lovedOne,
                        onClick = { onLovedOneClick(lovedOne.id) }
                    )
                }
                
                item {
                    // Bottom hint
                    Text(
                        text = "Tap on any person to view detailed health\nreports and alerts",
                        fontSize = 14.sp,
                        color = Color(0xFF9333EA),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .background(
                                color = Color(0xFFF3E8FF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                when (label) {
                    "Good" -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    "Attention" -> Text(
                        text = "!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    "Urgent" -> Text(
                        text = "!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = count.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun LovedOneCard(
    lovedOne: LovedOne,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = lovedOne.borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = lovedOne.statusColor.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with status indicator
            Box {
                Text(
                    text = lovedOne.emoji,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = Color.White,
                            shape = CircleShape
                        )
                        .padding(8.dp)
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(lovedOne.statusColor)
                        .align(Alignment.TopEnd)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lovedOne.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2C3E50)
                )
                Text(
                    text = lovedOne.relationship,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(lovedOne.statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lovedOne.statusMessage,
                        fontSize = 13.sp,
                        color = lovedOne.statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CareNavigationBar(
    selectedTab: Int,
    onHomeClick: () -> Unit,
    onChatClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = onHomeClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home"
                )
            },
            label = {
                Text("Home")
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF9333EA),
                selectedTextColor = Color(0xFF9333EA),
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF)
            )
        )
        
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = onChatClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Chat"
                )
            },
            label = {
                Text("Chat")
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF9333EA),
                selectedTextColor = Color(0xFF9333EA),
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF)
            )
        )
        
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = onProfileClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile"
                )
            },
            label = {
                Text("Profile")
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF9333EA),
                selectedTextColor = Color(0xFF9333EA),
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF)
            )
        )
    }
}
