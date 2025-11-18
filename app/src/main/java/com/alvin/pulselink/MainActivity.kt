package com.alvin.pulselink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.alvin.pulselink.presentation.navigation.NavGraph
import com.alvin.pulselink.ui.theme.PulseLinkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseLinkTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}

@Composable
fun HomeScreen() {
    var selectedItem by remember { mutableStateOf(0) }
    
    Scaffold(
        containerColor = BackgroundLight,
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            // Header Section
            HeaderSection()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Feature Cards Grid
            FeatureCardsGrid()
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = stringResource(R.string.greeting_hello),
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
fun FeatureCardsGrid() {
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
                subtitle = stringResource(R.string.health_data_value),
                icon = Icons.Default.Favorite,
                backgroundColor = HealthGreen,
                modifier = Modifier.weight(1f)
            )
            FeatureCard(
                title = stringResource(R.string.reminders_title),
                subtitle = stringResource(R.string.reminders_count),
                icon = Icons.Default.Notifications,
                backgroundColor = ReminderOrange,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Second Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureCard(
                title = stringResource(R.string.bp_history_title),
                subtitle = stringResource(R.string.bp_history_subtitle),
                icon = Icons.Outlined.ShowChart,
                backgroundColor = SmartHomeBlue,
                modifier = Modifier.weight(1f)
            )
            FeatureCard(
                title = stringResource(R.string.smart_device_title),
                subtitle = stringResource(R.string.smart_device_subtitle),
                icon = Icons.Outlined.PhoneAndroid,
                backgroundColor = Color(0xFFAB47BC),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(180.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
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
                    contentDescription = stringResource(R.string.nav_home)
                )
            },
            label = { Text(stringResource(R.string.nav_home)) },
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
                containerColor = SmartHomeBlue,
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
                    contentDescription = stringResource(R.string.nav_profile)
                )
            },
            label = { Text(stringResource(R.string.nav_profile)) },
            selected = selectedItem == 2,
            onClick = { onItemSelected(2) }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    PulseLinkTheme {
        HomeScreen()
    }
}