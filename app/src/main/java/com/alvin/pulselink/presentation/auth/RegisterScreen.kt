package com.alvin.pulselink.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.pulselink.R
import com.alvin.pulselink.domain.model.UserRole
import com.alvin.pulselink.presentation.nav.Role
import com.alvin.pulselink.presentation.common.theme.RoleThemeProvider
import com.alvin.pulselink.presentation.common.theme.roleColors

/**
 * 统一的注册界面
 * 根据 role 参数动态改变 UI 配色
 * 
 * @param role 用户角色 ("senior" 或 "caregiver")
 * @param viewModel 认证 ViewModel
 * @param onNavigateBack 返回按钮回调
 * @param onNavigateToLogin 跳转登录页回调
 * @param onRegisterSuccess 注册成功回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    role: String,
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (email: String) -> Unit
) {
    RoleThemeProvider(role = role) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val colors = roleColors
        
        // 确定用户角色枚举
        val userRole = if (role == Role.SENIOR) UserRole.SENIOR else UserRole.CAREGIVER
        
        // 获取标题资源 ID
        val titleResId = if (role == Role.SENIOR) {
            R.string.senior_register_title
        } else {
            R.string.caregiver_register_title
        }
        
        // 注册成功后导航
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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.backgroundColor
                    )
                )
            },
            containerColor = colors.backgroundColor
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
                
                // 用户图标
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(colors.iconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        tint = colors.iconTint,
                        modifier = Modifier.size(50.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 标题
                Text(
                    text = stringResource(titleResId),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.register_create_account),
                fontSize = 16.sp,
                color = colors.textSecondary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 注册表单
            RegisterForm(
                uiState = uiState,
                onUsernameChange = viewModel::onUsernameChange,
                onEmailChange = viewModel::onEmailChange,
                onPhoneNumberChange = viewModel::onPhoneNumberChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onTermsAgreementChange = viewModel::onTermsAgreementChange,
                onRegister = { viewModel.register(userRole) },
                onNavigateToLogin = onNavigateToLogin,
                buttonColor = colors.buttonBackground,
                isLoading = uiState.isLoading
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    }
}
