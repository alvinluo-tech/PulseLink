package com.alvin.pulselink.presentation.common.state

/**
 * Snackbar 类型枚举
 * 定义不同的反馈类型及其对应的视觉样式
 */
enum class SnackbarType {
    /**
     * 成功反馈 - 绿色主题
     * 用于：操作成功、数据保存成功等
     */
    SUCCESS,
    
    /**
     * 错误反馈 - 红色主题
     * 用于：操作失败、网络错误等
     */
    ERROR,
    
    /**
     * 警告反馈 - 黄/橙色主题
     * 用于：需要用户注意但非致命的情况
     */
    WARNING,
    
    /**
     * 信息反馈 - 蓝色主题
     * 用于：一般性提示、中性信息
     */
    INFO
}
