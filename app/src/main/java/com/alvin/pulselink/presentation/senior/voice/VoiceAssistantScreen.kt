package com.alvin.pulselink.presentation.senior.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    viewModel: AssistantViewModel = hiltViewModel(),
    onClose: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
        bottomBar = {
            BottomBar(
                inputText = uiState.inputText,
                onInputChange = viewModel::onInputChange,
                onSendClick = viewModel::sendTextMessage,
                onMicClick = viewModel::onMicClicked,
                onNavigateHome = onNavigateHome,
                onNavigateProfile = onNavigateProfile,
                sending = uiState.sending
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
            ChatList(messages = uiState.messages)
        }
    }
}

@Composable
private fun ChatList(messages: List<ChatMessage>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 160.dp),
    ) {
        items(messages) { msg ->
            val bubbleColor = if (msg.fromAssistant) Color.White else Color(0xFFE3F2FD)
            val alignment = if (msg.fromAssistant) Alignment.CenterStart else Alignment.CenterEnd
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (msg.fromAssistant) Arrangement.Start else Arrangement.End
            ) {
                Surface(
                    color = bubbleColor,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .widthIn(max = 320.dp)
                ) {
                    Text(
                        text = msg.text,
                        fontSize = 22.sp,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onMicClick: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateProfile: () -> Unit,
    sending: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.0f))
    ) {
        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mic button (square rounded)
            Surface(
                onClick = onMicClick,
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF448AFF),
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text input
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = { Text("Type a message") },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFCBD5E1),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Send button (disabled style when empty)
            val enabled = inputText.isNotBlank() && !sending
            Surface(
                onClick = { if (enabled) onSendClick() },
                shape = RoundedCornerShape(16.dp),
                color = if (enabled) Color(0xFF448AFF) else Color(0xFFE2E8F0)
            ) {
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (enabled) Color.White else Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Hint text
        Text(
            text = "Tap microphone for voice input",
            color = Color(0xFF6B7280),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )

        // Bottom navigation
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 8.dp
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home") },
                selected = false,
                onClick = onNavigateHome,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF448AFF),
                    selectedTextColor = Color(0xFF448AFF),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color(0xFFE3F2FD)
                )
            )

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .offset(y = (-16).dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = onMicClick,
                    containerColor = Color(0xFF448AFF),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Assistant",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") },
                selected = true,
                onClick = onNavigateProfile,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF448AFF),
                    selectedTextColor = Color(0xFF448AFF),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color(0xFFE3F2FD)
                )
            )
        }
    }
}
