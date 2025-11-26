package com.alvin.pulselink.presentation.caregiver.senior

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.core.constants.AuthConstants
import com.alvin.pulselink.domain.model.CaregiverRelation
import com.alvin.pulselink.domain.model.SeniorProfile
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import com.alvin.pulselink.domain.repository.SeniorProfileRepository
import com.alvin.pulselink.util.RelationshipHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Link Senior Account ViewModel
 */
@HiltViewModel
class LinkSeniorViewModel @Inject constructor(
    private val seniorProfileRepository: SeniorProfileRepository,
    private val caregiverRelationRepository: CaregiverRelationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LinkSeniorUiState())
    val uiState: StateFlow<LinkSeniorUiState> = _uiState.asStateFlow()
    
    // Channel for one-time events (success Snackbar)
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()
    
    // StateFlow for error dialogs (persists across config changes)
    private val _errorDialog = MutableStateFlow<ErrorDialogState?>(null)
    val errorDialogState: StateFlow<ErrorDialogState?> = _errorDialog.asStateFlow()

    init {
        loadLinkedSeniors()
    }

    fun loadLinkedSeniors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingList = true) }
            
            val caregiverId = authRepository.getCurrentUid() ?: ""
            Log.d("LinkSeniorVM", "Loading linked seniors for caregiver: $caregiverId")
            
            try {
                // 获取活跃的关系
                val relationsResult = caregiverRelationRepository.getActiveRelationsByCaregiver(caregiverId)
                
                if (relationsResult.isSuccess) {
                    val relations = relationsResult.getOrNull() ?: emptyList()
                    
                    Log.d("LinkSeniorVM", "Active relations: ${relations.size}")
                    
                    // 获取对应的老人档案（排除自己创建的）
                    val linkedSeniors = mutableListOf<SeniorProfile>()
                    for (relation in relations) {
                        val profileResult = seniorProfileRepository.getProfileById(relation.seniorId)
                        profileResult.onSuccess { profile ->
                            if (profile.creatorId != caregiverId) {
                                linkedSeniors.add(profile)
                            }
                        }
                    }
                    
                    Log.d("LinkSeniorVM", "Linked seniors (excluding created): ${linkedSeniors.size}")
                    
                    _uiState.update { it.copy(
                        linkedSeniors = linkedSeniors,
                        isLoadingList = false
                    ) }
                } else {
                    val error = relationsResult.exceptionOrNull()
                    Log.e("LinkSeniorVM", "Failed to load linked seniors: ${error?.message}", error)
                    _uiState.update { it.copy(
                        isLoadingList = false,
                        errorMessage = error?.message ?: "Failed to load linked seniors"
                    ) }
                }
            } catch (e: Exception) {
                Log.e("LinkSeniorVM", "Exception loading linked seniors: ${e.message}", e)
                _uiState.update { it.copy(
                    isLoadingList = false,
                    errorMessage = e.message ?: "Failed to load linked seniors"
                ) }
            }
        }
    }
    
    fun loadLinkHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }
            
            val caregiverId = authRepository.getCurrentUid() ?: ""
            Log.d("LinkSeniorVM", "Loading link history for requester: $caregiverId")
            
            try {
                // 获取所有关系（包括 pending）
                val relationsResult = caregiverRelationRepository.getRelationsByCaregiver(caregiverId)
                
                if (relationsResult.isSuccess) {
                    val relations = relationsResult.getOrNull() ?: emptyList()
                    Log.d("LinkSeniorVM", "Found ${relations.size} relations in history")
                    
                    // 丰富老人信息
                    val historyItems = relations.map { relation ->
                        var seniorName = ""
                        var avatarType = ""
                        
                        seniorProfileRepository.getProfileById(relation.seniorId)
                            .onSuccess { profile ->
                                seniorName = profile.name
                                avatarType = profile.avatarType
                            }
                            .onFailure { error ->
                                Log.e("LinkSeniorVM", "Failed to load senior ${relation.seniorId}: ${error.message}")
                            }
                        
                        LinkHistoryItem(
                            relation = relation,
                            seniorName = seniorName,
                            seniorAvatarType = avatarType
                        )
                    }
                    
                    // 按创建时间排序（最新在前）
                    val sortedHistory = historyItems.sortedByDescending { it.relation.createdAt }
                    
                    _uiState.update { it.copy(
                        linkHistory = sortedHistory,
                        isLoadingHistory = false
                    ) }
                } else {
                    val error = relationsResult.exceptionOrNull()
                    Log.e("LinkSeniorVM", "Failed to load link history: ${error?.message}", error)
                    _uiState.update { it.copy(
                        isLoadingHistory = false,
                        errorMessage = error?.message ?: "Failed to load link history"
                    ) }
                }
            } catch (e: Exception) {
                Log.e("LinkSeniorVM", "Exception loading link history: ${e.message}", e)
                _uiState.update { it.copy(
                    isLoadingHistory = false,
                    errorMessage = e.message ?: "Failed to load link history"
                ) }
            }
        }
    }

    fun onSeniorIdChanged(id: String) {
        _uiState.update { it.copy(
            seniorId = id.uppercase().trim(),
            seniorIdError = null,
            errorMessage = null
        ) }
    }
    
    fun onSeniorIdChangedAndSearch(id: String) {
        _uiState.update { it.copy(
            seniorId = id.uppercase().trim(),
            seniorIdError = null,
            errorMessage = null
        ) }
        // Auto search after setting ID (for QR code scanning)
        if (id.trim().isNotEmpty() && id.trim().matches(AuthConstants.SNR_ID_REGEX)) {
            searchSenior()
        }
    }

    fun onRelationshipChanged(relationship: String) {
        _uiState.update { it.copy(
            relationship = relationship,
            relationshipError = null
        ) }
    }

    fun onNicknameChanged(nickname: String) {
        _uiState.update { it.copy(nickname = nickname) }
    }

    fun onCaregiverNameChanged(name: String) {
        _uiState.update { it.copy(
            caregiverName = name,
            caregiverNameError = null
        ) }
    }

    fun onMessageChanged(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    fun searchSenior() {
        val seniorId = _uiState.value.seniorId
        
        // Validate
        if (seniorId.isBlank()) {
            _uiState.update { it.copy(seniorIdError = "Please enter a Senior ID") }
            return
        }
        
        if (!seniorId.matches(AuthConstants.SNR_ID_REGEX)) {
            _uiState.update { it.copy(
                seniorIdError = "Invalid ID format. Expected: SNR-XXXXXXXXXXXX"
            ) }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }
            
            val caregiverId = authRepository.getCurrentUid() ?: ""
            
            // 搜索老人档案
            seniorProfileRepository.getProfileById(seniorId)
                .onSuccess { profile ->
                    // 检查是否已绑定
                    val existingRelation = caregiverRelationRepository.getRelation(caregiverId, seniorId).getOrNull()
                    if (existingRelation != null && existingRelation.status == "active") {
                        _uiState.update { it.copy(
                            isSearching = false,
                            errorMessage = "This senior is already linked to your account"
                        ) }
                        return@launch
                    }
                    
                    // 找到 - 显示验证界面
                    _uiState.update { it.copy(
                        isSearching = false,
                        foundSenior = profile
                    ) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isSearching = false,
                        showNotFoundDialog = true
                    ) }
                }
        }
    }

    fun sendLinkRequest() {
        val profile = _uiState.value.foundSenior ?: return
        val relationship = _uiState.value.relationship
        val nickname = _uiState.value.nickname
        val caregiverName = _uiState.value.caregiverName
        
        // Validate
        var hasError = false
        
        if (relationship.isBlank() || relationship == "-- Select Relationship --") {
            _uiState.update { it.copy(relationshipError = "Please select a relationship") }
            hasError = true
        }
        
        if (caregiverName.isBlank()) {
            _uiState.update { it.copy(caregiverNameError = "Please enter your name") }
            hasError = true
        }
        
        if (hasError) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLinking = true, errorMessage = null) }
            
            val caregiverId = authRepository.getCurrentUid() ?: ""
            
            // 如果nickname为空，使用默认值
            val finalNickname = nickname.ifBlank {
                RelationshipHelper.getDefaultAddressTitle(relationship, profile.gender)
            }
            
            // 创建关系请求
            val relation = CaregiverRelation(
                id = CaregiverRelation.generateId(caregiverId, profile.id),
                caregiverId = caregiverId,
                seniorId = profile.id,
                status = "pending",
                relationship = relationship,
                nickname = finalNickname,  // 使用finalNickname确保始终有值
                caregiverName = _uiState.value.caregiverName,  // 护理者的真实姓名
                message = _uiState.value.message,
                canViewHealthData = true,
                canEditHealthData = false,
                canViewReminders = true,
                canEditReminders = false,
                canApproveRequests = false,
                createdAt = System.currentTimeMillis()
            )
            
            Log.d("LinkSeniorVM", "Creating link request for senior: ${profile.id}")
            
            caregiverRelationRepository.createRelation(relation)
                .onSuccess {
                    Log.d("LinkSeniorVM", "Link request created successfully")
                    _uiState.update { it.copy(
                        isLinking = false,
                        isSuccess = true
                    ) }
                    
                    // Send success Snackbar via Channel
                    _uiEvent.send(UiEvent.ShowSnackbar("Link request sent successfully! Waiting for senior approval."))
                }
                .onFailure { error ->
                    Log.e("LinkSeniorVM", "Failed to create link request: ${error.message}", error)
                    _uiState.update { it.copy(isLinking = false) }
                    
                    // Show error dialog via StateFlow
                    _errorDialog.value = ErrorDialogState(
                        title = "Link Request Failed",
                        message = error.message ?: "Failed to send link request. Please try again."
                    )
                }
        }
    }

    fun dismissNotFoundDialog() {
        _uiState.update { it.copy(showNotFoundDialog = false) }
    }

    fun resetSearch() {
        _uiState.update { it.copy(
            foundSenior = null,
            relationship = "",
            relationshipError = null,
            nickname = "",
            caregiverName = "",
            caregiverNameError = null,
            message = ""
        ) }
    }

    fun resetLinkForm() {
        _uiState.update { it.copy(
            seniorId = "",
            seniorIdError = null,
            foundSenior = null,
            relationship = "",
            relationshipError = null,
            nickname = "",
            caregiverName = "",
            caregiverNameError = null,
            message = "",
            errorMessage = null,
            isSearching = false,
            isLinking = false,
            isSuccess = false,
            showNotFoundDialog = false
        ) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * 关闭错误对话框
     */
    fun dismissErrorDialog() {
        _errorDialog.value = null
    }
}

/**
 * UI Events (one-time events via Channel)
 */
sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
}

/**
 * Error Dialog State (persisted via StateFlow)
 */
data class ErrorDialogState(
    val title: String,
    val message: String
)
