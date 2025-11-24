package com.alvin.pulselink.presentation.senior.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.pulselink.R
import com.alvin.pulselink.ui.theme.*
import com.alvin.pulselink.presentation.common.components.SeniorBottomNavigationBar
import com.alvin.pulselink.presentation.common.components.ErrorBanner
import com.alvin.pulselink.presentation.common.components.LoadingScreen

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToHealthData: () -> Unit = {},
    onNavigateToHealthHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAssistant: () -> Unit = {},
    onNavigateToReminder: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedItem by remember { mutableStateOf(0) }
    
    Scaffold(
        containerColor = BackgroundLight,
        bottomBar = {
            SeniorBottomNavigationBar(
                selectedItem = selectedItem,
                onItemSelected = { 
                    selectedItem = it
                    when (it) {
                        1 -> onNavigateToAssistant()
                        2 -> onNavigateToProfile()
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingScreen()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
            ) {
                uiState.error?.let { ErrorBanner(message = it) }
                // Header Section
                HeaderSection(username = uiState.username)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Feature Cards Grid
                FeatureCardsGrid(
                    uiState = uiState,
                    onHealthDataClick = onNavigateToHealthData,
                    onHealthHistoryClick = onNavigateToHealthHistory,
                    onReminderClick = onNavigateToReminder
                )
            }
        }
    }
}

@Composable
fun HeaderSection(username: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = "Hello, $username",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 40.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.greeting_subtitle),
                fontSize = 16.sp,
                color = TextSecondary
            )
        }
        
        // Notification Icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(SmartHomeBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun FeatureCardsGrid(
    uiState: HomeUiState,
    onHealthDataClick: () -> Unit = {},
    onHealthHistoryClick: () -> Unit = {},
    onReminderClick: () -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // First Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureCard(
                title = stringResource(R.string.health_data_title),
                subtitle = uiState.healthData?.let { "BP ${it.systolic}/${it.diastolic}" }
                    ?: stringResource(R.string.health_data_value),
                icon = Icons.Default.Favorite,
                backgroundColor = HealthGreen,
                modifier = Modifier.weight(1f),
                onClick = onHealthDataClick
            )
            FeatureCard(
                title = stringResource(R.string.reminders_title),
                subtitle = if (uiState.nextReminderTime == "No reminders") "All clear!" else uiState.nextReminderTime,
                icon = Icons.Default.Notifications,
                backgroundColor = ReminderOrange,
                modifier = Modifier.weight(1f),
                onClick = onReminderClick,
                isEmpty = uiState.nextReminderTime == "No reminders"
            )
        }
        
        // Second Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureCard(
                title = stringResource(R.string.health_history_title),
                subtitle = if (uiState.healthData == null) "No records yet" else stringResource(R.string.health_history_subtitle),
                icon = Icons.Outlined.Timeline,
                backgroundColor = SmartHomeBlue,
                modifier = Modifier.weight(1f),
                onClick = onHealthHistoryClick,
                isEmpty = uiState.healthData == null
            )
            FeatureCard(
                title = stringResource(R.string.smart_device_title),
                subtitle = stringResource(R.string.smart_device_subtitle),
                icon = Icons.Outlined.Smartphone,
                backgroundColor = Color(0xFFAB47BC),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    isEmpty: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.height(180.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = if (isEmpty) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.25f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(if (isEmpty) 28.dp else 32.dp)
                    )
                }
                
                // Show empty state indicator with pulsing animation
                if (isEmpty) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.White, CircleShape)
                            )
                            Text(
                                text = "Empty",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                if (isEmpty) {
                    // Empty state with icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = subtitle,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                } else {
                    Text(
                        text = subtitle,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// BottomNavigationBar moved to shared component: SeniorBottomNavigationBar

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    PulseLinkTheme {
        HomeScreen()
    }
}
