package com.alvin.pulselink.presentation.caregiver.senior

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.model.CaregiverRelation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import com.alvin.pulselink.domain.model.SeniorProfile
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.usecase.profile.CreateSeniorProfileUseCase
import com.alvin.pulselink.domain.usecase.profile.DeleteSeniorProfileUseCase
import com.alvin.pulselink.domain.usecase.profile.GetCreatedProfilesUseCase
import com.alvin.pulselink.domain.usecase.profile.GetManagedSeniorsUseCase
import com.alvin.pulselink.domain.usecase.profile.ManagedSeniorInfo
import com.alvin.pulselink.domain.usecase.profile.DeletionCheckResult
import com.alvin.pulselink.domain.usecase.relation.ManageRelationUseCase
import com.alvin.pulselink.util.AvatarHelper
import com.alvin.pulselink.util.QRCodeGenerator
import com.alvin.pulselink.util.RelationshipHelper
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 管理老人页面 ViewModel
 * 
 * 使用新的独立集合架构:
 * - senior_profiles: 老人资料
 * - caregiver_relations: 关系管理
 * - health_records: 健康记录
 */
@HiltViewModel
class ManageSeniorsViewModel @Inject constructor(
    private val getManagedSeniorsUseCase: GetManagedSeniorsUseCase,
    private val getCreatedProfilesUseCase: GetCreatedProfilesUseCase,
    private val createSeniorProfileUseCase: CreateSeniorProfileUseCase,
    private val deleteSeniorProfileUseCase: DeleteSeniorProfileUseCase,
    private val manageRelationUseCase: ManageRelationUseCase,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    companion object {
        private const val TAG = "ManageSeniorsVM"
    }

    private val _uiState = MutableStateFlow(ManageSeniorsUiState())
    val uiState: StateFlow<ManageSeniorsUiState> = _uiState.asStateFlow()
    
    private val _createFormState = MutableStateFlow(CreateSeniorFormState())
    val createFormState: StateFlow<CreateSeniorFormState> = _createFormState.asStateFlow()
    
    // Channel for one-time UI events (success, navigation)
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()
    
    // StateFlow for error dialog (must be confirmed by user)
    private val _errorDialog = MutableStateFlow<ErrorDialogState?>(null)
    val errorDialog: StateFlow<ErrorDialogState?> = _errorDialog.asStateFlow()
    
    /**
     * UI事件（一次性消息：成功提示、导航等）
     */
    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }
    
    /**
     * 错误对话框状态（必须用户确认）
     */
    data class ErrorDialogState(
        val title: String,
        val message: String
    )
    
    fun dismissErrorDialog() {
        _errorDialog.value = null
    }

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
    
    fun onCreatorNameChanged(name: String) {
        _createFormState.update { it.copy(creatorName = name, creatorNameError = null) }
    }
    
    fun onRelationshipChanged(relationship: String, gender: String) {
        // 自动设置默认 nickname
        val defaultNickname = RelationshipHelper.getDefaultAddressTitle(relationship, gender)
        _createFormState.update { it.copy(
            relationship = relationship,
            nickname = defaultNickname  // 自动填充默认值
        ) }
    }
    
    fun onNicknameChanged(nickname: String) {
        _createFormState.update { it.copy(nickname = nickname) }
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
        
        if (state.creatorName.isBlank()) {
            _createFormState.update { it.copy(creatorNameError = "请输入您的姓名") }
            hasError = true
        }
        
        if (hasError) return
        
        viewModelScope.launch {
            _createFormState.update { it.copy(isLoading = true) }
            
            val caregiverId = authRepository.getCurrentUid()
            if (caregiverId.isNullOrBlank()) {
                _createFormState.update { it.copy(isLoading = false) }
                _errorDialog.value = ErrorDialogState(
                    title = "Not Logged In",
                    message = "Please log in to create a senior account."
                )
                return@launch
            }
            
            // 根据年龄和性别自动选择头像类型
            val avatarType = AvatarHelper.getAvatarType(ageInt!!, state.gender)
            
            // 确保 nickname 有值，如果为空则使用默认值
            val finalNickname = state.nickname.ifBlank {
                RelationshipHelper.getDefaultAddressTitle(state.relationship, state.gender)
            }
            
            createSeniorProfileUseCase(
                name = state.name,
                age = ageInt,
                gender = state.gender,
                avatarType = avatarType,
                creatorId = caregiverId,
                customPassword = null,
                caregiverName = state.creatorName,
                relationship = state.relationship,
                nickname = finalNickname  // 确保使用有值的 nickname
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
                
                // 发送成功通知到Channel（Snackbar）
                _uiEvent.send(UiEvent.ShowSnackbar(
                    "Senior account created successfully!\nEmail: ${result.email}\nPassword: ${result.password}"
                ))
                
                onSuccess()
            }.onFailure { error ->
                _createFormState.update { it.copy(isLoading = false) }
                
                // 显示错误对话框（StateFlow）
                _errorDialog.value = ErrorDialogState(
                    title = "Creation Failed",
                    message = error.message ?: "Failed to create senior account. Please try again."
                )
            }
        }
    }

    /**
     * 检查是否可以删除老人（第一步）
     */
    suspend fun checkCanDeleteSenior(seniorProfileId: String): Result<DeletionCheckInfo> {
        return try {
            val requesterId = authRepository.getCurrentUid()
                ?: return Result.failure(Exception("未登录"))
            
            val checkResult = deleteSeniorProfileUseCase
                .checkDeletionAllowed(seniorProfileId, requesterId)
                .getOrThrow()
            
            Result.success(
                DeletionCheckInfo(
                    canDelete = checkResult.canDelete,
                    hasOtherCaregivers = checkResult.hasOtherCaregivers,
                    otherCaregiversText = checkResult.otherCaregiversInfo.joinToString("、"),
                    seniorName = checkResult.seniorName,
                    totalRelations = checkResult.totalActiveRelations
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check deletion", e)
            Result.failure(e)
        }
    }
    
    /**
     * 执行删除老人（用户确认后）
     */
    fun executeDeleteSenior(seniorProfileId: String) {
        // 立即关闭对话框
        _uiState.update { it.copy(
            showDeleteConfirmDialog = false,
            seniorToDelete = null
        ) }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val requesterId = authRepository.getCurrentUid()
            if (requesterId.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "未登录") }
                return@launch
            }
            
            deleteSeniorProfileUseCase(seniorProfileId, requesterId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvent.send(UiEvent.ShowSnackbar("Successfully deleted senior account"))
                    loadSeniors()
                }
                .onFailure { e ->
                    // 解析错误消息，提供友好提示
                    val (title, message) = when {
                        e.message?.contains("无法删除") == true && e.message?.contains("其他护理者") == true -> {
                            "Cannot Delete" to (e.message ?: "Unable to delete: There are other caregivers linked to this senior")
                        }
                        e.message?.contains("permission-denied") == true || e.message?.contains("只有创建者") == true ->
                            "Permission Denied" to "Only the creator can delete this account"
                        e.message?.contains("not-found") == true ->
                            "Not Found" to "Senior profile not found"
                        else -> "Delete Failed" to (e.message ?: "Failed to delete")
                    }
                    _uiState.update { it.copy(isLoading = false) }
                    _errorDialog.value = ErrorDialogState(title, message)
                }
        }
    }
    
    /**
     * 删除老人资料（旧方法，保留兼容）
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
                    _uiState.update { it.copy(isLoading = false) }
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
    /**
     * 显示 unlink 确认对话框
     */
    fun showUnlinkConfirmation(seniorInfo: ManagedSeniorInfo) {
        _uiState.update { 
            it.copy(
                showUnlinkConfirmDialog = true,
                seniorToUnlink = seniorInfo
            )
        }
    }
    
    /**
     * 取消 unlink
     */
    fun cancelUnlink() {
        _uiState.update { 
            it.copy(
                showUnlinkConfirmDialog = false,
                seniorToUnlink = null
            )
        }
    }
    
    /**
     * 执行 unlink（用户确认后）
     */
    fun executeUnlinkSenior(seniorProfileId: String) {
        // 立即关闭对话框
        _uiState.update { it.copy(
            showUnlinkConfirmDialog = false,
            seniorToUnlink = null
        ) }
        
        viewModelScope.launch {
            val caregiverId = authRepository.getCurrentUid() ?: return@launch
            _uiState.update { it.copy(isLoading = true) }
            
            val relationId = CaregiverRelation.generateId(caregiverId, seniorProfileId)
            
            manageRelationUseCase.removeRelation(relationId, caregiverId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvent.send(UiEvent.ShowSnackbar("Successfully unlinked from senior account"))
                    loadSeniors()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _errorDialog.value = ErrorDialogState(
                        title = "Unlink Failed",
                        message = e.message ?: "Failed to unlink from senior account. Please try again."
                    )
                }
        }
    }

    fun resetCreateForm() {
        _createFormState.value = CreateSeniorFormState()
    }
    
    /**
     * 获取老人的登录凭据用于显示二维码
     * 
     * 权限规则：只有账号创建者才能查看登录密码
     * 其他护理者无权查看密码，应该使用绑定二维码邀请功能
     */
    suspend fun getSeniorCredentials(seniorProfileId: String): Result<Pair<String, String>> {
        return try {
            val currentUserId = authRepository.getCurrentUid() 
                ?: return Result.failure(Exception("未登录"))
            
            // 1. 获取老人档案，检查是否为创建者
            val profileDoc = firestore.collection("senior_profiles")
                .document(seniorProfileId)
                .get()
                .await()
            
            if (!profileDoc.exists()) {
                return Result.failure(Exception("未找到老人档案"))
            }
            
            val creatorId = profileDoc.getString("creatorId") ?: ""
            val seniorName = profileDoc.getString("name") ?: "该老人"
            
            // 2. 检查当前用户是否为创建者
            if (currentUserId != creatorId) {
                return Result.failure(
                    Exception("您不是${seniorName}账号的创建者，无法查看登录凭证。\n请联系创建者获取登录信息。")
                )
            }
            
            // 3. 作为创建者，从自己的关系中获取密码
            val relationId = CaregiverRelation.generateId(currentUserId, seniorProfileId)
            val relationDoc = firestore.collection("caregiver_relations")
                .document(relationId)
                .get()
                .await()
            
            if (!relationDoc.exists()) {
                return Result.failure(Exception("未找到关系记录"))
            }
            
            val password = relationDoc.getString("virtualAccountPassword")
            
            if (password.isNullOrBlank()) {
                return Result.failure(Exception("密码数据缺失，请尝试重新创建账号"))
            }
            
            Result.success(Pair(seniorProfileId, password))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get senior credentials", e)
            Result.failure(e)
        }
    }
    
    /**
     * 检查当前用户是否为老人账号的创建者
     */
    suspend fun isCreatorOf(seniorProfileId: String): Boolean {
        return try {
            val currentUserId = authRepository.getCurrentUid() ?: return false
            
            val profileDoc = firestore.collection("senior_profiles")
                .document(seniorProfileId)
                .get()
                .await()
            
            val creatorId = profileDoc.getString("creatorId") ?: ""
            currentUserId == creatorId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check creator", e)
            false
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * 显示删除确认对话框
     */
    fun showDeleteConfirmation(seniorInfo: ManagedSeniorInfo) {
        _uiState.update { it.copy(
            showDeleteConfirmDialog = true,
            seniorToDelete = seniorInfo
        ) }
    }
    
    /**
     * 取消删除
     */
    fun cancelDelete() {
        _uiState.update { it.copy(
            showDeleteConfirmDialog = false,
            seniorToDelete = null
        ) }
    }
}

/**
 * 管理老人页面 UI 状态
 */
data class ManageSeniorsUiState(
    val createdSeniors: List<ManagedSeniorInfo> = emptyList(),
    val linkedSeniors: List<ManagedSeniorInfo> = emptyList(),
    val pendingRequests: List<CaregiverRelation> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,  // Keep for loading errors
    val currentUserId: String = "",
    val showDeleteConfirmDialog: Boolean = false,
    val seniorToDelete: ManagedSeniorInfo? = null,
    val showUnlinkConfirmDialog: Boolean = false,
    val seniorToUnlink: ManagedSeniorInfo? = null
)

/**
 * 创建老人表单状态
 */
data class CreateSeniorFormState(
    val name: String = "",
    val age: String = "",
    val gender: String = "Male",
    val creatorName: String = "",  // Caregiver's actual name
    val relationship: String = "Son",  // Caregiver's relationship to senior
    val nickname: String = "",  // Caregiver's nickname for senior (e.g., "Dad", "Mom")
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val nameError: String? = null,
    val ageError: String? = null,
    val creatorNameError: String? = null,
    // 创建成功后的账户信息
    val createdEmail: String = "",
    val createdPassword: String = "",
    val qrCodeData: String = "",
    val qrCodeBitmap: Bitmap? = null
)

/**
 * 删除检查信息（用于UI显示）
 */
data class DeletionCheckInfo(
    val canDelete: Boolean,              // 是否可以删除
    val hasOtherCaregivers: Boolean,     // 是否有其他护理者
    val otherCaregiversText: String,     // 其他护理者的文本描述
    val seniorName: String,              // 老人姓名
    val totalRelations: Int              // 总关系数
)
