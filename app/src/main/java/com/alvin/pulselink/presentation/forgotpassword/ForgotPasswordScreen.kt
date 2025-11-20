package com.alvin.pulselink.presentation.forgotpassword

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeniorForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(
                context,
                "Reset code sent to your email",
                Toast.LENGTH_LONG
            ).show()
            onNavigateBack()
        }
    }
    
    ForgotPasswordContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChanged,
        onSendResetCode = viewModel::sendResetCode,
        onBackClick = onNavigateBack,
        backgroundColor = Color(0xFFE8EDF2),
        iconBackground = Color(0xFF4A90E2),
        buttonColor = Color(0xFF6AADFF)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(
                context,
                "Reset code sent to your email",
                Toast.LENGTH_LONG
            ).show()
            onNavigateBack()
        }
    }
    
    ForgotPasswordContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChanged,
        onSendResetCode = viewModel::sendResetCode,
        onBackClick = onNavigateBack,
        backgroundColor = Color(0xFFF3E5F5),
        iconBackground = Color(0xFF9C27B0),
        buttonColor = Color(0xFFB863E8)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForgotPasswordContent(
    uiState: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSendResetCode: () -> Unit,
    onBackClick: () -> Unit,
    backgroundColor: Color,
    iconBackground: Color,
    buttonColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Back Button
        IconButton(
            onClick = onBackClick,
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
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Question Mark Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "Help",
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = "Forgot Password?",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle
            Text(
                text = "Enter your email to reset",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Email Label
            Text(
                text = "Email Address",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2C3E50),
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Email Input Field
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    Text(
                        "Enter your email",
                        color = Color.Gray
                    ) 
                },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White.copy(alpha = 0.7f),
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = iconBackground
                ),
                isError = uiState.emailError != null,
                supportingText = {
                    uiState.emailError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Send Reset Code Button
            Button(
                onClick = onSendResetCode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                ),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Send Reset Code",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Back to Login Link
            TextButton(
                onClick = onBackClick
            ) {
                Text(
                    text = "Back to Login",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iconBackground
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Information Text
            Text(
                text = "We'll send a verification code to\nyour email. Use it to reset your\npassword.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            // Error Message
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
