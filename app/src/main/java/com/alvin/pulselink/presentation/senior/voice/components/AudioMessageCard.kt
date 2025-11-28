package com.alvin.pulselink.presentation.senior.voice.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 音频消息卡片
 * 
 * @param duration 音频时长（秒）
 * @param isPlaying 是否正在播放
 * @param isFromAssistant 是否来自AI
 * @param onPlayClick 点击播放/暂停
 */
@Composable
fun AudioMessageCard(
    duration: Int,
    isPlaying: Boolean,
    isFromAssistant: Boolean,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isFromAssistant) Color.White else Color(0xFFE3F2FD),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = modifier.widthIn(max = 280.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable { onPlayClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 播放按钮
            Surface(
                shape = CircleShape,
                color = if (isFromAssistant) Color(0xFF448AFF) else Color(0xFF1976D2),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // 音频信息
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Audio",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Voice Message",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
                
                Text(
                    text = formatDuration(duration),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
            }
        }
    }
}

/**
 * 格式化时长 (秒 -> mm:ss)
 */
private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}
