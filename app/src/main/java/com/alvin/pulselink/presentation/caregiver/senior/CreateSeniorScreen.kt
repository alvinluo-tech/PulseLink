package com.alvin.pulselink.presentation.caregiver.senior

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.alvin.pulselink.domain.model.Senior
import kotlinx.coroutines.launch

/**
 * 创建老人账户页面
 * - 空状态：显示引导按钮
 * - 有数据：显示列表 + FAB
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSeniorScreen(
    viewModel: ManageSeniorsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val createState by viewModel.createSeniorState.collectAsStateWithLifecycle()
    val manageState by viewModel.manageSeniorsState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateForm by remember { mutableStateOf(false) }

    // 获取已创建的老人（使用 manageSeniorsState 中的 createdSeniors）
    val createdSeniors = manageState.createdSeniors

    // 加载老人列表
    LaunchedEffect(Unit) {
        viewModel.loadSeniors()
    }

    // 监听创建成功 - 不再自动弹出二维码对话框
    LaunchedEffect(createState.isSuccess) {
        if (createState.isSuccess) {
            showCreateForm = false  // 关闭创建表单
            viewModel.resetCreateForm()
            viewModel.loadSeniors()
            snackbarHostState.showSnackbar("老人账户创建成功！")
        }
    }
    
    // 监听错误
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
    
    // 二维码对话框不再在这里显示，改为在卡片上点击按钮显示
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
private fun SeniorsList(seniors: List<Senior>) {
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
        items(seniors) { SeniorCard(it) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SeniorCard(senior: Senior) {
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showQRCodeDialog by remember { mutableStateOf(false) }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
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
                Column(Modifier.weight(1f)) {
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
            
            // 二维码按钮
            Button(
                onClick = {
                    // 生成二维码数据
                    val qrData = """
                        {
                          "type": "pulselink_login",
                          "id": "${senior.id}",
                          "password": "${senior.password}"
                        }
                    """.trimIndent()
                    
                    // 生成二维码图片
                    qrCodeBitmap = com.alvin.pulselink.util.QRCodeGenerator.generateQRCode(qrData)
                    showQRCodeDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                enabled = senior.password.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("查看登录二维码", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    
    // 二维码对话框
    if (showQRCodeDialog) {
        QRCodeDialog(
            seniorId = senior.id,
            password = senior.password,
            qrCodeBitmap = qrCodeBitmap,
            onDismiss = { showQRCodeDialog = false }
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
        Icon(icon, null, Modifier.size(18.dp), tint = Color(0xFF8B5CF6))
        Column {
            Text(label, fontSize = 11.sp, color = Color(0xFF9CA3AF))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CreateForm(state: CreateSeniorUiState, viewModel: ManageSeniorsViewModel) {
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

        // Health History
        FormCard("Health History") {
            Text("Blood Pressure", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField("Systolic", state.systolicBP, viewModel::onSystolicBPChanged, "120", keyboardType = KeyboardType.Number, suffix = "mmHg", modifier = Modifier.weight(1f))
                FormField("Diastolic", state.diastolicBP, viewModel::onDiastolicBPChanged, "80", keyboardType = KeyboardType.Number, suffix = "mmHg", modifier = Modifier.weight(1f))
            }
            state.bpError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField("Heart Rate", state.heartRate, viewModel::onHeartRateChanged, "72", keyboardType = KeyboardType.Number, suffix = "bpm", leadingIcon = Icons.Default.Favorite, modifier = Modifier.weight(1f))
                FormField("Blood Sugar", state.bloodSugar, viewModel::onBloodSugarChanged, "5.5", keyboardType = KeyboardType.Decimal, suffix = "mmol/L", leadingIcon = Icons.Default.Bloodtype, modifier = Modifier.weight(1f))
            }
            
            FormField("Medical Conditions", state.medicalConditions, viewModel::onMedicalConditionsChanged, "Hypertension, Diabetes", leadingIcon = Icons.Default.MedicalServices, multiline = true)
            FormField("Medications", state.medications, viewModel::onMedicationsChanged, "Aspirin, Metformin", leadingIcon = Icons.Default.Medication, multiline = true)
            FormField("Allergies", state.allergies, viewModel::onAllergiesChanged, "Penicillin, Pollen", leadingIcon = Icons.Default.Warning, multiline = true)
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
