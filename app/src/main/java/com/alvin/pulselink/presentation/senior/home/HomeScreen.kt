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
                subtitle = uiState.nextReminderTime,
                icon = Icons.Default.Notifications,
                backgroundColor = ReminderOrange,
                modifier = Modifier.weight(1f),
                onClick = onReminderClick
            )
        }
        
        // Second Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureCard(
                title = stringResource(R.string.health_history_title),
                subtitle = stringResource(R.string.health_history_subtitle),
                icon = Icons.Outlined.Timeline,
                backgroundColor = SmartHomeBlue,
                modifier = Modifier.weight(1f),
                onClick = onHealthHistoryClick
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
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            
            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
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
