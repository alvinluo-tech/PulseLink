package com.alvin.pulselink.presentation.caregiver.senior

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.BloodPressureRecord
import com.alvin.pulselink.domain.model.HealthHistory
import com.alvin.pulselink.domain.model.Senior
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.SeniorRepository
import com.alvin.pulselink.domain.usecase.CreateSeniorUseCase
import com.alvin.pulselink.util.QRCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 管理老人页面 ViewModel
 */
@HiltViewModel
class ManageSeniorsViewModel @Inject constructor(
    private val seniorRepository: SeniorRepository,
    private val createSeniorUseCase: CreateSeniorUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _manageSeniorsState = MutableStateFlow(ManageSeniorsUiState())
    val manageSeniorsState: StateFlow<ManageSeniorsUiState> = _manageSeniorsState.asStateFlow()
    
    private val _createSeniorState = MutableStateFlow(CreateSeniorUiState())
    val createSeniorState: StateFlow<CreateSeniorUiState> = _createSeniorState.asStateFlow()

    init {
        loadSeniors()
    }

    fun loadSeniors() {
        viewModelScope.launch {
            _manageSeniorsState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            val caregiverId = authRepository.getCurrentUid() ?: ""

            // 获取所有绑定的（包含创建者）
            seniorRepository.getSeniorsByCaregiver(caregiverId)
                .onSuccess { allSeniors ->
                    val created = allSeniors.filter { it.creatorId == caregiverId }
                    val linkedOnly = allSeniors.filter { it.creatorId != caregiverId }

                    _manageSeniorsState.update {
                        it.copy(
                            createdSeniors = created,
                            linkedSeniors = linkedOnly,
                            isLoading = false,
                            currentUserId = caregiverId
                        )
                    }
                }
                .onFailure { error ->
                    _manageSeniorsState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load seniors",
                            currentUserId = caregiverId
                        )
                    }
                }
        }
    }

    // Create Senior Form Handlers
    fun onNameChanged(name: String) {
        _createSeniorState.update { it.copy(name = name, nameError = null) }
    }

    fun onAgeChanged(age: String) {
        _createSeniorState.update { it.copy(age = age, ageError = null) }
    }

    fun onGenderChanged(gender: String) {
        _createSeniorState.update { it.copy(gender = gender) }
    }

    fun onSystolicBPChanged(value: String) {
        _createSeniorState.update { it.copy(systolicBP = value, bpError = null) }
    }

    fun onDiastolicBPChanged(value: String) {
        _createSeniorState.update { it.copy(diastolicBP = value, bpError = null) }
    }

    fun onHeartRateChanged(value: String) {
        _createSeniorState.update { it.copy(heartRate = value) }
    }

    fun onBloodSugarChanged(value: String) {
        _createSeniorState.update { it.copy(bloodSugar = value) }
    }

    fun onMedicalConditionsChanged(value: String) {
        _createSeniorState.update { it.copy(medicalConditions = value) }
    }

    fun onMedicationsChanged(value: String) {
        _createSeniorState.update { it.copy(medications = value) }
    }

    fun onAllergiesChanged(value: String) {
        _createSeniorState.update { it.copy(allergies = value) }
    }

    fun createSenior(onSuccess: () -> Unit) {
        val state = _createSeniorState.value
        
        // Validate
        var hasError = false
        
        if (state.name.isBlank()) {
            _createSeniorState.update { it.copy(nameError = "Name is required") }
            hasError = true
        }
        
        val ageInt = state.age.toIntOrNull()
        if (ageInt == null || ageInt <= 0) {
            _createSeniorState.update { it.copy(ageError = "Valid age is required") }
            hasError = true
        }
        
        if (hasError) return
        
        viewModelScope.launch {
            _createSeniorState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val caregiverId = authRepository.getCurrentUid() ?: ""
            
            // Parse blood pressure
            val bloodPressure = if (state.systolicBP.isNotBlank() && state.diastolicBP.isNotBlank()) {
                val systolic = state.systolicBP.toIntOrNull()
                val diastolic = state.diastolicBP.toIntOrNull()
                
                if (systolic != null && diastolic != null) {
                    BloodPressureRecord(systolic = systolic, diastolic = diastolic)
                } else null
            } else null
            
            // Parse lists (comma-separated)
            val medicalConditions = state.medicalConditions
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            
            val medications = state.medications
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            
            val allergies = state.allergies
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            
            val senior = Senior(
                name = state.name,
                age = ageInt!!,
                gender = state.gender,
                caregiverIds = listOf(caregiverId),
                creatorId = caregiverId,
                healthHistory = HealthHistory(
                    bloodPressure = bloodPressure,
                    heartRate = state.heartRate.toIntOrNull(),
                    bloodSugar = state.bloodSugar.toDoubleOrNull(),
                    medicalConditions = medicalConditions,
                    medications = medications,
                    allergies = allergies
                )
            )
            
            createSeniorUseCase(senior)
                .onSuccess { result ->
                    // 生成二维码图片
                    val qrBitmap = QRCodeGenerator.generateQRCode(result.qrCodeData)
                    
                    _createSeniorState.update { it.copy(
                        isLoading = false,
                        isSuccess = true,
                        createdAccountEmail = result.email,
                        createdAccountPassword = result.password,
                        qrCodeData = result.qrCodeData,
                        qrCodeBitmap = qrBitmap
                    ) }
                    
                    // Reload seniors list
                    loadSeniors()
                    
                    // 创建成功提示消息（包含账户信息）
                    _manageSeniorsState.update { 
                        it.copy(
                            successMessage = "老人账户创建成功！\n邮箱: ${result.email}\n密码: ${result.password}"
                        ) 
                    }

                    // 不自动重置表单，以便显示二维码
                    // 由 UI 在用户确认后手动调用 resetCreateForm()
                    
                    onSuccess()
                }
                .onFailure { error ->
                    _createSeniorState.update { it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to create senior"
                    ) }
                }
        }
    }

    fun resetCreateForm() {
        _createSeniorState.value = CreateSeniorUiState()
    }

    fun clearSuccessMessage() {
        _manageSeniorsState.update { it.copy(successMessage = null) }
    }

    fun clearErrorMessage() {
        _manageSeniorsState.update { it.copy(errorMessage = null) }
        _createSeniorState.update { it.copy(errorMessage = null) }
    }

    /**
     * 加载指定老人的信息到创建表单，用于编辑模式
     */
    fun loadSeniorForEdit(seniorId: String) {
        viewModelScope.launch {
            _createSeniorState.update { it.copy(isLoading = true, errorMessage = null) }
            seniorRepository.getSeniorById(seniorId)
                .onSuccess { senior ->
                    _createSeniorState.update {
                        it.copy(
                            isLoading = false,
                            name = senior.name,
                            age = senior.age.toString(),
                            gender = senior.gender,
                            systolicBP = senior.healthHistory.bloodPressure?.systolic?.toString() ?: "",
                            diastolicBP = senior.healthHistory.bloodPressure?.diastolic?.toString() ?: "",
                            heartRate = senior.healthHistory.heartRate?.toString() ?: "",
                            bloodSugar = senior.healthHistory.bloodSugar?.toString() ?: "",
                            medicalConditions = senior.healthHistory.medicalConditions.joinToString(", "),
                            medications = senior.healthHistory.medications.joinToString(", "),
                            allergies = senior.healthHistory.allergies.joinToString(", ")
                        )
                    }
                }
                .onFailure { e ->
                    _createSeniorState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load senior") }
                }
        }
    }

    /**
     * 更新老人信息（仅创建者可操作）
     */
    fun updateSenior(seniorId: String, onSuccess: () -> Unit) {
        val state = _createSeniorState.value
        // 基本校验复用创建逻辑
        var hasError = false
        if (state.name.isBlank()) {
            _createSeniorState.update { it.copy(nameError = "Name is required") }
            hasError = true
        }
        val ageInt = state.age.toIntOrNull()
        if (ageInt == null || ageInt <= 0) {
            _createSeniorState.update { it.copy(ageError = "Valid age is required") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _createSeniorState.update { it.copy(isLoading = true, errorMessage = null) }

            // 读取原始记录确保保留不可变字段
            seniorRepository.getSeniorById(seniorId)
                .onSuccess { original ->
                    // 权限校验：仅创建者可编辑
                    val currentUid = authRepository.getCurrentUid()
                    if (currentUid == null || original.creatorId != currentUid) {
                        _createSeniorState.update { it.copy(isLoading = false, errorMessage = "无权限编辑该老人账户") }
                        return@onSuccess
                    }

                    val bloodPressure = if (state.systolicBP.isNotBlank() && state.diastolicBP.isNotBlank()) {
                        val systolic = state.systolicBP.toIntOrNull()
                        val diastolic = state.diastolicBP.toIntOrNull()
                        if (systolic != null && diastolic != null) BloodPressureRecord(systolic, diastolic) else null
                    } else null

                    val medicalConditions = state.medicalConditions.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val medications = state.medications.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val allergies = state.allergies.split(",").map { it.trim() }.filter { it.isNotBlank() }

                    val updated = original.copy(
                        name = state.name,
                        age = ageInt!!,
                        gender = state.gender,
                        healthHistory = HealthHistory(
                            bloodPressure = bloodPressure,
                            heartRate = state.heartRate.toIntOrNull(),
                            bloodSugar = state.bloodSugar.toDoubleOrNull(),
                            medicalConditions = medicalConditions,
                            medications = medications,
                            allergies = allergies
                        )
                    )

                    seniorRepository.updateSenior(updated)
                        .onSuccess {
                            _createSeniorState.update { it.copy(isLoading = false) }
                            // 刷新列表与提示
                            loadSeniors()
                            _manageSeniorsState.update { it.copy(successMessage = "老人信息已更新") }
                            onSuccess()
                        }
                        .onFailure { e ->
                            _createSeniorState.update { it.copy(isLoading = false, errorMessage = e.message ?: "更新失败") }
                        }
                }
                .onFailure { e ->
                    _createSeniorState.update { it.copy(isLoading = false, errorMessage = e.message ?: "未找到老人账户") }
                }
        }
    }
    /**
     * 解除与某个老人的绑定（不影响创建者与其他护理者）
     */
    fun unlinkSenior(seniorId: String) {
        viewModelScope.launch {
            val caregiverId = authRepository.getCurrentUid() ?: return@launch
            _manageSeniorsState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            seniorRepository.getSeniorById(seniorId)
                .onSuccess { senior ->
                    val updated = senior.copy(caregiverIds = senior.caregiverIds.filter { it != caregiverId })
                    seniorRepository.updateSenior(updated)
                        .onSuccess {
                            _manageSeniorsState.update { it.copy(successMessage = "已解除绑定", isLoading = false) }
                            loadSeniors()
                        }
                        .onFailure { e ->
                            _manageSeniorsState.update { it.copy(errorMessage = e.message ?: "解除绑定失败", isLoading = false) }
                        }
                }
                .onFailure { e ->
                    _manageSeniorsState.update { it.copy(errorMessage = e.message ?: "未找到老人账户", isLoading = false) }
                }
        }
    }

    /**
     * 删除自己创建的老人账户（需具备创建者权限）
     */
    fun deleteSenior(seniorId: String) {
        viewModelScope.launch {
            _manageSeniorsState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            seniorRepository.deleteSenior(seniorId)
                .onSuccess {
                    _manageSeniorsState.update { it.copy(successMessage = "已删除老人账户", isLoading = false) }
                    loadSeniors()
                }
                .onFailure { e ->
                    _manageSeniorsState.update { it.copy(errorMessage = e.message ?: "删除失败", isLoading = false) }
                }
        }
    }
    /**
     * 加载当前用户创建的老人列表，用于CreateSeniorScreen展示
     */
    fun loadCreatedSeniorsForCreateScreen() {
        viewModelScope.launch {
            val caregiverId = authRepository.getCurrentUid() ?: return@launch
            seniorRepository.getSeniorsByCreator(caregiverId)
                .onSuccess { list ->
                    _createSeniorState.update { it.copy(createdSeniors = list) }
                }
                .onFailure {
                    _createSeniorState.update { it.copy(createdSeniors = emptyList()) }
                }
        }
    }
}
