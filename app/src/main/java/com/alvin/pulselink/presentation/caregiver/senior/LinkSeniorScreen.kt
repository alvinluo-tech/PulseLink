package com.alvin.pulselink.presentation.caregiver.senior

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.pulselink.domain.model.Senior

/**
 * 绑定老人账户页面
 * - 空状态：显示引导按钮
 * - 有数据：显示列表 + FAB
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkSeniorScreen(
    viewModel: LinkSeniorViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLinkForm by remember { mutableStateOf(false) }
    
    // 加载已绑定的老人
    LaunchedEffect(Unit) {
        viewModel.loadLinkedSeniors()
    }
    
    // 监听绑定成功
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar("老人账户绑定成功！")
            viewModel.resetLinkForm()
            viewModel.loadLinkedSeniors()
            showLinkForm = false
        }
    }
    
    // 监听错误
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Link Senior Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showLinkForm) {
                            showLinkForm = false
                            viewModel.resetLinkForm()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = if (showLinkForm) Icons.Default.ArrowBack else Icons.Default.Close,
                            contentDescription = "Back",
                            tint = Color(0xFF8B5CF6)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.linkedSeniors.isNotEmpty() && !showLinkForm) {
                FloatingActionButton(
                    onClick = { showLinkForm = true },
                    containerColor = Color(0xFF8B5CF6)
                ) {
                    Icon(Icons.Default.Link, "Link")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(padding)
        ) {
            when {
                showLinkForm -> LinkForm(uiState, viewModel)
                uiState.isLoadingList -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF8B5CF6)
                )
                uiState.linkedSeniors.isEmpty() -> EmptyState { showLinkForm = true }
                else -> LinkedSeniorsList(uiState.linkedSeniors)
            }
        }
    }
}

@Composable
private fun EmptyState(onLinkClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFFDEEDFF), RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Link,
                null,
                Modifier.size(60.dp),
                tint = Color(0xFF2563EB)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "No Linked Seniors Yet",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Link to an existing senior account using their virtual ID",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onLinkClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
        ) {
            Icon(Icons.Default.Link, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Link Senior Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LinkedSeniorsList(seniors: List<Senior>) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Linked Seniors (${seniors.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
        }
        items(seniors) { LinkedSeniorCard(it) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun LinkedSeniorCard(senior: Senior) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        Modifier.size(24.dp),
                        tint = Color(0xFF10B981)
                    )
                    Column {
                        Text(senior.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(senior.id, fontSize = 12.sp, color = Color(0xFF7C3AED))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDEEDFF)
                ) {
                    Text(
                        "Linked",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                InfoItem(Icons.Default.Cake, "Age", "${senior.age} years")
                InfoItem(
                    if (senior.gender == "Male") Icons.Default.Male else Icons.Default.Female,
                    "Gender",
                    senior.gender
                )
            }
        }
    }
}

@Composable
private fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = Color(0xFF8B5CF6))
        Column {
            Text(label, fontSize = 11.sp, color = Color(0xFF9CA3AF))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LinkForm(state: LinkSeniorUiState, viewModel: LinkSeniorViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Link Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Link New Senior",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        Modifier.size(20.dp).padding(top = 2.dp),
                        tint = Color(0xFF3B82F6)
                    )
                    Text(
                        "Enter the unique virtual ID (SNR-XXXXXXXX) to link an existing senior account",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp
                    )
                }
                
                Column {
                    Text(
                        "Senior Virtual ID",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.seniorId,
                        onValueChange = viewModel::onSeniorIdChanged,
                        placeholder = { Text("SNR-XXXXXXXX", color = Color(0xFFBDBDBD)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Badge,
                                null,
                                Modifier.size(20.dp),
                                tint = Color(0xFF8B5CF6)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF9FAFB),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            focusedBorderColor = Color(0xFF8B5CF6)
                        ),
                        isError = state.seniorIdError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    state.seniorIdError?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                Button(
                    onClick = { viewModel.linkSenior() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Link, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Link Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        
        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(Color(0xFFEFF6FF))
        ) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Info, null, Modifier.size(20.dp), tint = Color(0xFF3B82F6))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "How to find the Virtual ID?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E40AF)
                    )
                    Text(
                        "Ask the person who created the senior account. The ID format is: SNR-XXXXXXXX",
                        fontSize = 12.sp,
                        color = Color(0xFF1E40AF),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
