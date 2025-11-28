package com.alvin.pulselink.presentation.caregiver.seniordetail.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvin.pulselink.domain.model.*
import com.alvin.pulselink.presentation.caregiver.seniordetail.viewmodels.RemindersViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Add/Edit Medication Screen
 * 添加或编辑用药提醒界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationScreen(
    seniorId: String,
    reminderId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("片") }
    var instruction by remember { mutableStateOf(IntakeInstruction.AFTER_MEAL) }
    var frequency by remember { mutableStateOf(FrequencyType.DAILY) }
    var specificDays by remember { mutableStateOf(setOf<Int>()) }
    var intervalDays by remember { mutableStateOf("1") }
    var timeSlots by remember { mutableStateOf(listOf("08:00")) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var currentStock by remember { mutableStateOf("30") }
    var lowStockThreshold by remember { mutableStateOf("10") }
    var enableStockAlert by remember { mutableStateOf(true) }
    var colorHex by remember { mutableStateOf("#8B5CF6") }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var editingTimeIndex by remember { mutableStateOf<Int?>(null) }

    // 监听成功事件后再导航返回
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            if (event is com.alvin.pulselink.presentation.common.state.UiEvent.ShowSnackbar &&
                event.type == com.alvin.pulselink.presentation.common.state.SnackbarType.SUCCESS) {
                onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (reminderId == null) "添加用药提醒" else "编辑用药提醒",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF111827)
                )
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 基本信息
            item {
                SectionCard(title = "基本信息") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("药品名称 *") },
                            placeholder = { Text("例如：阿司匹林") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            label = { Text("昵称（可选）") },
                            placeholder = { Text("例如：降压药") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = dosage,
                                onValueChange = { dosage = it },
                                label = { Text("剂量 *") },
                                placeholder = { Text("例如：1") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )

                            var expandedUnit by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expandedUnit,
                                onExpandedChange = { expandedUnit = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = unit,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("单位") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnit)
                                    },
                                    modifier = Modifier.menuAnchor()
                                )

                                ExposedDropdownMenu(
                                    expanded = expandedUnit,
                                    onDismissRequest = { expandedUnit = false }
                                ) {
                                    listOf("片", "粒", "ml", "mg", "g", "滴", "喷").forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text(u) },
                                            onClick = {
                                                unit = u
                                                expandedUnit = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 服用说明
                        var expandedInstruction by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedInstruction,
                            onExpandedChange = { expandedInstruction = it }
                        ) {
                            OutlinedTextField(
                                value = getInstructionText(instruction),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("服用说明") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedInstruction)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = expandedInstruction,
                                onDismissRequest = { expandedInstruction = false }
                            ) {
                                IntakeInstruction.values().forEach { inst ->
                                    DropdownMenuItem(
                                        text = { Text(getInstructionText(inst)) },
                                        onClick = {
                                            instruction = inst
                                            expandedInstruction = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 服药时间
            item {
                SectionCard(title = "服药时间") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        timeSlots.forEachIndexed { index, time ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = time,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("时间 ${index + 1}") },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            editingTimeIndex = index
                                            showTimePickerDialog = true
                                        }) {
                                            Icon(Icons.Default.Schedule, contentDescription = "选择时间")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                if (timeSlots.size > 1) {
                                    IconButton(onClick = {
                                        timeSlots = timeSlots.filterIndexed { i, _ -> i != index }
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "删除",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { timeSlots = timeSlots + "08:00" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEFF6FF),
                                contentColor = Color(0xFF3B82F6)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("添加服药时间")
                        }
                    }
                }
            }

            // 频率设置
            item {
                SectionCard(title = "频率设置") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FrequencyType.values().forEach { freq ->
                                FilterChip(
                                    selected = frequency == freq,
                                    onClick = { frequency = freq },
                                    label = { Text(getFrequencyText(freq)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        when (frequency) {
                            FrequencyType.SPECIFIC_DAYS -> {
                                Text("选择星期", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(1 to "一", 2 to "二", 3 to "三", 4 to "四", 5 to "五", 6 to "六", 7 to "日").forEach { (day, label) ->
                                        FilterChip(
                                            selected = specificDays.contains(day),
                                            onClick = {
                                                specificDays = if (specificDays.contains(day)) {
                                                    specificDays - day
                                                } else {
                                                    specificDays + day
                                                }
                                            },
                                            label = { Text(label, fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            FrequencyType.INTERVAL -> {
                                OutlinedTextField(
                                    value = intervalDays,
                                    onValueChange = { intervalDays = it },
                                    label = { Text("间隔天数") },
                                    placeholder = { Text("例如：2（表示每两天一次）") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }

            // 库存管理
            item {
                SectionCard(title = "库存管理") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = currentStock,
                            onValueChange = { currentStock = it },
                            label = { Text("当前库存") },
                            placeholder = { Text("例如：30") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                Text(unit, color = Color.Gray, fontSize = 14.sp)
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("库存预警", fontSize = 14.sp)
                            Switch(
                                checked = enableStockAlert,
                                onCheckedChange = { enableStockAlert = it }
                            )
                        }

                        if (enableStockAlert) {
                            OutlinedTextField(
                                value = lowStockThreshold,
                                onValueChange = { lowStockThreshold = it },
                                label = { Text("预警阈值") },
                                placeholder = { Text("例如：10") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                trailingIcon = {
                                    Text(unit, color = Color.Gray, fontSize = 14.sp)
                                }
                            )
                        }
                    }
                }
            }

            // 提交按钮
            item {
                Button(
                    onClick = {
                        // Get actual user ID from Firebase Auth
                        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        
                        val reminder = MedicationReminder(
                            id = reminderId ?: "",  // 空字符串会在 Repository 中生成新 ID
                            seniorId = seniorId,
                            name = name,
                            nickname = nickname.takeIf { it.isNotBlank() },
                            imageUrl = null,
                            iconType = MedicationIconType.PILL,
                            colorHex = colorHex,
                            dosage = dosage.toDoubleOrNull() ?: 1.0,
                            unit = unit,
                            instruction = instruction,
                            frequency = frequency,
                            specificWeekDays = specificDays.toList(),
                            intervalDays = intervalDays.toIntOrNull() ?: 1,
                            timeSlots = timeSlots,
                            startDate = startDate.toEpochDay(),
                            endDate = endDate?.toEpochDay(),
                            currentStock = currentStock.toIntOrNull() ?: 0,
                            lowStockThreshold = lowStockThreshold.toIntOrNull() ?: 0,
                            enableStockAlert = enableStockAlert,
                            status = ReminderStatus.ACTIVE,
                            createdBy = currentUserId,
                            createdAt = System.currentTimeMillis()
                        )

                        if (reminderId == null) {
                            viewModel.createReminder(reminder)
                            // 等待创建成功后再返回
                        } else {
                            viewModel.updateReminder(reminder)
                            // 等待更新成功后再返回
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B5CF6)
                    ),
                    enabled = name.isNotBlank() && dosage.isNotBlank()
                ) {
                    Text(
                        text = if (reminderId == null) "创建提醒" else "保存修改",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Time Picker Dialog
    if (showTimePickerDialog && editingTimeIndex != null) {
        TimePickerDialog(
            initialTime = timeSlots[editingTimeIndex!!],
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { time ->
                timeSlots = timeSlots.toMutableList().apply {
                    set(editingTimeIndex!!, time)
                }
                showTimePickerDialog = false
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun TimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var hour by remember { mutableStateOf(initialTime.split(":")[0].toIntOrNull() ?: 8) }
    var minute by remember { mutableStateOf(initialTime.split(":")[1].toIntOrNull() ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour Picker
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { hour = (hour + 1) % 24 }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                    }
                    Text(
                        text = hour.toString().padStart(2, '0'),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { hour = if (hour == 0) 23 else hour - 1 }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                }

                Text(":", fontSize = 32.sp, modifier = Modifier.padding(horizontal = 16.dp))

                // Minute Picker
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { minute = (minute + 5) % 60 }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                    }
                    Text(
                        text = minute.toString().padStart(2, '0'),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { minute = if (minute < 5) 55 else minute - 5 }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val timeString = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
                    onConfirm(timeString)
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun getInstructionText(instruction: IntakeInstruction): String {
    return when (instruction) {
        IntakeInstruction.NONE -> "无特殊要求"
        IntakeInstruction.BEFORE_MEAL -> "饭前服用"
        IntakeInstruction.AFTER_MEAL -> "饭后服用"
        IntakeInstruction.WITH_FOOD -> "随餐服用"
        IntakeInstruction.BEFORE_SLEEP -> "睡前服用"
    }
}

private fun getFrequencyText(frequency: FrequencyType): String {
    return when (frequency) {
        FrequencyType.DAILY -> "每天"
        FrequencyType.SPECIFIC_DAYS -> "特定日期"
        FrequencyType.INTERVAL -> "间隔天数"
    }
}
