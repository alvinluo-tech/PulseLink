package com.alvin.pulselink.presentation.caregiver.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 子女端设置页面
 * 包含账户安全、修改密码、删除账户等功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareSettingsScreen(
    viewModel: CareSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // 控制折叠面板
    var isPasswordSectionExpanded by remember { mutableStateOf(false) }
    var isDeleteSectionExpanded by remember { mutableStateOf(false) }
    
    // 密码可见性
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // 确认删除对话框
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF8B5CF6)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account & Security 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Account & Security",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2C3E50)
                )
            }
            
            // Change My Password 折叠卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // 标题行（可点击展开/收起）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPasswordSectionExpanded = !isPasswordSectionExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Change My Password",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2C3E50)
                        )
                        Icon(
                            imageVector = if (isPasswordSectionExpanded) 
                                Icons.Default.KeyboardArrowUp 
                            else 
                                Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isPasswordSectionExpanded) "Collapse" else "Expand",
                            tint = Color(0xFF8B5CF6)
                        )
                    }
                    
                    // 展开的内容
                    AnimatedVisibility(
                        visible = isPasswordSectionExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // New Password
                            Text(
                                text = "New Password",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            OutlinedTextField(
                                value = uiState.newPassword,
                                onValueChange = viewModel::onNewPasswordChanged,
                                placeholder = { 
                                    Text(
                                        "Enter new password",
                                        color = Color(0xFFBDBDBD)
                                    ) 
                                },
                                visualTransformation = if (newPasswordVisible) 
                                    VisualTransformation.None 
                                else 
                                    PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                        Icon(
                                            imageVector = if (newPasswordVisible) 
                                                Icons.Default.Visibility 
                                            else 
                                                Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password visibility",
                                            tint = Color(0xFF9CA3AF)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color(0xFFF9FAFB),
                                    focusedContainerColor = Color.White,
                                    unfocusedBorderColor = Color(0xFFE5E7EB),
                                    focusedBorderColor = Color(0xFF8B5CF6)
                                ),
                                isError = uiState.newPasswordError != null
                            )
                            uiState.newPasswordError?.let { error ->
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }
                            
                            // Confirm New Password
                            Text(
                                text = "Confirm New Password",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            OutlinedTextField(
                                value = uiState.confirmPassword,
                                onValueChange = viewModel::onConfirmPasswordChanged,
                                placeholder = { 
                                    Text(
                                        "Confirm new password",
                                        color = Color(0xFFBDBDBD)
                                    ) 
                                },
                                visualTransformation = if (confirmPasswordVisible) 
                                    VisualTransformation.None 
                                else 
                                    PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (confirmPasswordVisible) 
                                                Icons.Default.Visibility 
                                            else 
                                                Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password visibility",
                                            tint = Color(0xFF9CA3AF)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color(0xFFF9FAFB),
                                    focusedContainerColor = Color.White,
                                    unfocusedBorderColor = Color(0xFFE5E7EB),
                                    focusedBorderColor = Color(0xFF8B5CF6)
                                ),
                                isError = uiState.confirmPasswordError != null
                            )
                            uiState.confirmPasswordError?.let { error ->
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }
                            
                            // Change Password Button
                            Button(
                                onClick = { viewModel.changePassword() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF8B5CF6),
                                    disabledContainerColor = Color(0xFFD1D5DB)
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
                                        text = "Change Password",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Delete Account 折叠卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // 标题行（可点击展开/收起）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDeleteSectionExpanded = !isDeleteSectionExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Delete Account",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2C3E50)
                        )
                        Icon(
                            imageVector = if (isDeleteSectionExpanded) 
                                Icons.Default.KeyboardArrowUp 
                            else 
                                Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isDeleteSectionExpanded) "Collapse" else "Expand",
                            tint = Color(0xFF8B5CF6)
                        )
                    }
                    
                    // 展开的内容
                    AnimatedVisibility(
                        visible = isDeleteSectionExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Warning Box
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFEE2E2)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Warning: This action is permanent!",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                    
                                    Text(
                                        text = "Deleting your account will:",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF991B1B)
                                    )
                                    
                                    // Warning list items
                                    WarningListItem("Remove all your personal data")
                                    WarningListItem("Delete all family member connections")
                                    WarningListItem("Erase all health records and reports")
                                    WarningListItem("Cannot be undone or recovered")
                                }
                            }
                            
                            // Delete Button
                            Button(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Delete My Account",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
            
            // Security Tip Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEFF6FF)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "For account security, we recommend using a strong password with at least 8 characters, including uppercase, lowercase, numbers, and special characters.",
                        fontSize = 14.sp,
                        color = Color(0xFF1E40AF),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
    
    // Delete Account Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Account?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you absolutely sure you want to delete your account? This action cannot be undone and all your data will be permanently removed.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount {
                            onAccountDeleted()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    )
                ) {
                    Text("Yes, Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
    
    // Success/Error Snackbar
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            // Show success message
            // You can use SnackbarHostState here if needed
        }
    }
    
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            // Show error message
            // You can use SnackbarHostState here if needed
        }
    }
}

@Composable
private fun WarningListItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = "•",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF991B1B)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color(0xFF991B1B)
        )
    }
}
