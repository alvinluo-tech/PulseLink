package com.alvin.pulselink.presentation.caregiver.linkguard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Family Requests Screen (Link Guard)
 * Shows pending link requests from other caregivers wanting to link to seniors I created
 * The request is sent to the caregiver who created the senior account, not to the senior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyRequestsScreen(
    viewModel: LinkRequestsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load pending requests
    LaunchedEffect(Unit) {
        viewModel.loadPendingRequests()
    }
    
    // Listen for success messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }
    
    // Listen for errors
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Caregiver Requests",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Approve or reject link requests",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF9333EA),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF8B5CF6)
                    )
                }
                uiState.pendingRequests.isEmpty() -> {
                    EmptyRequestsState()
                }
                else -> {
                    RequestsList(
                        requests = uiState.pendingRequests,
                        onApprove = { request -> viewModel.approveRequest(request) },
                        onReject = { request -> viewModel.rejectRequest(request) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRequestsState() {
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
                .background(Color(0xFFEFF6FF), RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                Modifier.size(60.dp),
                tint = Color(0xFF3B82F6)
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
            "You're all caught up! No link requests to review.",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun RequestsList(
    requests: List<LinkRequestDisplay>,
    onApprove: (LinkRequestDisplay) -> Unit,
    onReject: (LinkRequestDisplay) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests) { request ->
            RequestCard(
                request = request,
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
private fun RequestCard(
    request: LinkRequestDisplay,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
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
            // Requester header (Dr. Wang, email)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.requesterName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Text(
                        text = request.requesterEmail,
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }
                
                // Time indicator icon
                Icon(
                    Icons.Default.AccessTime,
                    null,
                    Modifier.size(24.dp),
                    tint = Color(0xFFF59E0B)
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Requesting to link with section (purple background)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(Color(0xFFF3E8FF))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Requesting to link with:",
                        fontSize = 14.sp,
                        color = Color(0xFF7C3AED),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = request.seniorName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B21A8)
                    )
                    Text(
                        text = "(${request.relationship})",
                        fontSize = 16.sp,
                        color = Color(0xFF7C3AED)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Message section
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
            
            // Requested date
            Text(
                text = request.requestedDate,
                fontSize = 13.sp,
                color = Color(0xFF9CA3AF)
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Warning banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(Color(0xFFFEF3C7)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
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
                        tint = Color(0xFFD97706)
                    )
                    Text(
                        text = "Only approve if you know this person",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF92400E)
                    )
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
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
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
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
