package com.alvin.pulselink.presentation.verification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SeniorEmailVerificationScreen(
    email: String,
    onNavigateBack: () -> Unit = {},
    onGotEmail: () -> Unit = {}
) {
    EmailVerificationContent(
        email = email,
        onNavigateBack = onNavigateBack,
        onGotEmail = onGotEmail,
        backgroundColor = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE3E8F0),
                Color(0xFFD6DDEB)
            )
        ),
        iconColor = Color(0xFF4A90E2),
        buttonColor = Color(0xFF4A90E2),
        warningBackground = Color(0xFFFFF4E6)
    )
}

@Composable
fun CaregiverEmailVerificationScreen(
    email: String,
    onNavigateBack: () -> Unit = {},
    onGotEmail: () -> Unit = {}
) {
    EmailVerificationContent(
        email = email,
        onNavigateBack = onNavigateBack,
        onGotEmail = onGotEmail,
        backgroundColor = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF3E5F5),
                Color(0xFFE1BEE7)
            )
        ),
        iconColor = Color(0xFFB863E8),
        buttonColor = Color(0xFFB863E8),
        warningBackground = Color(0xFFFFF4E6)
    )
}

@Composable
private fun EmailVerificationContent(
    email: String,
    onNavigateBack: () -> Unit,
    onGotEmail: () -> Unit,
    backgroundColor: Brush,
    iconColor: Color,
    buttonColor: Color,
    warningBackground: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Back Button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF2C3E50),
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Email Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(iconColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email",
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Title
            Text(
                text = "Verify your\nemail",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center,
                lineHeight = 44.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            Text(
                text = "A verification email has\nbeen sent to",
                fontSize = 18.sp,
                color = Color(0xFF5F6F7E),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Email Address
            Text(
                text = email,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = iconColor,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Please check your inbox\nand click the verification\nlink to activate your\naccount.",
                        fontSize = 16.sp,
                        color = Color(0xFF2C3E50),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "After verifying your\nemail, you can sign in.",
                        fontSize = 16.sp,
                        color = Color(0xFF5F6F7E),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Got Email Button
            Button(
                onClick = onGotEmail,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                )
            ) {
                Text(
                    text = "I've Got the Email",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Warning Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = warningBackground
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFFFA726),
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = "Didn't receive the email?\nCheck your spam folder or\ncontact support.",
                        fontSize = 14.sp,
                        color = Color(0xFF5F6F7E),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
