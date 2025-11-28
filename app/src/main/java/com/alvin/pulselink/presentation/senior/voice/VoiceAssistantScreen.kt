package com.alvin.pulselink.presentation.senior.voice

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.pulselink.domain.model.ChatMessage
import com.alvin.pulselink.presentation.common.components.PulseLinkSnackbar
import com.alvin.pulselink.presentation.common.components.SeniorBottomNavigationBar
import com.alvin.pulselink.presentation.common.state.SnackbarType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    viewModel: AssistantViewModel = hiltViewModel(),
    onClose: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // 显示错误提示 - 使用 PulseLinkSnackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Short
                )
            }
            // 自动清除错误状态
            viewModel.clearError()
        }
    }
    
    // 检查权限状态
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    
    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (!isGranted) {
            showPermissionDeniedDialog = true
        } else {
            // 权限授予后立即开始录音
            viewModel.onMicPressed()
        }
    }
    
    // 权限拒绝对话框
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Microphone Permission Required") },
            text = { Text("Voice input requires microphone permission. Please grant permission in app settings.") },
            confirmButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Voice Assistant",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF448AFF)
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                PulseLinkSnackbar(
                    snackbarData = snackbarData,
                    type = SnackbarType.ERROR
                )
            }
        },
        bottomBar = {
            BottomBar(
                inputText = uiState.inputText,
                onInputChange = viewModel::onInputChange,
                onSendClick = viewModel::sendTextMessage,
                onMicPressed = {
                    Log.d("VoiceDebug", "Mic pressed! hasPermission=$hasAudioPermission")
                    if (hasAudioPermission) {
                        Log.d("VoiceDebug", "Starting audio recording...")
                        viewModel.onMicPressed()
                    } else {
                        Log.d("VoiceDebug", "Requesting permission...")
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onMicReleased = {
                    Log.d("VoiceDebug", "Mic released!")
                    if (hasAudioPermission) {
                        Log.d("VoiceDebug", "Stopping audio recording...")
                        viewModel.onMicReleased()
                    }
                },
                onNavigateHome = onNavigateHome,
                onNavigateProfile = onNavigateProfile,
                sending = uiState.sending,
                isRecording = uiState.isRecording,
                uiState = uiState
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF2F6FB), Color(0xFFE9EEF4))
                    )
                )
                .padding(paddingValues),
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            ChatList(
                messages = uiState.messages,
                isAiThinking = uiState.sending,
                userAvatarEmoji = uiState.userAvatarEmoji,
                playingMessageId = uiState.playingMessageId,
                onPlayAudio = { messageId, downloadUrl ->
                    if (messageId.isBlank()) {
                        viewModel.stopAudioPlayback()
                    } else {
                        viewModel.playAudioMessage(messageId, downloadUrl)
                    }
                }
            )
        }
    }
}

