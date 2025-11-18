package com.alvin.pulselink.presentation.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvin.pulselink.R
import com.alvin.pulselink.ui.theme.PulseLinkTheme

@Composable
fun WelcomeScreen(
    onNavigateToSeniorLogin: () -> Unit = {},
    onNavigateToCaregiverLogin: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3E8F0),
                        Color(0xFFD6DDEB)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Logo and Title Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Heart Icon with Gradient
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF6B8AFF),
                                    Color(0xFF9D6BFF)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Health Assistant Logo",
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }
                
                // Welcome Text
                Text(
                    text = stringResource(R.string.welcome_title),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                )
                
                // Subtitle
                Text(
                    text = stringResource(R.string.welcome_subtitle),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF5F6F7E),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Role Selection Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.welcome_role_prompt),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF5F6F7E),
                    textAlign = TextAlign.Center
                )
                
                // Senior Button
                RoleCard(
                    title = stringResource(R.string.role_senior_title),
                    subtitle = stringResource(R.string.role_senior_subtitle),
                    icon = Icons.Default.Person,
                    backgroundColor = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4A90E2),
                            Color(0xFF357ABD)
                        )
                    ),
                    onClick = onNavigateToSeniorLogin
                )
                
                // Caregiver Button
                RoleCard(
                    title = stringResource(R.string.role_caregiver_title),
                    subtitle = stringResource(R.string.role_caregiver_subtitle),
                    icon = Icons.Default.Favorite,
                    backgroundColor = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFB863E8),
                            Color(0xFF9B4FD8)
                        )
                    ),
                    onClick = onNavigateToCaregiverLogin
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bottom hint text
            Text(
                text = stringResource(R.string.welcome_choose_hint),
                fontSize = 16.sp,
                color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Brush,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Icon Circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Text Content
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WelcomeScreenPreview() {
    PulseLinkTheme {
        WelcomeScreen()
    }
}
