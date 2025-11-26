package com.alvin.pulselink.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.pulselink.R
import com.alvin.pulselink.domain.model.UserRole
import com.alvin.pulselink.presentation.nav.Role
import com.alvin.pulselink.presentation.common.theme.LocalRoleColorScheme
import com.alvin.pulselink.presentation.common.theme.RoleThemeProvider
import com.alvin.pulselink.presentation.common.theme.roleColors
import com.alvin.pulselink.util.LoginHistoryManager

/**
 * 统一的登录界面
 * 根据 role 参数动态改变 UI 配色和文案
 * 
 * @param role 用户角色 ("senior" 或 "caregiver")
 * @param viewModel 认证 ViewModel
 * @param onNavigateToHome 登录成功后的导航回调
 * @param onNavigateBack 返回按钮回调
 * @param onNavigateToRegister 跳转注册页回调
 * @param onNavigateToForgotPassword 跳转忘记密码页回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    role: String,
    viewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRegister: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {}
) {
    // 使用 RoleThemeProvider 包裹整个界面
    RoleThemeProvider(role = role) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        var showQRScanner by remember { mutableStateOf(false) }
        
        // 确定用户角色枚举
        val userRole = if (role == Role.SENIOR) UserRole.SENIOR else UserRole.CAREGIVER
        
        // 获取当前角色的标题资源 ID
        val titleResId = if (role == Role.SENIOR) {
            R.string.senior_login_title
        } else {
            R.string.caregiver_login_title
        }
        
        // 处理登录成功导航
        LaunchedEffect(uiState.isSuccess) {
            if (uiState.isSuccess) {
                // 保存登录历史（账号+密码）
                if (userRole == UserRole.SENIOR) {
                    LoginHistoryManager.saveVirtualIdCredential(context, uiState.email, uiState.password)
                } else {
                    LoginHistoryManager.saveEmailCredential(context, uiState.email, uiState.password)
                }
                onNavigateToHome()
            }
        }
        
        // 显示错误 Toast
        LaunchedEffect(uiState.error) {
            uiState.error?.let { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
        
        LoginScreenContent(
            uiState = uiState,
            titleResId = titleResId,
            userRole = userRole,
            context = context,
            onVirtualIdChange = viewModel::onVirtualIdChange,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onTermsAgreementChange = viewModel::onTermsAgreementChange,
            onScanQRCode = { showQRScanner = true },
            onLoginClick = {
                // ⭐ 所有角色统一使用 login() 方法，支持邮箱和SNR-ID自动识别
                viewModel.login(userRole)
            },
            onBackClick = onNavigateBack,
            onRegisterClick = onNavigateToRegister,
            onForgotPasswordClick = onNavigateToForgotPassword,
            onResendVerification = viewModel::resendVerificationEmail
        )
        
        // 二维码扫描对话框
        if (showQRScanner) {
            QRCodeScannerDialog(
                onDismiss = { showQRScanner = false },
                onQRCodeScanned = { qrCode ->
                    viewModel.parseQRCodeAndLogin(qrCode)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreenContent(
    uiState: AuthUiState,
    titleResId: Int,
    userRole: UserRole,
    context: android.content.Context,
    onVirtualIdChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTermsAgreementChange: (Boolean) -> Unit,
    onScanQRCode: () -> Unit,
    onLoginClick: () -> Unit,
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onResendVerification: () -> Unit
) {
    // 从主题获取颜色
    val colors = roleColors
    
    // 加载历史记录
    val loginHistory: List<String> = remember(userRole) {
        if (userRole == UserRole.SENIOR) {
            LoginHistoryManager.getVirtualIdHistory(context)
        } else {
            LoginHistoryManager.getEmailHistory(context)
        }
    }
    
    // 状态：下拉菜单展开
    var expanded by remember { mutableStateOf(false) }
    
    // 过滤后的建议列表
    val filteredSuggestions: List<String> = remember(uiState.email, loginHistory) {
        if (uiState.email.isBlank()) {
            loginHistory
        } else {
            loginHistory.filter { it.contains(uiState.email, ignoreCase = true) }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundColor)
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
                tint = colors.textPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        
        // 主内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                    contentDescription = "User Icon",
                    tint = colors.iconTint,
                    modifier = Modifier.size(50.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 标题
            Text(
                text = stringResource(titleResId),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 副标题
            Text(
                text = stringResource(R.string.login_welcome_back),
                fontSize = 18.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // 老人端和子女端都使用邮箱密码登录
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (userRole == UserRole.SENIOR) "账号ID" else stringResource(R.string.login_username_label),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // 带历史记录下拉菜单的输入框
                ExposedDropdownMenuBox(
                    expanded = expanded && filteredSuggestions.isNotEmpty(),
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.email,  // ⭐ 统一使用 email 字段（可以是邮箱或 SNR-ID）
                        onValueChange = { newValue ->
                            onEmailChange(newValue)
                            expanded = true
                            
                            // 自动填充对应的密码
                            val password = if (userRole == UserRole.SENIOR) {
                                LoginHistoryManager.getPasswordForVirtualId(context, newValue)
                            } else {
                                LoginHistoryManager.getPasswordForEmail(context, newValue)
                            }
                            password?.let { onPasswordChange(it) }
                        },
                        placeholder = {
                            Text(
                                text = if (userRole == UserRole.SENIOR) 
                                    stringResource(R.string.senior_login_username_hint)  // ⭐ 老人端显示新提示
                                else 
                                    stringResource(R.string.login_username_hint),
                                color = colors.textHint,
                                fontSize = 14.sp  // ⭐ 减小placeholder字体大小
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email  // 使用邮箱键盘
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .heightIn(min = 70.dp),  // ⭐ 使用heightIn替代固定height，确保内容完整显示
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = colors.inputBackground.copy(alpha = 0.8f),
                            focusedContainerColor = colors.inputBackground,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = colors.inputFocusBorder,
                            unfocusedTextColor = colors.inputText,
                            focusedTextColor = colors.inputText
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),  // ⭐ 设置输入文字大小
                        singleLine = true,
                        isError = uiState.emailError != null,  // ⭐ 显示错误
                        supportingText = {
                            uiState.emailError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    
                    // 下拉菜单显示历史记录
                    ExposedDropdownMenu(
                        expanded = expanded && filteredSuggestions.isNotEmpty(),
                        onDismissRequest = { expanded = false }
                    ) {
                        filteredSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    onEmailChange(suggestion)
                                    expanded = false
                                    
                                    // 自动填充对应的密码
                                    val password = if (userRole == UserRole.SENIOR) {
                                        LoginHistoryManager.getPasswordForVirtualId(context, suggestion)
                                    } else {
                                        LoginHistoryManager.getPasswordForEmail(context, suggestion)
                                    }
                                    password?.let { onPasswordChange(it) }
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.login_password_label),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    placeholder = {
                        Text(
                            text = if (userRole == UserRole.SENIOR) "请输入密码" else stringResource(R.string.login_password_hint),
                            color = colors.textHint,
                            fontSize = 14.sp  // ⭐ 减小placeholder字体大小
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp),  // ⭐ 使用heightIn替代固定height
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = colors.inputBackground.copy(alpha = 0.8f),
                        focusedContainerColor = colors.inputBackground,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = colors.inputFocusBorder,
                        unfocusedTextColor = colors.inputText,
                        focusedTextColor = colors.inputText
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),  // ⭐ 设置输入文字大小
                    singleLine = true
                )
            }
            
            // 老人端：显示扫码登录提示
            if (userRole == UserRole.SENIOR) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = colors.textHint.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "或",
                        fontSize = 14.sp,
                        color = colors.textHint,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = colors.textHint.copy(alpha = 0.3f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 扫码登录按钮
                OutlinedButton(
                    onClick = onScanQRCode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.primary
                    ),
                    border = BorderStroke(2.dp, colors.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,  // TODO: 使用二维码图标
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "扫码登录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // 忘记密码和条款（所有角色都显示）
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onForgotPasswordClick) {
                    Text(
                        text = "Forgot Password?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.primary
                    )
                }
            }
            
            if (userRole == UserRole.CAREGIVER) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.agreedToTerms,
                        onCheckedChange = onTermsAgreementChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.primary,
                            uncheckedColor = colors.textHint
                        )
                    )
                    Text(
                        text = stringResource(R.string.login_terms_agreement),
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 登录按钮
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.buttonBackground,
                    disabledContainerColor = colors.buttonDisabled
                ),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = colors.buttonText
                    )
                } else {
                    Text(
                        text = stringResource(R.string.login_button),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.buttonText
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 重新发送验证邮件按钮（仅子女端显示且需要时）
            if (userRole == UserRole.CAREGIVER && uiState.showResendVerification) {
                OutlinedButton(
                    onClick = onResendVerification,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.primary
                    ),
                    border = BorderStroke(2.dp, colors.primary),
                    enabled = !uiState.isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Resend Verification Email",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 注册链接（所有角色都显示）
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.login_no_account),
                    fontSize = 15.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onRegisterClick) {
                    Text(
                        text = stringResource(R.string.login_register_link),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}