@Composable
private fun ChatList(
    messages: List<ChatMessage>, 
    isAiThinking: Boolean = false, 
    userAvatarEmoji: String = "🧓",
    playingMessageId: String? = null,
    onPlayAudio: (String, String) -> Unit
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new messages arrive or AI starts thinking
    LaunchedEffect(messages.size, isAiThinking) {
        if (messages.isNotEmpty() || isAiThinking) {
            val targetIndex = if (isAiThinking) messages.size else messages.size - 1
            listState.animateScrollToItem(targetIndex)
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 160.dp),
    ) {
        items(messages, key = { it.id }) { msg ->
            ChatMessageItem(
                message = msg, 
                userAvatarEmoji = userAvatarEmoji,
                playingMessageId = playingMessageId,
                onPlayAudio = onPlayAudio
            )
        }
        
        // AI thinking indicator
        if (isAiThinking) {
            item {
                AiThinkingIndicator()
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage, 
    userAvatarEmoji: String = "🧓",
    playingMessageId: String? = null,
    onPlayAudio: (String, String) -> Unit = { _, _ -> }
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = if (message.fromAssistant) Arrangement.Start else Arrangement.End
    ) {
        if (message.fromAssistant) {
            // AI Avatar
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color(0xFF448AFF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "AI",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        // 根据消息类型显示不同内容
        when (message.type) {
            com.alvin.pulselink.domain.model.MessageType.TEXT -> {
                // 文本消息气泡
                Surface(
                    color = if (message.fromAssistant) Color.White else Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    Text(
                        text = message.text,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            com.alvin.pulselink.domain.model.MessageType.AUDIO -> {
                // 音频消息卡片
                com.alvin.pulselink.presentation.senior.voice.components.AudioMessageCard(
                    duration = message.duration,
                    isPlaying = playingMessageId == message.id,
                    isFromAssistant = message.fromAssistant,
                    onPlayClick = {
                        android.util.Log.d("VoiceAssistantScreen", "Audio card clicked - ID: ${message.id}, URL: ${message.audioDownloadUrl}")
                        if (playingMessageId == message.id) {
                            // 如果正在播放，则停止
                            android.util.Log.d("VoiceAssistantScreen", "Stopping playback")
                            onPlayAudio("", "")
                        } else {
                            // 否则开始播放
                            android.util.Log.d("VoiceAssistantScreen", "Starting playback")
                            onPlayAudio(message.id, message.audioDownloadUrl ?: "")
                        }
                    }
                )
            }
        }
        
        if (!message.fromAssistant) {
            Spacer(modifier = Modifier.width(8.dp))
            // User Avatar - using emoji
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color(0xFFF3F4F6)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = userAvatarEmoji,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AiThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        // AI Avatar
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color(0xFF448AFF)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "AI",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        
        // Thinking bubble
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thinking",
                    fontSize = 16.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "●",
                    fontSize = 18.sp,
                    color = Color(0xFF448AFF),
                    modifier = Modifier.alpha(alpha1)
                )
                Text(
                    text = "●",
                    fontSize = 18.sp,
                    color = Color(0xFF448AFF),
                    modifier = Modifier.alpha(alpha2)
                )
                Text(
                    text = "●",
                    fontSize = 18.sp,
                    color = Color(0xFF448AFF),
                    modifier = Modifier.alpha(alpha3)
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    inputText: androidx.compose.ui.text.input.TextFieldValue,
    onInputChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onSendClick: () -> Unit,
    onMicPressed: () -> Unit,
    onMicReleased: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateProfile: () -> Unit,
    sending: Boolean,
    isRecording: Boolean,
    uiState: AssistantUiState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.0f))
    ) {
        // Input row - simplified (no left mic button)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Recording indicator above input field
            AnimatedVisibility(
                visible = isRecording,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Recording",
                        tint = Color(0xFF448AFF),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Listening... Release to stop",
                        color = Color(0xFF448AFF),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Input field row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = { 
                        Text(
                            "Type message here...", 
                            fontSize = 16.sp,
                            color = Color(0xFF94A3B8)
                        ) 
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = if (isRecording) {
                        RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 0.dp, topEnd = 0.dp)
                    } else {
                        RoundedCornerShape(16.dp)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF448AFF),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Send button
                val enabled = inputText.text.isNotBlank() && !sending
                Surface(
                    onClick = { if (enabled) onSendClick() },
                    shape = RoundedCornerShape(16.dp),
                    color = if (enabled) Color(0xFF448AFF) else Color(0xFFE2E8F0)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (enabled) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom navigation - 启用语音输入功能
        SeniorBottomNavigationBar(
            selectedItem = 1,
            onItemSelected = { index ->
                when (index) {
                    0 -> onNavigateHome()
                    1 -> { /* Already on voice assistant screen */ }
                    2 -> onNavigateProfile()
                }
            },
            enableVoiceInput = true, // 启用导航栏麦克风
            isRecording = isRecording,
            recordingAmplitude = uiState.recordingAmplitude, // 传递振幅数据
            onMicPressed = onMicPressed,
            onMicReleased = onMicReleased
        )
    }
}
