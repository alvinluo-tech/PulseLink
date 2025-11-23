package com.alvin.pulselink.presentation.caregiver.senior

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.pulselink.domain.model.LinkRequest
import com.alvin.pulselink.util.AvatarHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Link History Screen - 显示所有 link 请求历史记录
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkHistoryScreen(
    viewModel: LinkSeniorViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Load history when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadLinkHistory()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Link Request History", 
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoadingHistory -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF8B5CF6)
                    )
                }
                uiState.linkHistory.isEmpty() -> {
                    EmptyHistoryState()
                }
                else -> {
                    HistoryList(uiState.linkHistory)
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFFDEEDFF), RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.History,
                null,
                Modifier.size(60.dp),
                tint = Color(0xFF2563EB)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "No Link Requests Yet",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Your link request history will appear here",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HistoryList(history: List<LinkHistoryItem>) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Group by status
        val pending = history.filter { it.request.status == "pending" }
        val approved = history.filter { it.request.status == "approved" }
        val rejected = history.filter { it.request.status == "rejected" }
        
        // Pending section
        if (pending.isNotEmpty()) {
            item {
                SectionHeader("Pending (${pending.size})", Color(0xFFFBBF24))
            }
            items(pending) { item ->
                HistoryCard(item)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        
        // Approved section
        if (approved.isNotEmpty()) {
            item {
                SectionHeader("Approved (${approved.size})", Color(0xFF22C55E))
            }
            items(approved) { item ->
                HistoryCard(item)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        
        // Rejected section
        if (rejected.isNotEmpty()) {
            item {
                SectionHeader("Rejected (${rejected.size})", Color(0xFFEF4444))
            }
            items(rejected) { item ->
                HistoryCard(item)
            }
        }
        
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 20.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
    }
}

@Composable
private fun HistoryCard(item: LinkHistoryItem) {
    val request = item.request
    val statusColor = when (request.status) {
        "pending" -> Color(0xFFFBBF24)
        "approved" -> Color(0xFF22C55E)
        "rejected" -> Color(0xFFEF4444)
        else -> Color(0xFF9CA3AF)
    }
    
    val statusBgColor = when (request.status) {
        "pending" -> Color(0xFFFFFBEB)
        "approved" -> Color(0xFFDCFCE7)
        "rejected" -> Color(0xFFFEE2E2)
        else -> Color(0xFFF3F4F6)
    }
    
    val statusIcon = when (request.status) {
        "pending" -> Icons.Default.Schedule
        "approved" -> Icons.Default.CheckCircle
        "rejected" -> Icons.Default.Cancel
        else -> Icons.Default.Info
    }
    
    // Get avatar based on senior info
    val avatarIcon = if (item.seniorName.isNotEmpty()) {
        AvatarHelper.getAvatarIcon(item.seniorAvatarType)
    } else {
        Icons.Default.Person
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(statusBgColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = avatarIcon,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(28.dp),
                            tint = statusColor
                        )
                    }
                    
                    Column {
                        Text(
                            text = item.seniorName.ifEmpty { "Loading..." },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = request.seniorId,
                            fontSize = 12.sp,
                            color = Color(0xFF7C3AED)
                        )
                        Text(
                            text = "As ${request.relationship}",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                
                // Status badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Text(
                            request.status.replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Nickname if exists
            if (request.nickname.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF8B5CF6)
                    )
                    Text(
                        "Nickname: ${request.nickname}",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
            
            // Message if exists
            if (request.message.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF9FAFB)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Message:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF9CA3AF)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            request.message,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
            
            // Timestamps
            Spacer(Modifier.height(12.dp))
            Divider(color = Color(0xFFE5E7EB))
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimeInfo("Requested", request.createdAt)
                if (request.status != "pending") {
                    TimeInfo("Updated", request.updatedAt)
                }
            }
        }
    }
}

@Composable
private fun TimeInfo(label: String, timestamp: Long) {
    Column {
        Text(
            label,
            fontSize = 11.sp,
            color = Color(0xFF9CA3AF)
        )
        Text(
            formatTimestamp(timestamp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF64748B)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

/**
 * Link history item with enriched senior info
 */
data class LinkHistoryItem(
    val request: LinkRequest,
    val seniorName: String = "",
    val seniorAvatarType: String = ""
)
