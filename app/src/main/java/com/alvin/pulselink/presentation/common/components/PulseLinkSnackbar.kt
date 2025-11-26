package com.alvin.pulselink.presentation.common.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvin.pulselink.presentation.common.state.SnackbarType

/**
 * 自定义胶囊式 Snackbar
 * 
 * 设计特点：
 * - 圆角卡片设计，带阴影
 * - 彩色背景和图标，清晰的视觉区分
 * - 大字体，适老化设计
 * - 左侧图标 + 右侧文字布局
 * 
 * 适用场景：
 * - Caregiver 的所有操作反馈
 * - Senior 的轻量级提示
 */
@Composable
fun PulseLinkSnackbar(
    snackbarData: SnackbarData,
    type: SnackbarType = SnackbarType.INFO,
    modifier: Modifier = Modifier
) {
    val colors = getSnackbarColors(type)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 左侧图标
            Icon(
                imageVector = getSnackbarIcon(type),
                contentDescription = null,
                tint = colors.iconColor,
                modifier = Modifier.size(32.dp)
            )
            
            // 中间消息文字
            Text(
                text = snackbarData.visuals.message,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textColor
                ),
                modifier = Modifier.weight(1f)
            )
            
            // 右侧操作按钮（如果有）
            snackbarData.visuals.actionLabel?.let { actionLabel ->
                TextButton(
                    onClick = { snackbarData.performAction() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.iconColor
                    )
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // 关闭按钮
            if (snackbarData.visuals.withDismissAction) {
                IconButton(
                    onClick = { snackbarData.dismiss() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "关闭",
                        tint = colors.textColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 获取 Snackbar 颜色方案
 */
private data class SnackbarColors(
    val backgroundColor: Color,
    val iconColor: Color,
    val textColor: Color
)

@Composable
private fun getSnackbarColors(type: SnackbarType): SnackbarColors {
    return when (type) {
        SnackbarType.SUCCESS -> SnackbarColors(
            backgroundColor = Color(0xFFE8F5E9), // 极淡绿
            iconColor = Color(0xFF2E7D32),        // 深绿
            textColor = Color(0xFF1B5E20)         // 更深绿
        )
        SnackbarType.ERROR -> SnackbarColors(
            backgroundColor = Color(0xFFFFEBEE), // 极淡红
            iconColor = Color(0xFFC62828),        // 深红
            textColor = Color(0xFFB71C1C)         // 更深红
        )
        SnackbarType.WARNING -> SnackbarColors(
            backgroundColor = Color(0xFFFFF3E0), // 极淡橙
            iconColor = Color(0xFFF57C00),        // 深橙
            textColor = Color(0xFFE65100)         // 更深橙
        )
        SnackbarType.INFO -> SnackbarColors(
            backgroundColor = Color(0xFFE3F2FD), // 极淡蓝
            iconColor = Color(0xFF1976D2),        // 深蓝
            textColor = Color(0xFF0D47A1)         // 更深蓝
        )
    }
}

/**
 * 获取 Snackbar 图标
 */
private fun getSnackbarIcon(type: SnackbarType): ImageVector {
    return when (type) {
        SnackbarType.SUCCESS -> Icons.Default.CheckCircle
        SnackbarType.ERROR -> Icons.Default.Error
        SnackbarType.WARNING -> Icons.Default.Warning
        SnackbarType.INFO -> Icons.Default.Info
    }
}

// ========== Previews ==========

@Preview(showBackground = true)
@Composable
private fun PreviewSuccessSnackbar() {
    MaterialTheme {
        Surface {
            PulseLinkSnackbar(
                snackbarData = object : SnackbarData {
                    override val visuals = object : SnackbarVisuals {
                        override val message = "操作成功！数据已保存"
                        override val actionLabel: String? = null
                        override val withDismissAction = true
                        override val duration = SnackbarDuration.Short
                    }
                    override fun dismiss() {}
                    override fun performAction() {}
                },
                type = SnackbarType.SUCCESS
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewErrorSnackbar() {
    MaterialTheme {
        Surface {
            PulseLinkSnackbar(
                snackbarData = object : SnackbarData {
                    override val visuals = object : SnackbarVisuals {
                        override val message = "操作失败，请稍后重试"
                        override val actionLabel = "重试"
                        override val withDismissAction = true
                        override val duration = SnackbarDuration.Short
                    }
                    override fun dismiss() {}
                    override fun performAction() {}
                },
                type = SnackbarType.ERROR
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewWarningSnackbar() {
    MaterialTheme {
        Surface {
            PulseLinkSnackbar(
                snackbarData = object : SnackbarData {
                    override val visuals = object : SnackbarVisuals {
                        override val message = "请注意：此操作无法撤销"
                        override val actionLabel: String? = null
                        override val withDismissAction = true
                        override val duration = SnackbarDuration.Short
                    }
                    override fun dismiss() {}
                    override fun performAction() {}
                },
                type = SnackbarType.WARNING
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewInfoSnackbar() {
    MaterialTheme {
        Surface {
            PulseLinkSnackbar(
                snackbarData = object : SnackbarData {
                    override val visuals = object : SnackbarVisuals {
                        override val message = "温馨提示：记得多喝水哦"
                        override val actionLabel: String? = null
                        override val withDismissAction = true
                        override val duration = SnackbarDuration.Short
                    }
                    override fun dismiss() {}
                    override fun performAction() {}
                },
                type = SnackbarType.INFO
            )
        }
    }
}
