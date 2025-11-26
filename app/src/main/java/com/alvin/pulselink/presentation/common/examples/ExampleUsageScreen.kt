package com.alvin.pulselink.presentation.common.examples

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.presentation.common.base.BaseViewModel
import com.alvin.pulselink.presentation.common.components.PulseLinkScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI 反馈系统使用示例页面
 * 
 * 展示所有反馈类型和使用场景
 * 可作为参考模板
 */

@HiltViewModel
class ExampleViewModel @Inject constructor() : BaseViewModel() {
    
    // ========== Caregiver 端示例 ==========
    
    fun caregiverSuccessExample() {
        showSuccess("数据保存成功！")
    }
    
    fun caregiverErrorExample() {
        showError("网络连接失败，请检查网络设置", actionLabel = "重试")
    }
    
    fun caregiverWarningExample() {
        showWarning("此操作无法撤销，请谨慎操作")
    }
    
    fun caregiverInfoExample() {
        showInfo("数据已自动同步到云端")
    }
    
    fun caregiverLoadingExample() {
        viewModelScope.launch {
            showLoading("正在上传数据...")
            delay(2000)
            hideLoading()
            showSuccess("上传完成")
        }
    }
    
    // ========== Senior 端示例 ==========
    
    fun seniorMedicationExample() {
        viewModelScope.launch {
            showLoading("正在记录...")
            delay(1000)
            hideLoading()
            showHeroSuccess("吃药打卡成功！\n按时服药身体好")
        }
    }
    
    fun seniorVoiceInputExample() {
        viewModelScope.launch {
            showLoading("正在识别语音...")
            delay(2000)
            hideLoading()
            showHeroSuccess("已收到您的消息")
        }
    }
    
    fun seniorErrorExample() {
        viewModelScope.launch {
            showLoading("正在处理...")
            delay(1500)
            hideLoading()
            showHeroError("操作失败\n请稍后重试")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExampleUsageScreen(
    viewModel: ExampleViewModel,
    onNavigateBack: () -> Unit = {}
) {
    PulseLinkScaffold(
        uiEventFlow = viewModel.uiEvent,
        topBar = {
            TopAppBar(
                title = { Text("状态反馈系统示例") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Caregiver 端示例
            Text(
                text = "Caregiver 端反馈示例",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Button(
                onClick = { viewModel.caregiverSuccessExample() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("✓ 成功反馈（绿色）")
            }
            
            Button(
                onClick = { viewModel.caregiverErrorExample() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("✗ 错误反馈（红色 + 重试按钮）")
            }
            
            Button(
                onClick = { viewModel.caregiverWarningExample() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("⚠ 警告反馈（橙色）")
            }
            
            Button(
                onClick = { viewModel.caregiverInfoExample() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ⓘ 信息反馈（蓝色）")
            }
            
            Button(
                onClick = { viewModel.caregiverLoadingExample() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⌛ 加载反馈示例")
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Senior 端示例
            Text(
                text = "Senior 端反馈示例",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Text(
                text = "以下为全屏中央大卡片反馈，专为老人设计",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(
                onClick = { viewModel.seniorMedicationExample() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("💊 吃药打卡成功（英雄式）")
            }
            
            Button(
                onClick = { viewModel.seniorVoiceInputExample() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("🎤 语音输入成功（英雄式）")
            }
            
            Button(
                onClick = { viewModel.seniorErrorExample() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("✗ 操作失败（英雄式）")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 说明卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "设计原则",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "• Caregiver：简洁高效的胶囊式提示\n" +
                               "• Senior：醒目的全屏中央反馈\n" +
                               "• 所有文字 ≥ 16sp，确保可读性\n" +
                               "• 图标大而清晰，色彩分明",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewExampleUsageScreen() {
    MaterialTheme {
        // 注意：Preview 中无法注入 HiltViewModel
        // 实际使用时通过 hiltViewModel() 获取
        ExampleUsageScreen(
            viewModel = ExampleViewModel()
        )
    }
}
