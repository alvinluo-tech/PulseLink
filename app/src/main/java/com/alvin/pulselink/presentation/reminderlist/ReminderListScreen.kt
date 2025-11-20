package com.alvin.pulselink.presentation.reminderlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: ReminderListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedItem by remember { mutableStateOf(2) } // Profile selected
    
    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedItem,
                onItemSelected = { 
                    selectedItem = it
                    when (it) {
                        0 -> onNavigateToHome()
                        1 -> { /* Voice assistant */ }
                        2 -> onNavigateToProfile()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4A90E2))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = "Today's",
                                fontSize = 24.sp,
                                color = Color(0xFF2C3E50)
                            )
                            Text(
                                text = "Reminders",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C3E50)
                            )
                            Text(
                                text = formatDate(uiState.date),
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Status Summary Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatusCard(
                            count = uiState.takenCount,
                            label = "Taken",
                            icon = Icons.Default.Check,
                            backgroundColor = Color(0xFFE8F5E9),
                            iconColor = Color(0xFF4CAF50),
                            textColor = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        
                        StatusCard(
                            count = uiState.pendingCount,
                            label = "Pending",
                            icon = Icons.Default.Schedule,
                            backgroundColor = Color(0xFFE3F2FD),
                            iconColor = Color(0xFF2196F3),
                            textColor = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f)
                        )
                        
                        StatusCard(
                            count = uiState.missedCount,
                            label = "Missed",
                            icon = Icons.Default.Close,
                            backgroundColor = Color(0xFFFFEBEE),
                            iconColor = Color(0xFFE53935),
                            textColor = Color(0xFFE53935),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Reminders List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.reminders) { reminder ->
                    ReminderItemCard(reminder = reminder)
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    count: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = textColor
            )
        }
    }
}

@Composable
fun ReminderItemCard(reminder: ReminderItem) {
    val (backgroundColor, borderColor, statusColor, statusText, statusIcon) = when (reminder.status) {
        ReminderStatus.TAKEN -> ReminderCardColors(
            background = Color(0xFFE8F5E9),
            border = Color(0xFF4CAF50),
            statusColor = Color(0xFF4CAF50),
            statusText = "Taken",
            icon = Icons.Default.Check
        )
        ReminderStatus.PENDING -> ReminderCardColors(
            background = Color(0xFFE3F2FD),
            border = Color(0xFF2196F3),
            statusColor = Color(0xFF2196F3),
            statusText = "Pending",
            icon = Icons.Default.Schedule
        )
        ReminderStatus.MISSED -> ReminderCardColors(
            background = Color(0xFFFFEBEE),
            border = Color(0xFFE53935),
            statusColor = Color(0xFFE53935),
            statusText = "Missed",
            icon = Icons.Default.Close
        )
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = reminder.time,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reminder.medicationName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2C3E50)
                        )
                    }
                    
                    Text(
                        text = reminder.dosage,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Text(
                text = statusText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
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
            selected = selectedItem == 0,
            onClick = { onItemSelected(0) }
        )
        
        // Center Microphone Button
        Box(
            modifier = Modifier
                .size(72.dp)
                .offset(y = (-16).dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = { onItemSelected(1) },
                containerColor = Color(0xFF4A90E2),
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
            selected = selectedItem == 2,
            onClick = { onItemSelected(2) }
        )
    }
}

private data class ReminderCardColors(
    val background: Color,
    val border: Color,
    val statusColor: Color,
    val statusText: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun formatDate(date: java.time.LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)
    return date.format(formatter)
}
