package com.alvin.pulselink.presentation.senior.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthHistoryScreen(
    viewModel: HealthHistoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateAssistant: () -> Unit,
    onNavigateToHealthReport: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Health History",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
                    containerColor = Color(0xFF448AFF)
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                onHomeClick = onNavigateHome,
                onProfileClick = onNavigateProfile,
                onMicClick = onNavigateAssistant
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F6FB))
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF448AFF))
                }
            } else if (uiState.records.isEmpty()) {
                EmptyHealthHistoryState(onNavigateToHealthReport = onNavigateToHealthReport)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.records) { record ->
                        HealthRecordCard(record = record)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHealthHistoryState(
    onNavigateToHealthReport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon background
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = Color(0xFFE3F2FD),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "No health data",
                tint = Color(0xFF448AFF),
                modifier = Modifier.size(60.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "No Health Records Yet",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Start recording your blood pressure and heart rate to track your health journey",
            fontSize = 16.sp,
            color = Color(0xFF6B7280),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onNavigateToHealthReport,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF448AFF)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Record Health Data",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HealthRecordCard(record: HealthRecord) {
    val statusColor = when (record.status) {
        HealthStatus.HIGH -> Color(0xFFFFE4E1)
        HealthStatus.NORMAL -> Color(0xFFE8F5E9)
        HealthStatus.LOW -> Color(0xFFFFF9E6)
    }
    
    val statusTextColor = when (record.status) {
        HealthStatus.HIGH -> Color(0xFFE53935)
        HealthStatus.NORMAL -> Color(0xFF4CAF50)
        HealthStatus.LOW -> Color(0xFFFFA726)
    }
    
    val statusBadgeColor = when (record.status) {
        HealthStatus.HIGH -> Color(0xFFFFCDD2)
        HealthStatus.NORMAL -> Color(0xFFC8E6C9)
        HealthStatus.LOW -> Color(0xFFFFE0B2)
    }
    
    val statusText = when (record.status) {
        HealthStatus.HIGH -> "High"
        HealthStatus.NORMAL -> "Normal"
        HealthStatus.LOW -> "Low"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = record.date,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = record.time,
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBadgeColor
                ) {
                    Text(
                        text = statusText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusTextColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "High",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "${record.systolic}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }
                
                Text(
                    text = "/",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Column {
                    Text(
                        text = "Low",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "${record.diastolic}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Heart Rate",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Heart Rate",
                        fontSize = 16.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${record.heartRate}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "bpm",
                        fontSize = 16.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFC8E6C9)
                ) {
                    Text(
                        text = "Normal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onMicClick: () -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") },
            selected = false,
            onClick = onHomeClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF448AFF),
                selectedTextColor = Color(0xFF448AFF),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color(0xFFE3F2FD)
            )
        )
        
        Box(
            modifier = Modifier
                .size(72.dp)
                .offset(y = (-16).dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = onMicClick,
                containerColor = Color(0xFF448AFF),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Assistant",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile"
                )
            },
            label = { Text("Profile") },
            selected = false,
            onClick = onProfileClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF448AFF),
                selectedTextColor = Color(0xFF448AFF),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color(0xFFE3F2FD)
            )
        )
    }
}
