package com.alvin.pulselink.presentation.common.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alvin.pulselink.presentation.common.state.SnackbarType
import kotlinx.coroutines.delay

/**
 * 全屏英雄式状态反馈组件
 * 
 * 设计特点：
 * - 全屏半透明黑色遮罩
 * - 屏幕中央显示大圆角卡片
 * - 巨大的图标 + 大字体文字
 * - 弹跳动画效果
 * - 自动消失
 * 
 * 适用场景：
 * **仅用于 Senior 端**的关键操作：
 * - 语音录入成功
 * - 吃药打卡成功
 * - 重要数据保存成功
 * 
 * 给老人强烈的视觉反馈和安全感
 */
@Composable
fun StatusHeroOverlay(
    message: String,
    type: SnackbarType = SnackbarType.SUCCESS,
    durationMillis: Long = 1500L,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
        delay(durationMillis)
        visible = false
        delay(300) // 等待退出动画完成
        onDismiss()
    }
    
    Dialog(
        onDismissRequest = { /* 不允许点击外部关闭 */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(
                    animationSpec = tween(300)
                ) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = fadeOut(
                    animationSpec = tween(200)
                ) + scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(200)
                )
            ) {
                HeroCard(
                    message = message,
                    type = type
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    message: String,
    type: SnackbarType
) {
    val colors = getHeroColors(type)
    val icon = getHeroIcon(type)
    
    // 图标脉动动画
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_pulse"
    )
    
    Card(
        modifier = Modifier
            .width(280.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 16.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 大图标（带脉动动画）
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.iconColor,
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
            )
            
            // 消息文字（超大字体）
            Text(
                text = message,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textColor,
                    lineHeight = 36.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 英雄式反馈的颜色方案
 */
private data class HeroColors(
    val backgroundColor: Color,
    val iconColor: Color,
    val textColor: Color
)

@Composable
private fun getHeroColors(type: SnackbarType): HeroColors {
    return when (type) {
        SnackbarType.SUCCESS -> HeroColors(
            backgroundColor = Color(0xFFF1F8E9), // 非常淡的绿
            iconColor = Color(0xFF4CAF50),        // 明亮的绿
            textColor = Color(0xFF2E7D32)         // 深绿
        )
        SnackbarType.ERROR -> HeroColors(
            backgroundColor = Color(0xFFFCE4EC), // 非常淡的红
            iconColor = Color(0xFFEF5350),        // 明亮的红
            textColor = Color(0xFFC62828)         // 深红
        )
        SnackbarType.WARNING -> HeroColors(
            backgroundColor = Color(0xFFFFF8E1), // 非常淡的黄
            iconColor = Color(0xFFFF9800),        // 明亮的橙
            textColor = Color(0xFFF57C00)         // 深橙
        )
        SnackbarType.INFO -> HeroColors(
            backgroundColor = Color(0xFFE1F5FE), // 非常淡的蓝
            iconColor = Color(0xFF42A5F5),        // 明亮的蓝
            textColor = Color(0xFF1976D2)         // 深蓝
        )
    }
}

/**
 * 获取英雄式图标
 */
private fun getHeroIcon(type: SnackbarType): ImageVector {
    return when (type) {
        SnackbarType.SUCCESS -> Icons.Default.CheckCircle
        SnackbarType.ERROR -> Icons.Default.Error
        SnackbarType.WARNING -> Icons.Default.Warning
        SnackbarType.INFO -> Icons.Default.Info
    }
}

/**
 * 加载中英雄式覆盖层
 * 专门用于显示加载状态
 */
@Composable
fun LoadingHeroOverlay(
    message: String = "处理中...",
    onDismiss: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .width(280.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 16.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 大号进度指示器
                    CircularProgressIndicator(
                        modifier = Modifier.size(80.dp),
                        strokeWidth = 6.dp,
                        color = Color(0xFF8B5CF6)
                    )
                    
                    // 加载文字
                    Text(
                        text = message,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ========== Previews ==========

@Preview(showBackground = true)
@Composable
private fun PreviewSuccessHero() {
    MaterialTheme {
        StatusHeroOverlay(
            message = "保存成功",
            type = SnackbarType.SUCCESS,
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewErrorHero() {
    MaterialTheme {
        StatusHeroOverlay(
            message = "操作失败",
            type = SnackbarType.ERROR,
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLoadingHero() {
    MaterialTheme {
        LoadingHeroOverlay(
            message = "正在处理...",
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMedicationSuccess() {
    MaterialTheme {
        StatusHeroOverlay(
            message = "吃药打卡成功！\n按时服药身体好",
            type = SnackbarType.SUCCESS,
            onDismiss = {}
        )
    }
}
