package com.alvin.pulselink.presentation.caregiver.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.pulselink.presentation.caregiver.dashboard.CareDashboardViewModel
import com.alvin.pulselink.presentation.caregiver.dashboard.LovedOne
import com.alvin.pulselink.presentation.caregiver.dashboard.CareNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareChatScreen(
    viewModel: CareDashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLovedOneClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        bottomBar = {
            CareNavigationBar(
                selectedTab = 1,
                onHomeClick = onNavigateToHome,
                onChatClick = { },
                onProfileClick = onNavigateToProfile
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(paddingValues)
        ) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF9333EA)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Care Chat",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Text(
                        text = "Select who to consult about",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Instruction text
            Text(
                text = "Choose a loved one to get their latest\nhealth updates",
                fontSize = 16.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Loved ones list
            if (uiState.lovedOnes.isEmpty()) {
                // Empty state
                EmptyChatState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.lovedOnes) { lovedOne ->
                        ChatLovedOneCard(
                            lovedOne = lovedOne,
                            onClick = { onLovedOneClick(lovedOne.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Chat,
            contentDescription = null,
            tint = Color(0xFFD1D5DB),
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Conversations Yet",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7280)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Add loved ones to start conversations\nand check their health updates",
            fontSize = 15.sp,
            color = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            color = Color(0xFFF3E8FF),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    tint = Color(0xFF9333EA),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Create or link a senior account first",
                    fontSize = 14.sp,
                    color = Color(0xFF7C3AED),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ChatLovedOneCard(
    lovedOne: LovedOne,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = lovedOne.borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = lovedOne.statusColor.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Text(
                text = lovedOne.emoji,
                fontSize = 48.sp,
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = Color.White,
                        shape = CircleShape
                    )
                    .padding(8.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lovedOne.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2C3E50)
                )
                Text(
                    text = lovedOne.relationship,
                    fontSize = 15.sp,
                    color = Color(0xFF6B7280)
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "Chat",
                tint = Color(0xFF9333EA),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
