package com.alvin.pulselink.presentation.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.pulselink.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverRegisterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (email: String) -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(uiState.registrationSuccess) {
        if (uiState.registrationSuccess) {
            onRegisterSuccess(uiState.email)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF3E5F5)
                )
            )
        },
        containerColor = Color(0xFFF3E5F5)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // User Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9C27B0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "Caregiver",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = "Caregiver",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            Text(
                text = "Register",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Create your account",
                fontSize = 16.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Registration Form
            RegisterForm(
                uiState = uiState,
                onUsernameChange = viewModel::onUsernameChanged,
                onEmailChange = viewModel::onEmailChanged,
                onPhoneChange = viewModel::onPhoneNumberChanged,
                onPasswordChange = viewModel::onPasswordChanged,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChanged,
                onAgreeToTermsChange = viewModel::onAgreeToTermsChanged,
                onRegister = { viewModel.register(UserRole.CAREGIVER) },
                onNavigateToLogin = onNavigateToLogin,
                buttonColor = Color(0xFF9C27B0)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
