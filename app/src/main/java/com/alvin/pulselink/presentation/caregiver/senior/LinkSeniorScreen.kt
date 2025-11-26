package com.alvin.pulselink.presentation.caregiver.senior

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.pulselink.domain.model.SeniorProfile
import com.alvin.pulselink.presentation.common.components.QRCodeScannerDialog
import com.alvin.pulselink.util.AvatarHelper
import com.alvin.pulselink.util.RelationshipHelper
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Link Senior Account Screen
 * - Empty state: Show guide button
 * - With data: Show list + FAB
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkSeniorScreen(
    viewModel: LinkSeniorViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorDialogState by viewModel.errorDialogState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLinkForm by remember { mutableStateOf(false) }
    
    // Load linked seniors
    LaunchedEffect(Unit) {
        viewModel.loadLinkedSeniors()
    }
    
    // Listen for link success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            showLinkForm = false  // Close link form
            viewModel.resetLinkForm()  // Reset state
            viewModel.loadLinkedSeniors()  // Reload list
        }
    }
    
    // Collect UiEvent from Channel (success Snackbar)
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is com.alvin.pulselink.presentation.caregiver.senior.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Link Senior Account", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827)) },
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF8B5CF6)
                        )
                    }
                },
                actions = {
                    // History button
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Link History",
                            tint = Color(0xFF8B5CF6)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF111827),
                    navigationIconContentColor = Color(0xFF8B5CF6)
                )
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
    
    // Error Dialog (from StateFlow)
    errorDialogState?.let { dialogState ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissErrorDialog() },
            title = {
                Text(
                    text = dialogState.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = dialogState.message,
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.dismissErrorDialog() }
                ) {
                    Text("OK", fontSize = 16.sp)
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(48.dp)
                )
            }
        )
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
private fun LinkedSeniorsList(seniors: List<SeniorProfile>) {
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
        items(seniors) { senior ->
            LinkedSeniorCard(senior)
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun LinkedSeniorCard(senior: SeniorProfile) {
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Get avatar emoji based on avatarType
    val avatarEmoji = AvatarHelper.getAvatarEmoji(senior.avatarType)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
        onClick = {}
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
                    // Avatar emoji
                    Text(
                        text = avatarEmoji,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = Color.White,
                                shape = CircleShape
                            )
                            .padding(8.dp)
                    )
                    
                    Column {
                        // Note: LinkedSeniorCard only shows senior info without nickname
                        // as it displays linked seniors without relation details
                        Text(senior.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(senior.id, fontSize = 12.sp, color = Color(0xFF7C3AED))
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(senior.id))
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "ID copied to clipboard",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy ID",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF7C3AED)
                                )
                            }
                        }
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
    // Show verification screen if senior is found
    if (state.foundSenior != null) {
        VerifyAndLinkScreen(state, viewModel)
    } else {
        SearchSeniorScreen(state, viewModel)
    }
    
    // Show not found dialog
    if (state.showNotFoundDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNotFoundDialog() },
            icon = {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Senior Account Not Found",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "The Virtual ID you entered does not exist. Please check the ID and try again.",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissNotFoundDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Try Again")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SearchSeniorScreen(state: LinkSeniorUiState, viewModel: LinkSeniorViewModel) {
    var showScanner by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        
        // Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFFE9D5FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Link,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color(0xFF8B5CF6)
            )
        }
        
        // Title
        Text(
            "Connect to an existing senior account",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(8.dp))
        
        // Input Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Senior Virtual ID *",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = state.seniorId,
                    onValueChange = viewModel::onSeniorIdChanged,
                    placeholder = { Text("Enter ID or scan QR code", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFD1D5DB),
                        focusedBorderColor = Color(0xFF8B5CF6)
                    ),
                    isError = state.seniorIdError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                
                // QR Code Scan Button
                Button(
                    onClick = { showScanner = true },
                    modifier = Modifier
                        .size(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B5CF6)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR Code",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }
            }
            
            Text(
                "Format: SNR-XXXXXXXXXXXX (12 digits, e.g., SNR-KXM2VQW7ABCD)",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF)
            )
            
            state.seniorIdError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }
        
        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(Color(0xFFF3E8FF)),
            border = BorderStroke(1.dp, Color(0xFFE9D5FF))
        ) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF8B5CF6)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ask your senior family member for their Virtual ID. They can find it in their profile settings.",
                        fontSize = 13.sp,
                        color = Color(0xFF6B21A8),
                        lineHeight = 18.sp
                    )
                    Text(
                        "After submitting, you'll specify your relationship and the account creator will review your request.",
                        fontSize = 13.sp,
                        color = Color(0xFF6B21A8),
                        lineHeight = 18.sp
                    )
                }
            }
        }
        
        // Search Button
        Button(
            onClick = { viewModel.searchSenior() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            enabled = !state.isSearching
        ) {
            if (state.isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Search Senior Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        // Help Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(Color(0xFFFEF3C7)),
            border = BorderStroke(1.dp, Color(0xFFFDE68A))
        ) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFD97706)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Don't have the ID?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF92400E)
                    )
                    Text(
                        "If your senior family member doesn't have an account yet, you can create one for them using the \"New Senior\" option in the Profile menu.",
                        fontSize = 12.sp,
                        color = Color(0xFF92400E),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
    
    // QR Code Scanner Dialog
    if (showScanner) {
        QRCodeScannerDialog(
            onDismiss = { showScanner = false },
            onQRCodeScanned = { qrCodeData ->
                try {
                    // Parse QR code JSON: {"type":"pulselink_senior_id","id":"SNR-XXX"}
                    val json = JSONObject(qrCodeData)
                    if (json.optString("type") == "pulselink_senior_id") {
                        val seniorId = json.optString("id")
                        if (seniorId.isNotEmpty()) {
                            viewModel.onSeniorIdChangedAndSearch(seniorId)
                            showScanner = false
                        }
                    }
                } catch (e: Exception) {
                    // Invalid QR code format, ignore
                }
            },
            title = "Scan Senior QR Code"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerifyAndLinkScreen(state: LinkSeniorUiState, viewModel: LinkSeniorViewModel) {
    val senior = state.foundSenior ?: return
    val avatarEmoji = AvatarHelper.getAvatarEmoji(senior.avatarType)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        
        // Senior Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(Color(0xFFF0FDF4)),
            border = BorderStroke(1.dp, Color(0xFF86EFAC))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarEmoji,
                        fontSize = 40.sp,
                        color = Color.Unspecified
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        senior.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        "ID: ${senior.id}",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        "Age: ${senior.age} • Gender: ${senior.gender}",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF10B981)
                        )
                        Text(
                            "Senior account found",
                            fontSize = 13.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF10B981)
                )
            }
        }
        
        // Relationship Selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "You are the senior's... *",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
            
            var expanded by remember { mutableStateOf(false) }
            val relationships = listOf(
                "-- Select Relationship --",
                "Son",
                "Daughter", 
                "Grandson",
                "Granddaughter",
                "Spouse",
                "Sibling",
                "Friend",
                "Caregiver",
                "Other"
            )
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = state.relationship.ifBlank { "-- Select Relationship --" },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFD1D5DB),
                        focusedBorderColor = Color(0xFF8B5CF6)
                    ),
                    isError = state.relationshipError != null
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    relationships.forEach { relationship ->
                        DropdownMenuItem(
                            text = { Text(relationship) },
                            onClick = {
                                viewModel.onRelationshipChanged(relationship)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            state.relationshipError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }
        
        // How you call them (Nickname) - Optional
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "How you call them (Optional)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
            
            OutlinedTextField(
                value = state.nickname,
                onValueChange = viewModel::onNicknameChanged,
                placeholder = { 
                    val defaultTitle = if (state.relationship.isNotBlank() && state.relationship != "-- Select Relationship --" && state.foundSenior != null) {
                        RelationshipHelper.getDefaultAddressTitle(state.relationship, state.foundSenior!!.gender)
                    } else {
                        "e.g., Dad, Mom, Grandpa"
                    }
                    Text(defaultTitle, color = Color(0xFF9CA3AF))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFD1D5DB),
                    focusedBorderColor = Color(0xFF8B5CF6)
                ),
                singleLine = true
            )
            
            Text(
                "Leave blank to use default (e.g., ${if (state.relationship.isNotBlank() && state.relationship != "-- Select Relationship --" && state.foundSenior != null) RelationshipHelper.getDefaultAddressTitle(state.relationship, state.foundSenior!!.gender) else "Father, Mother"})",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF)
            )
        }
        
        // Your Name
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Your Name *",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
            
            OutlinedTextField(
                value = state.caregiverName,
                onValueChange = viewModel::onCaregiverNameChanged,
                placeholder = { Text("Enter your full name", color = Color(0xFF9CA3AF)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFD1D5DB),
                    focusedBorderColor = Color(0xFF8B5CF6)
                ),
                isError = state.caregiverNameError != null,
                singleLine = true
            )
            
            Text(
                "This name will be shown to the senior and account creator",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF)
            )
            
            state.caregiverNameError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }
        
        // Message (Optional)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Message (Optional)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
            
            OutlinedTextField(
                value = state.message,
                onValueChange = viewModel::onMessageChanged,
                placeholder = { Text("Add a personal message to help verify your identity...", color = Color(0xFF9CA3AF)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFD1D5DB),
                    focusedBorderColor = Color(0xFF8B5CF6)
                ),
                maxLines = 4
            )
            
            Text(
                "Example: \"Hi Dad, it's your son David. I'd like to help monitor your health.\"",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF)
            )
        }
        
        // Warning Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(Color(0xFFFEF3C7)),
            border = BorderStroke(1.dp, Color(0xFFFDE68A))
        ) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFD97706)
                )
                Text(
                    "Your request will be reviewed by the account creator for approval. Please provide accurate information.",
                    fontSize = 13.sp,
                    color = Color(0xFF92400E),
                    lineHeight = 18.sp
                )
            }
        }
        
        // Send Request Button
        Button(
            onClick = { viewModel.sendLinkRequest() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            enabled = !state.isLinking
        ) {
            if (state.isLinking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text(
                    "Send Link Request",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
