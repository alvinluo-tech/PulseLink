package com.alvin.pulselink.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.pulselink.R
import com.alvin.pulselink.presentation.nav.Role
import com.alvin.pulselink.ui.theme.SmartHomeBlue

/**
 * 统一的忘记密码页面
 * 根据 role 参数动态改变 UI 配色
 * 
 * @param role 用户角色 ("senior" 或 "caregiver")
 * @param viewModel ForgotPasswordViewModel
 * @param onNavigateBack 返回按钮回调
 * @param onResetSuccess 重置成功回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    role: String,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onResetSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val successMessage = stringResource(R.string.forgot_password_success)
    
    // 根据角色配置 UI
    val uiConfig = remember(role) {
        if (role == Role.SENIOR) {
            ForgotPasswordUIConfig(
                backgroundColor = Color(0xFFE8EDF2),
                iconBackground = SmartHomeBlue,
                buttonColor = SmartHomeBlue.copy(alpha = 0.7f),
                focusBorderColor = SmartHomeBlue.copy(alpha = 0.5f)
            )
        } else {
            ForgotPasswordUIConfig(
                backgroundColor = Color(0xFFF3E8F8),
                iconBackground = Color(0xFFB863E8),
                buttonColor = Color(0xFFB68DD9),
                focusBorderColor = Color(0xFFB863E8).copy(alpha = 0.5f)
            )
        }
    }
    
    // 处理重置成功
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(
                context,
                successMessage,
                Toast.LENGTH_LONG
            ).show()
            onResetSuccess()
        }
    }
    
    // 显示错误 Toast
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMsg ->
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }
    
    ForgotPasswordContent(
        uiState = uiState,
        uiConfig = uiConfig,
        onEmailChange = viewModel::onEmailChanged,
        onSendResetCode = viewModel::sendResetCode,
        onBackClick = onNavigateBack
    )
}

/**
 * UI 配置数据类
 */
private data class ForgotPasswordUIConfig(
    val backgroundColor: Color,
    val iconBackground: Color,
    val buttonColor: Color,
    val focusBorderColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForgotPasswordContent(
    uiState: ForgotPasswordUiState,
    uiConfig: ForgotPasswordUIConfig,
    onEmailChange: (String) -> Unit,
    onSendResetCode: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(uiConfig.backgroundColor)
    ) {
        // 返回按钮
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF2C3E50),
                modifier = Modifier.size(28.dp)
            )
        }
        
        // 主内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 锁图标
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(uiConfig.iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Reset Password",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 标题
            Text(
                text = stringResource(R.string.forgot_password_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 说明文字
            Text(
                text = stringResource(R.string.forgot_password_instruction),
                fontSize = 16.sp,
                color = Color(0xFF5F6F7E),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 邮箱输入框
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.forgot_password_email_label),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.forgot_password_email_hint),
                            color = Color(0xFF9CA3AF)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = uiConfig.focusBorderColor
                    ),
                    singleLine = true,
                    isError = uiState.errorMessage != null
                )
                
                // 错误提示
                uiState.errorMessage?.let { errorMsg ->
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 发送重置链接按钮
            Button(
                onClick = onSendResetCode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = uiConfig.buttonColor
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
                        text = stringResource(R.string.forgot_password_send_button),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 返回登录链接
            TextButton(onClick = onBackClick) {
                Text(
                    text = stringResource(R.string.forgot_password_back_to_login),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = uiConfig.buttonColor
                )
            }
        }
    }
}
