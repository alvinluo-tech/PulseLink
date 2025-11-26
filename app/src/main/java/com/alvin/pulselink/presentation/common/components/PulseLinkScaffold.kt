package com.alvin.pulselink.presentation.common.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.alvin.pulselink.presentation.common.state.CustomSnackbarVisuals
import com.alvin.pulselink.presentation.common.state.SnackbarType
import com.alvin.pulselink.presentation.common.state.UiEvent
import kotlinx.coroutines.flow.Flow

/**
 * PulseLink 通用 Scaffold
 * 
 * 自动处理：
 * - 自定义 Snackbar 样式
 * - 英雄式覆盖层
 * - 加载中覆盖层
 * 
 * 使用方式：
 * ```kotlin
 * PulseLinkScaffold(
 *     uiEventFlow = viewModel.uiEvent,
 *     topBar = { ... },
 *     floatingActionButton = { ... }
 * ) { paddingValues ->
 *     // 你的内容
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseLinkScaffold(
    uiEventFlow: Flow<UiEvent>? = null,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { 
        SnackbarHost(it) { data ->
            PulseLinkSnackbar(
                snackbarData = data,
                type = (data.visuals as? CustomSnackbarVisuals)?.type ?: SnackbarType.INFO
            )
        }
    },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.background,
    contentColor: androidx.compose.ui.graphics.Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showHeroOverlay by remember { mutableStateOf<UiEvent.ShowHeroOverlay?>(null) }
    var showLoadingOverlay by remember { mutableStateOf<String?>(null) }
    
    // 收集 UI 事件
    LaunchedEffect(uiEventFlow) {
        uiEventFlow?.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        CustomSnackbarVisuals(
                            message = event.message,
                            type = event.type,
                            actionLabel = event.actionLabel,
                            duration = event.duration
                        )
                    )
                }
                is UiEvent.ShowHeroOverlay -> {
                    showHeroOverlay = event
                }
                is UiEvent.ShowLoading -> {
                    showLoadingOverlay = event.message
                }
                is UiEvent.HideLoading -> {
                    showLoadingOverlay = null
                }
            }
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = { snackbarHost(snackbarHostState) },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        content = content
    )
    
    // 英雄式覆盖层
    showHeroOverlay?.let { overlay ->
        StatusHeroOverlay(
            message = overlay.message,
            type = overlay.type,
            durationMillis = overlay.durationMillis,
            onDismiss = { showHeroOverlay = null }
        )
    }
    
    // 加载中覆盖层
    showLoadingOverlay?.let { message ->
        LoadingHeroOverlay(
            message = message,
            onDismiss = null // 不允许手动关闭
        )
    }
}
