package com.alvin.pulselink.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvin.pulselink.R
import com.alvin.pulselink.presentation.common.theme.RoleThemeProvider
import com.alvin.pulselink.presentation.common.theme.roleColors
import com.alvin.pulselink.presentation.nav.Role

/**
 * 统一的邮箱验证页面
 * 使用角色主题颜色
 *
 * @param email 注册时使用的邮箱地址
 * @param onNavigateToLogin 返回登录页回调
 * @param role 当前角色，默认老人端以保持蓝色主色调
 */
@Composable
fun EmailVerificationScreen(
    email: String,
    onNavigateToLogin: () -> Unit,
    role: String = Role.SENIOR
) {
    RoleThemeProvider(role = role) {
        val colors = roleColors
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundColor)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 邮件图标
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colors.primary,
                                colors.accent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email",
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 标题
            Text(
                text = stringResource(R.string.email_verification_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 说明文字
            Text(
                text = stringResource(R.string.email_verification_instruction),
                fontSize = 16.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 邮箱地址
            Text(
                text = email,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 提示信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.email_verification_check_inbox),
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    HorizontalDivider(color = colors.divider)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = stringResource(R.string.email_verification_spam_note),
                        fontSize = 12.sp,
                        color = colors.textHint,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 返回登录按钮
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.buttonBackground,
                    disabledContainerColor = colors.buttonDisabled
                )
            ) {
                Text(
                    text = stringResource(R.string.email_verification_go_to_login),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.buttonText
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 重新发送链接（可选）
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.email_verification_resend),
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = { /* TODO: 实现重新发送逻辑 */ }
                ) {
                    Text(
                        text = stringResource(R.string.email_verification_resend_button),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
            }
        }
        }
    }
}
