package com.alvin.pulselink.presentation.caregiver.senior

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.core.constants.AuthConstants
import com.alvin.pulselink.domain.model.LinkRequest
import com.alvin.pulselink.domain.model.Senior
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.LinkRequestRepository
import com.alvin.pulselink.domain.repository.SeniorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Link Senior Account ViewModel
 */
@HiltViewModel
class LinkSeniorViewModel @Inject constructor(
    private val seniorRepository: SeniorRepository,
    private val linkRequestRepository: LinkRequestRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LinkSeniorUiState())
    val uiState: StateFlow<LinkSeniorUiState> = _uiState.asStateFlow()

    init {
        loadLinkedSeniors()
    }

    fun loadLinkedSeniors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingList = true) }
            
            val caregiverId = authRepository.getCurrentUid() ?: ""
            Log.d("LinkSeniorVM", "Loading linked seniors for caregiver: $caregiverId")
            
            try {
                // Only get approved linked seniors (no pending)
                val activeSeniorsResult = seniorRepository.getSeniorsByCaregiver(caregiverId)
                
                if (activeSeniorsResult.isSuccess) {
                    val activeSeniors = activeSeniorsResult.getOrNull() ?: emptyList()
                    
                    Log.d("LinkSeniorVM", "Active linked seniors: ${activeSeniors.size}")
                    
                    // Filter only linked ones (not created by current user)
                    val linkedOnly = activeSeniors.filter { it.creatorId != caregiverId }
                    
                    Log.d("LinkSeniorVM", "Linked seniors (excluding created): ${linkedOnly.size}")
                    
                    _uiState.update { it.copy(
                        linkedSeniors = linkedOnly,
                        isLoadingList = false
                    ) }
                } else {
                    val error = activeSeniorsResult.exceptionOrNull()
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
                // Get all requests by this user (pending, approved, rejected)
                val requestsResult = linkRequestRepository.getRequestsByRequester(caregiverId)
                
                if (requestsResult.isSuccess) {
                    val requests = requestsResult.getOrNull() ?: emptyList()
                    Log.d("LinkSeniorVM", "Found ${requests.size} link requests in history")
                    
                    // Enrich with senior info
                    val historyItems = requests.map { request ->
                        var seniorName = ""
                        var avatarType = ""
                        
                        seniorRepository.getSeniorById(request.seniorId)
                            .onSuccess { senior ->
                                seniorName = senior.name
                                avatarType = senior.avatarType
                            }
                            .onFailure { error ->
                                Log.e("LinkSeniorVM", "Failed to load senior ${request.seniorId}: ${error.message}")
                            }
                        
                        LinkHistoryItem(
                            request = request,
                            seniorName = seniorName,
                            seniorAvatarType = avatarType
                        )
                    }
                    
                    // Sort by created time (newest first)
                    val sortedHistory = historyItems.sortedByDescending { it.request.createdAt }
                    
                    _uiState.update { it.copy(
                        linkHistory = sortedHistory,
                        isLoadingHistory = false
                    ) }
                } else {
                    val error = requestsResult.exceptionOrNull()
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
            
            // Search for senior account
            seniorRepository.getSeniorById(seniorId)
                .onSuccess { senior ->
                    // Check if already linked
                    if (senior.caregiverIds.contains(caregiverId)) {
                        _uiState.update { it.copy(
                            isSearching = false,
                            errorMessage = "This senior is already linked to your account"
                        ) }
                        return@launch
                    }
                    
                    // Found - show verification screen
                    _uiState.update { it.copy(
                        isSearching = false,
                        foundSenior = senior
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
        val senior = _uiState.value.foundSenior ?: return
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
            
            // 创建链接请求到独立的 linkRequests collection
            val linkRequest = LinkRequest(
                seniorId = senior.id,
                requesterId = caregiverId,
                creatorId = senior.creatorId,
                relationship = relationship,
                nickname = nickname,
                message = _uiState.value.message,
                status = "pending"
            )
            
            Log.d("LinkSeniorVM", "Creating link request for senior: ${senior.id}")
            
            linkRequestRepository.createLinkRequest(linkRequest)
                .onSuccess {
                    Log.d("LinkSeniorVM", "Link request created successfully")
                    _uiState.update { it.copy(
                        isLinking = false,
                        isSuccess = true
                    ) }
                }
                .onFailure { error ->
                    Log.e("LinkSeniorVM", "Failed to create link request: ${error.message}", error)
                    _uiState.update { it.copy(
                        isLinking = false,
                        errorMessage = error.message ?: "Failed to send link request"
                    ) }
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
}
