package com.alvin.pulselink.presentation.caregiver.senior

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.CaregiverRelation
import com.alvin.pulselink.domain.model.SeniorProfile
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.usecase.profile.CreateSeniorProfileUseCase
import com.alvin.pulselink.domain.usecase.profile.DeleteSeniorProfileUseCase
import com.alvin.pulselink.domain.usecase.profile.GetCreatedProfilesUseCase
import com.alvin.pulselink.domain.usecase.profile.GetManagedSeniorsUseCase
import com.alvin.pulselink.domain.usecase.profile.ManagedSeniorInfo
import com.alvin.pulselink.domain.usecase.relation.ManageRelationUseCase
import com.alvin.pulselink.util.QRCodeGenerator
import com.alvin.pulselink.util.AvatarHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 管理老人页面 ViewModel (新架构 V2)
 * 
 * 使用新的独立集合架构:
 * - senior_profiles: 老人资料
 * - caregiver_relations: 关系管理
 * - health_records: 健康记录
 */
@HiltViewModel
class ManageSeniorsV2ViewModel @Inject constructor(
    private val getManagedSeniorsUseCase: GetManagedSeniorsUseCase,
    private val getCreatedProfilesUseCase: GetCreatedProfilesUseCase,
    private val createSeniorProfileUseCase: CreateSeniorProfileUseCase,
    private val deleteSeniorProfileUseCase: DeleteSeniorProfileUseCase,
    private val manageRelationUseCase: ManageRelationUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ManageSeniorsV2VM"
    }

    private val _uiState = MutableStateFlow(ManageSeniorsV2UiState())
    val uiState: StateFlow<ManageSeniorsV2UiState> = _uiState.asStateFlow()
    
    private val _createFormState = MutableStateFlow(CreateSeniorFormState())
    val createFormState: StateFlow<CreateSeniorFormState> = _createFormState.asStateFlow()

    init {
        loadSeniors()
    }

    /**
     * 加载所有管理的老人
     */
    fun loadSeniors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val caregiverId = authRepository.getCurrentUid()
            if (caregiverId.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "未登录") }
                return@launch
            }

            Log.d(TAG, "Loading seniors for caregiver: $caregiverId")

            try {
                // 1. 获取管理的老人（已批准状态）
                val managedResult = getManagedSeniorsUseCase(caregiverId)
                
                // 2. 获取创建的老人
                val createdResult = getCreatedProfilesUseCase(caregiverId)
                
                // 3. 获取待处理的关系请求
                // TODO: 添加获取 pending 请求的逻辑

                if (managedResult.isSuccess && createdResult.isSuccess) {
                    val managedSeniors = managedResult.getOrNull() ?: emptyList()
                    val createdProfiles = createdResult.getOrNull() ?: emptyList()
                    
                    Log.d(TAG, "Managed seniors: ${managedSeniors.size}")
                    Log.d(TAG, "Created profiles: ${createdProfiles.size}")
                    
                    // 分类：创建的 vs 仅关联的
                    val createdIds = createdProfiles.map { it.id }.toSet()
                    val linkedOnly = managedSeniors.filter { it.profile.id !in createdIds }
                    val created = managedSeniors.filter { it.profile.id in createdIds }
                    
                    _uiState.update {
                        it.copy(
                            createdSeniors = created,
                            linkedSeniors = linkedOnly,
                            isLoading = false,
                            currentUserId = caregiverId
                        )
                    }
                } else {
                    val error = managedResult.exceptionOrNull() ?: createdResult.exceptionOrNull()
                    Log.e(TAG, "Failed to load seniors: ${error?.message}", error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error?.message ?: "加载失败"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading seniors: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "加载失败"
                    )
                }
            }
        }
    }

    // ========== 创建表单处理 ==========
    
    fun onNameChanged(name: String) {
        _createFormState.update { it.copy(name = name, nameError = null) }
    }

    fun onAgeChanged(age: String) {
        _createFormState.update { it.copy(age = age, ageError = null) }
    }

    fun onGenderChanged(gender: String) {
        _createFormState.update { it.copy(gender = gender) }
    }

    /**
     * 创建老人资料
     */
    fun createSenior(onSuccess: () -> Unit) {
        val state = _createFormState.value
        
        // 验证
        var hasError = false
        
        if (state.name.isBlank()) {
            _createFormState.update { it.copy(nameError = "姓名不能为空") }
            hasError = true
        }
        
        val ageInt = state.age.toIntOrNull()
        if (ageInt == null || ageInt <= 0) {
            _createFormState.update { it.copy(ageError = "请输入有效年龄") }
            hasError = true
        }
        
        if (hasError) return
        
        viewModelScope.launch {
            _createFormState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val caregiverId = authRepository.getCurrentUid()
            if (caregiverId.isNullOrBlank()) {
                _createFormState.update { it.copy(isLoading = false, errorMessage = "未登录") }
                return@launch
            }
            
            // 根据年龄和性别自动选择头像类型
            val avatarType = AvatarHelper.getAvatarType(ageInt!!, state.gender)
            
            createSeniorProfileUseCase(
                name = state.name,
                age = ageInt,
                gender = state.gender,
                avatarType = avatarType,
                creatorId = caregiverId,
                customPassword = null
            ).onSuccess { result ->
                // 生成二维码
                val qrBitmap = QRCodeGenerator.generateQRCode(result.qrCodeData)
                
                _createFormState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        createdEmail = result.email,
                        createdPassword = result.password,
                        qrCodeData = result.qrCodeData,
                        qrCodeBitmap = qrBitmap
                    )
                }
                
                // 刷新列表
                loadSeniors()
                
                // 提示消息
                _uiState.update { 
                    it.copy(successMessage = "老人账户创建成功！\n邮箱: ${result.email}\n密码: ${result.password}") 
                }
                
                onSuccess()
            }.onFailure { error ->
                _createFormState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "创建失败"
                    )
                }
            }
        }
    }

    /**
     * 删除老人资料
     */
    fun deleteSenior(seniorProfileId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val requesterId = authRepository.getCurrentUid()
            if (requesterId.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "未登录") }
                return@launch
            }
            
            deleteSeniorProfileUseCase(seniorProfileId, requesterId)
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "已删除老人账户", isLoading = false) }
                    loadSeniors()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "删除失败", isLoading = false) }
                }
        }
    }

    /**
     * 解除关系
     */
    fun unlinkSenior(seniorProfileId: String) {
        viewModelScope.launch {
            val caregiverId = authRepository.getCurrentUid() ?: return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val relationId = CaregiverRelation.generateId(caregiverId, seniorProfileId)
            
            manageRelationUseCase.removeRelation(relationId, caregiverId)
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "已解除绑定", isLoading = false) }
                    loadSeniors()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "解除绑定失败", isLoading = false) }
                }
        }
    }

    fun resetCreateForm() {
        _createFormState.value = CreateSeniorFormState()
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
        _createFormState.update { it.copy(errorMessage = null) }
    }
}

/**
 * 管理老人页面 UI 状态 (V2)
 */
data class ManageSeniorsV2UiState(
    val createdSeniors: List<ManagedSeniorInfo> = emptyList(),
    val linkedSeniors: List<ManagedSeniorInfo> = emptyList(),
    val pendingRequests: List<CaregiverRelation> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentUserId: String = ""
)

/**
 * 创建老人表单状态
 */
data class CreateSeniorFormState(
    val name: String = "",
    val age: String = "",
    val gender: String = "男",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val nameError: String? = null,
    val ageError: String? = null,
    // 创建成功后的账户信息
    val createdEmail: String = "",
    val createdPassword: String = "",
    val qrCodeData: String = "",
    val qrCodeBitmap: Bitmap? = null
)
