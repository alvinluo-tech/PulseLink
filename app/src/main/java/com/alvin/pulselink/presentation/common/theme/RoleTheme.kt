package com.alvin.pulselink.presentation.common.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.alvin.pulselink.ui.theme.*

/**
 * 角色主题配色方案
 * 定义两套完整的配色，支持基于用户角色的动态切换
 * 
 * 位置：presentation/common/theme/
 * 原因：角色主题属于 presentation 层的共享资源，不是全局 UI 主题
 */
@Immutable
data class RoleColorScheme(
    // 背景色
    val backgroundColor: Color,
    val surfaceColor: Color,
    val cardBackground: Color,
    
    // 主色调
    val primary: Color,
    val primaryVariant: Color,
    val onPrimary: Color,
    
    // 强调色
    val accent: Color,
    val accentVariant: Color,
    
    // 文本颜色
    val textPrimary: Color,
    val textSecondary: Color,
    val textHint: Color,
    
    // 按钮颜色
    val buttonBackground: Color,
    val buttonText: Color,
    val buttonDisabled: Color,
    
    // 输入框颜色
    val inputBackground: Color,
    val inputBorder: Color,
    val inputFocusBorder: Color,
    val inputText: Color,
    
    // 图标背景
    val iconBackground: Color,
    val iconTint: Color,
    
    // 状态颜色
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    
    // 分割线
    val divider: Color
)

/**
 * 老人端配色方案
 * 设计理念：温暖、友好、易读
 * 主色调：蓝色系（信任、安全感）
 */
val SeniorColorScheme = RoleColorScheme(
    // 背景 - 浅灰蓝色，柔和不刺眼
    backgroundColor = Color(0xFFE8EDF2),
    surfaceColor = Color(0xFFF5F7FA),
    cardBackground = Color(0xFFFFFFFF),
    
    // 主色 - 智能蓝（SmartHomeBlue）
    primary = SmartHomeBlue,  // #448AFF
    primaryVariant = Color(0xFF2979FF),
    onPrimary = Color.White,
    
    // 强调色 - 温暖的橙色
    accent = ReminderOrange,  // #FFB74D
    accentVariant = Color(0xFFFF9800),
    
    // 文本 - 深灰色，高对比度易读
    textPrimary = Color(0xFF2C3E50),
    textSecondary = Color(0xFF7F8C8D),
    textHint = Color(0xFFBDC3C7),
    
    // 按钮 - 使用主色调
    buttonBackground = SmartHomeBlue.copy(alpha = 0.7f),
    buttonText = Color.White,
    buttonDisabled = Color(0xFFCFD8DC),
    
    // 输入框 - 白色背景，蓝色边框
    inputBackground = Color.White,
    inputBorder = Color(0xFFE0E0E0),
    inputFocusBorder = SmartHomeBlue.copy(alpha = 0.5f),
    inputText = Color(0xFF2C3E50),
    
    // 图标 - 蓝色系
    iconBackground = SmartHomeBlue,
    iconTint = Color.White,
    
    // 状态色
    success = HealthGreen,  // #00E676
    warning = ReminderOrange,  // #FFB74D
    error = EmergencyRed,  // #FF5252
    info = SmartHomeBlue,  // #448AFF
    
    // 分割线
    divider = Color(0xFFE0E0E0)
)

/**
 * 子女端配色方案
 * 设计理念：现代、专业、清新
 * 主色调：绿色系（关怀、健康、活力）
 */
val CaregiverColorScheme = RoleColorScheme(
    // 背景 - 浅紫色，现代且柔和
    backgroundColor = Color(0xFFF3E8FF),
    surfaceColor = Color(0xFFF9F5FF),
    cardBackground = Color(0xFFFFFFFF),
    
    // 主色 - 紫色系（与护理端主色一致）
    primary = Color(0xFF9333EA),
    primaryVariant = Color(0xFF7E22CE),
    onPrimary = Color.White,
    
    // 强调色 - 渐变紫（与欢迎页一致）
    accent = Color(0xFFB863E8),
    accentVariant = Color(0xFF9B4FD8),
    
    // 文本 - 深色提高可读性
    textPrimary = Color(0xFF2C3E50),
    textSecondary = Color(0xFF6B7280),
    textHint = Color(0xFFAAA1C8),
    
    // 按钮 - 使用主色调
    buttonBackground = Color(0xFF9333EA),
    buttonText = Color.White,
    buttonDisabled = Color(0xFFEAD7FF),
    
    // 输入框 - 白底，紫色聚焦边框
    inputBackground = Color.White,
    inputBorder = Color(0xFFE0E0E0),
    inputFocusBorder = Color(0xFFB863E8),
    inputText = Color(0xFF2C3E50),
    
    // 图标 - 紫色系
    iconBackground = Color(0xFF9333EA),
    iconTint = Color.White,
    
    // 状态色（保留通用含义）
    success = Color(0xFF4CAF50),
    warning = Color(0xFFFFA726),
    error = Color(0xFFEF5350),
    info = Color(0xFF9333EA),
    
    // 分割线
    divider = Color(0xFFE0E0E0)
)

/**
 * 根据角色字符串获取对应的配色方案
 */
fun getRoleColorScheme(role: String): RoleColorScheme {
    return when (role.lowercase()) {
        "senior", "老人" -> SeniorColorScheme
        "caregiver", "子女", "看护" -> CaregiverColorScheme
        else -> SeniorColorScheme  // 默认使用老人端配色
    }
}
