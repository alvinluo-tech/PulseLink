package com.alvin.pulselink.presentation.caregiver.senior

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * 二维码展示对话框
 * 
 * @param seniorId 老人账户ID（例如：SNR-ABCD1234）
 * @param password 老人账户密码
 * @param qrCodeBitmap 二维码图片
 * @param onDismiss 关闭对话框回调
 * @param onShare 分享按钮回调（可选，用于分享图片到其他应用）
 */
@Composable
fun QRCodeDialog(
    seniorId: String,
    password: String,
    qrCodeBitmap: Bitmap?,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "老人账户登录信息",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 二维码图片
                if (qrCodeBitmap != null) {
                    Card(
                        modifier = Modifier
                            .size(280.dp)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrCodeBitmap.asImageBitmap(),
                                contentDescription = "登录二维码",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    // 二维码生成失败的提示
                    Card(
                        modifier = Modifier
                            .size(280.dp)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "二维码生成失败",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 账户信息
                AccountInfoItem(
                    label = "账号ID",
                    value = seniorId,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(seniorId))
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                AccountInfoItem(
                    label = "密码",
                    value = password,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(password))
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 提示信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📱 使用说明",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. 老人端打开应用，选择「扫码登录」\n" +
                                   "2. 扫描此二维码即可自动登录\n" +
                                   "3. 或手动输入账号ID和密码登录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 按钮组
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 分享按钮（如果提供了回调）
                    if (onShare != null) {
                        OutlinedButton(
                            onClick = onShare,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("分享")
                        }
                    }
                    
                    // 关闭按钮
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

/**
 * 账户信息条目（带复制功能）
 */
@Composable
private fun AccountInfoItem(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            IconButton(onClick = onCopy) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "复制$label",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
