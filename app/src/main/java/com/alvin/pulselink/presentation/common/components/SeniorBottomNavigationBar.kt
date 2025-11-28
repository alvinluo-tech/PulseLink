package com.alvin.pulselink.presentation.common.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun SeniorBottomNavigationBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    enableVoiceInput: Boolean = false,
    isRecording: Boolean = false,
    recordingAmplitude: Float = 0f, // 新增：振幅参数 (0f-1f)
    onMicPressed: () -> Unit = {},
    onMicReleased: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 8.dp
        ) {
            NavigationBarItem(
                icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home") },
                selected = selectedItem == 0,
                onClick = { onItemSelected(0) },
                modifier = Modifier.weight(1f)
            )

            // Spacer for FAB
            Spacer(modifier = Modifier.weight(1f))

            NavigationBarItem(
                icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") },
                selected = selectedItem == 2,
                onClick = { onItemSelected(2) },
                modifier = Modifier.weight(1f)
            )
        }
        
        // Floating Action Button positioned absolutely
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-20).dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // 波纹动画层 - 根据音量振幅变化
            if (isRecording) {
                val rippleScale by animateFloatAsState(
                    targetValue = 1.0f + (recordingAmplitude * 0.5f), // 0.5倍振幅变化
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "rippleScale"
                )
                
                // 外层波纹
                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(rippleScale),
                    shape = CircleShape,
                    color = Color(0xFF448AFF).copy(alpha = 0.2f)
                ) {}
            }
            
            // 按钮本身的缩放动画
            val buttonScale by animateFloatAsState(
                targetValue = if (isRecording) 1.0f + (recordingAmplitude * 0.15f) else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "buttonScale"
            )
            
            // Custom FAB with full gesture control
            Surface(
                modifier = Modifier
                    .size(72.dp)
                    .scale(buttonScale)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape
                    )
                    .pointerInput(enableVoiceInput) {
                        detectTapGestures(
                            onPress = {
                                if (enableVoiceInput) {
                                    // Voice input mode: press and hold
                                    onMicPressed()
                                    tryAwaitRelease()
                                    onMicReleased()
                                } else {
                                    // Navigation mode: single tap
                                    tryAwaitRelease()
                                    onItemSelected(1)
                                }
                            }
                        )
                    },
                shape = CircleShape,
                color = if (isRecording) Color(0xFFFF5252) else Color(0xFF448AFF)
            ) {
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = if (enableVoiceInput) "Long press to speak" else "Voice Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}