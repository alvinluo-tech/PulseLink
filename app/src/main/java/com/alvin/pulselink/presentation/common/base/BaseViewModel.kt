package com.alvin.pulselink.presentation.common.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.presentation.common.state.SnackbarType
import com.alvin.pulselink.presentation.common.state.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * 基础 ViewModel
 * 提供统一的 UI 事件发送机制
 * 
 * 所有 ViewModel 可以继承此类以获得：
 * - showSuccess()
 * - showError()
 * - showWarning()
 * - showInfo()
 * - showHeroSuccess() (Senior 端专用)
 * - showLoading() / hideLoading()
 */
abstract class BaseViewModel : ViewModel() {
    
    // Channel for one-time UI events
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()
    
    /**
     * 显示成功消息（绿色 Snackbar）
     * 
     * @param message 成功消息
     * @param actionLabel 可选的操作按钮文字
     */
    protected fun showSuccess(
        message: String,
        actionLabel: String? = null
    ) {
        viewModelScope.launch {
            _uiEvent.send(
                UiEvent.ShowSnackbar(
                    message = message,
                    type = SnackbarType.SUCCESS,
                    actionLabel = actionLabel
                )
            )
        }
    }
    
    /**
     * 显示错误消息（红色 Snackbar）
     * 
     * @param message 错误消息
     * @param actionLabel 可选的操作按钮文字（如"重试"）
     */
    protected fun showError(
        message: String,
        actionLabel: String? = null
    ) {
        viewModelScope.launch {
            _uiEvent.send(
                UiEvent.ShowSnackbar(
                    message = message,
                    type = SnackbarType.ERROR,
                    actionLabel = actionLabel,
                    duration = androidx.compose.material3.SnackbarDuration.Long
                )
            )
        }
    }
    
    /**
     * 显示警告消息（橙色 Snackbar）
     * 
     * @param message 警告消息
     * @param actionLabel 可选的操作按钮文字
     */
    protected fun showWarning(
        message: String,
        actionLabel: String? = null
    ) {
        viewModelScope.launch {
            _uiEvent.send(
                UiEvent.ShowSnackbar(
                    message = message,
                    type = SnackbarType.WARNING,
                    actionLabel = actionLabel
                )
            )
        }
    }
    
    /**
     * 显示信息消息（蓝色 Snackbar）
     * 
     * @param message 信息消息
     * @param actionLabel 可选的操作按钮文字
     */
    protected fun showInfo(
        message: String,
        actionLabel: String? = null
    ) {
        viewModelScope.launch {
            _uiEvent.send(
                UiEvent.ShowSnackbar(
                    message = message,
                    type = SnackbarType.INFO,
                    actionLabel = actionLabel
                )
            )
        }
    }
    
    /**
     * 显示英雄式成功反馈（全屏中央大卡片）
     * **仅用于 Senior 端的关键操作**
     * 
     * @param message 成功消息（建议简短有力）
     * @param durationMillis 显示时长（毫秒）
     */
    protected fun showHeroSuccess(
        message: String,
        durationMillis: Long = 1500L
    ) {
        viewModelScope.launch {
            _uiEvent.send(
                UiEvent.ShowHeroOverlay(
                    message = message,
                    type = SnackbarType.SUCCESS,
                    durationMillis = durationMillis
                )
            )
        }
    }
    
    /**
     * 显示英雄式错误反馈（全屏中央大卡片）
     * **仅用于 Senior 端的关键错误**
     * 
     * @param message 错误消息
     * @param durationMillis 显示时长（毫秒）
     */
    protected fun showHeroError(
        message: String,
        durationMillis: Long = 2000L
    ) {
        viewModelScope.launch {
            _uiEvent.send(
                UiEvent.ShowHeroOverlay(
                    message = message,
                    type = SnackbarType.ERROR,
                    durationMillis = durationMillis
                )
            )
        }
    }
    
    /**
     * 显示加载中覆盖层
     * 
     * @param message 加载提示文字
     */
    protected fun showLoading(message: String = "处理中...") {
        viewModelScope.launch {
            _uiEvent.send(UiEvent.ShowLoading(message))
        }
    }
    
    /**
     * 隐藏加载中覆盖层
     */
    protected fun hideLoading() {
        viewModelScope.launch {
            _uiEvent.send(UiEvent.HideLoading)
        }
    }
}
