package com.alvin.pulselink.presentation.common.state

/**
 * UI 事件密封类
 * 用于 ViewModel 向 UI 层发送一次性事件
 */
sealed class UiEvent {
    /**
     * 显示 Snackbar 通知
     * 适用于：Caregiver 的所有操作，Senior 的轻量级提示
     * 
     * @param message 提示消息
     * @param type 消息类型（成功/错误/警告/信息）
     * @param actionLabel 可选的操作按钮文字
     * @param duration 显示时长
     */
    data class ShowSnackbar(
        val message: String,
        val type: SnackbarType = SnackbarType.INFO,
        val actionLabel: String? = null,
        val duration: androidx.compose.material3.SnackbarDuration = androidx.compose.material3.SnackbarDuration.Short
    ) : UiEvent()
    
    /**
     * 显示全屏英雄式反馈
     * 适用于：仅用于 Senior 端的关键操作（语音录入成功、吃药打卡成功等）
     * 给老人强烈的视觉反馈和安全感
     * 
     * @param message 提示消息（大字体显示）
     * @param type 消息类型（通常为 SUCCESS）
     * @param durationMillis 显示时长（毫秒）
     */
    data class ShowHeroOverlay(
        val message: String,
        val type: SnackbarType = SnackbarType.SUCCESS,
        val durationMillis: Long = 1500L
    ) : UiEvent()
    
    /**
     * 显示加载中覆盖层
     * 适用于：需要阻止用户操作的加载状态
     * 
     * @param message 加载提示文字
     */
    data class ShowLoading(
        val message: String = "处理中..."
    ) : UiEvent()
    
    /**
     * 隐藏加载中覆盖层
     */
    object HideLoading : UiEvent()
}
