package com.alvin.pulselink.presentation.common.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

/**
 * CompositionLocal 用于提供当前角色的主题配色
 * 使得所有子组件都能访问到角色主题
 * 
 * 位置：presentation/common/theme/
 * 原因：角色主题提供者属于 presentation 层的共享工具
 */
val LocalRoleColorScheme = compositionLocalOf { SeniorColorScheme }

/**
 * 角色主题提供者
 * 根据用户角色动态切换配色方案
 * 
 * 使用示例：
 * ```
 * RoleThemeProvider(role = "senior") {
 *     LoginScreen(...)
 * }
 * ```
 * 
 * 在组件内部访问主题：
 * ```
 * val colors = LocalRoleColorScheme.current
 * Box(modifier = Modifier.background(colors.backgroundColor))
 * ```
 * 
 * @param role 用户角色 ("senior" 或 "caregiver")
 * @param content 使用该主题的内容组件
 */
@Composable
fun RoleThemeProvider(
    role: String,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(role) {
        getRoleColorScheme(role)
    }
    
    CompositionLocalProvider(LocalRoleColorScheme provides colorScheme) {
        content()
    }
}

/**
 * 便捷扩展函数：在 Composable 中快速获取当前角色配色
 */
val roleColors: RoleColorScheme
    @Composable
    get() = LocalRoleColorScheme.current
