package com.alvin.pulselink.presentation.senior.voice.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * 语音输入按钮，带音量波纹动画
 * 
 * @param amplitude 归一化音量 (0.0 - 1.0)
 * @param isRecording 是否正在录音
 * @param onPressed 按下时回调
 * @param onReleased 松开时回调
 */
@Composable
fun VoiceInputButton(
    amplitude: Float,
    isRecording: Boolean,
    onPressed: () -> Unit,
    onReleased: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 动画：根据音量大小，控制波纹的缩放
    val animatedScale by animateFloatAsState(
        targetValue = if (isRecording) 1f + (amplitude * 0.5f) else 1f, // 最大放大 1.5 倍
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "amplitude_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // --- 波纹层 (Ripple) ---
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(animatedScale) // 随音量跳动
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }

        // --- 按钮层 ---
        Button(
            onClick = { /* 使用 pointerInput 处理 */ },
            modifier = Modifier
                .size(80.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            // 按下：开始录音
                            onPressed()
                            tryAwaitRelease()
                            // 松开：结束录音
                            onReleased()
                        }
                    )
                },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "按住说话",
                modifier = Modifier.size(40.dp),
                tint = Color.White
            )
        }
    }
}
