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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.alvin.pulselink.util.RelationshipHelper
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
import com.alvin.pulselink.domain.usecase.profile.ManagedSeniorInfo
import com.alvin.pulselink.util.AvatarHelper
import com.alvin.pulselink.util.QRCodeGenerator
import kotlinx.coroutines.launch

/**
 * Create Senior Account Screen
 * - Empty state: Show guide button
 * - With data: Show list + FAB
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSeniorScreen(
    viewModel: ManageSeniorsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val createState by viewModel.createFormState.collectAsStateWithLifecycle()
    val manageState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateForm by remember { mutableStateOf(false) }

    // Get created seniors (using createdSeniors from uiState)
    val createdSeniors = manageState.createdSeniors

    // Load seniors list
    LaunchedEffect(Unit) {
        viewModel.loadSeniors()
    }

    // Listen for create success - no longer auto-show QR code dialog
    LaunchedEffect(createState.isSuccess) {
        if (createState.isSuccess) {
            showCreateForm = false  // Close create form
            viewModel.resetCreateForm()
            viewModel.loadSeniors()
            snackbarHostState.showSnackbar("Senior account created successfully!")
        }
    }
    
    // Listen for errors
    LaunchedEffect(createState.errorMessage) {
        createState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Senior Account", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showCreateForm) {
                            showCreateForm = false
                            viewModel.resetCreateForm()
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF111827),
                    navigationIconContentColor = Color(0xFF8B5CF6)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (createdSeniors.isNotEmpty() && !showCreateForm) {
                FloatingActionButton(
                    onClick = { showCreateForm = true },
                    containerColor = Color(0xFF8B5CF6)
                ) {
                    Icon(Icons.Default.PersonAdd, "Create")
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
                showCreateForm -> CreateForm(createState, viewModel)
                manageState.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF8B5CF6)
                )
                createdSeniors.isEmpty() -> EmptyState { showCreateForm = true }
                else -> SeniorsList(createdSeniors)
            }
        }
    }
    
    // QR code dialog is no longer shown here, changed to show when clicking button on card
}

@Composable
private fun EmptyState(onCreateClick: () -> Unit) {
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
                .background(Color(0xFFF3E8FF), RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PersonAdd,
                null,
                Modifier.size(60.dp),
                tint = Color(0xFF8B5CF6)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "No Senior Accounts Yet",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Create your first senior account to start monitoring",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
        ) {
            Icon(Icons.Default.PersonAdd, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Create Senior Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SeniorsList(seniors: List<ManagedSeniorInfo>) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "My Created Seniors (${seniors.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
        }
        items(seniors) { seniorInfo -> 
            SeniorCard(seniorInfo.profile) 
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SeniorCard(senior: SeniorProfile, viewModel: ManageSeniorsViewModel = hiltViewModel()) {
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showQRDialog by remember { mutableStateOf(false) }
    var qrPassword by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Get avatar emoji based on avatarType
    val avatarEmoji = AvatarHelper.getAvatarEmoji(senior.avatarType)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
                        Text(
                            text = senior.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = senior.id,
                                fontSize = 12.sp,
                                color = Color(0xFF7C3AED)
                            )
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
                    color = Color(0xFFDCFCE7)
                ) {
                    Text(
                        "Creator",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF16A34A),
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
            
            Spacer(Modifier.height(16.dp))
            
            // View QR Code Button
            Button(
                onClick = {
                    scope.launch {
                        viewModel.getSeniorCredentials(senior.id)
                            .onSuccess { (id, password) ->
                                qrPassword = password
                                val qrData = """
                                    {
                                      "type": "pulselink_login",
                                      "id": "$id",
                                      "password": "$password"
                                    }
                                """.trimIndent()
                                qrBitmap = QRCodeGenerator.generateQRCode(qrData)
                                showQRDialog = true
                            }
                            .onFailure { error ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = error.message ?: "获取凭据失败",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("View Login QR Code")
            }
        }
    }
    
    // QR Code Dialog
    if (showQRDialog) {
        QRCodeDialog(
            seniorId = senior.id,
            password = qrPassword,
            qrCodeBitmap = qrBitmap,
            onDismiss = { showQRDialog = false }
        )
    }
}

@Composable
private fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color(0xFF8B5CF6)
        )
        Column {
            Text(text = label, fontSize = 11.sp, color = Color(0xFF9CA3AF))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateForm(state: CreateSeniorFormState, viewModel: ManageSeniorsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Basic Information
        FormCard("Basic Information") {
            FormField("Full Name", state.name, viewModel::onNameChanged, "Enter name", state.nameError, Icons.Default.Person)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField("Age", state.age, viewModel::onAgeChanged, "Age", state.ageError, leadingIcon = Icons.Default.Cake, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                Column(Modifier.weight(1f)) {
                    Text("Gender", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GenderChip("Male", state.gender == "Male") { viewModel.onGenderChanged("Male") }
                        GenderChip("Female", state.gender == "Female") { viewModel.onGenderChanged("Female") }
                    }
                }
            }
        }

        // Relationship Information
        FormCard("Your Relationship") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "You are the senior's... *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                
                var expanded by remember { mutableStateOf(false) }
                val relationships = RelationshipHelper.getRelationshipOptions()
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = state.relationship,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF9FAFB),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            focusedBorderColor = Color(0xFF8B5CF6)
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Default.People,
                                null,
                                Modifier.size(20.dp),
                                tint = Color(0xFF8B5CF6)
                            )
                        }
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        relationships.forEach { relationship ->
                            DropdownMenuItem(
                                text = { Text(relationship) },
                                onClick = {
                                    viewModel.onRelationshipChanged(relationship, state.gender)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Nickname field with auto-mapped default
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "How you call them",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                
                val defaultNickname = RelationshipHelper.getDefaultAddressTitle(state.relationship, state.gender)
                
                OutlinedTextField(
                    value = state.nickname,
                    onValueChange = viewModel::onNicknameChanged,
                    placeholder = { Text(defaultNickname, color = Color(0xFFBDBDBD)) },
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
                    singleLine = true
                )
                
                Text(
                    "Default: $defaultNickname (will be used if left blank)",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }

        // Info note about relationship
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(Color(0xFFF0F9FF))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Info,
                    null,
                    tint = Color(0xFF0EA5E9),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "After creating the account, the senior can log in with the generated credentials. You can set your relationship and nickname later in settings.",
                    fontSize = 13.sp,
                    color = Color(0xFF0369A1),
                    lineHeight = 18.sp
                )
            }
        }

        // Create Button
        Button(
            onClick = { viewModel.createSenior {} },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            enabled = !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
            } else {
                Icon(Icons.Default.PersonAdd, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create Senior Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FormCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    errorMessage: String? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    suffix: String? = null,
    modifier: Modifier = Modifier,
    multiline: Boolean = false
) {
    Column(modifier) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value, onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFFBDBDBD)) },
            leadingIcon = leadingIcon?.let { { Icon(it, null, Modifier.size(20.dp), tint = Color(0xFF8B5CF6)) } },
            suffix = suffix?.let { { Text(it, fontSize = 14.sp, color = Color(0xFF9CA3AF)) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF9FAFB),
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedBorderColor = Color(0xFF8B5CF6)
            ),
            isError = errorMessage != null,
            singleLine = !multiline,
            maxLines = if (multiline) 3 else 1
        )
        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun GenderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFF8B5CF6) else Color(0xFFF9FAFB),
        border = BorderStroke(1.dp, if (selected) Color(0xFF8B5CF6) else Color(0xFFE5E7EB))
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun RelationshipChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFF8B5CF6) else Color(0xFFF9FAFB),
        border = BorderStroke(1.dp, if (selected) Color(0xFF8B5CF6) else Color(0xFFE5E7EB)),
        modifier = Modifier.width(140.dp)
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            textAlign = TextAlign.Center
        )
    }
}
