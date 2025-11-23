package com.alvin.pulselink.presentation.caregiver.linkguard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.LinkRequestRepository
import com.alvin.pulselink.domain.repository.SeniorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Link Request UI data model (for display)
 * Represents a request from another caregiver to link to a senior account I created
 */
data class LinkRequestDisplay(
    val id: String,
    val seniorId: String,
    val seniorName: String,
    val requesterId: String,
    val requesterName: String,
    val requesterEmail: String,
    val relationship: String,          // e.g., "Mother", "Grandmother (Maternal Grandmother)"
    val message: String,
    val requestedDate: String,         // e.g., "Requested on November 22, 2025"
    val timeAgo: String,               // e.g., "2 minutes ago" (for sorting/display)
    val timestamp: Long
)

/**
 * UI State for Link Requests Screen
 */
data class LinkRequestsUiState(
    val pendingRequests: List<LinkRequestDisplay> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class LinkRequestsViewModel @Inject constructor(
    private val linkRequestRepository: LinkRequestRepository,
    private val seniorRepository: SeniorRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LinkRequestsUiState())
    val uiState: StateFlow<LinkRequestsUiState> = _uiState.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH)
    
    fun loadPendingRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val currentUser = authRepository.getCurrentUser()
                Log.d("LinkRequestsVM", "Current user: ${currentUser?.id}")
                
                if (currentUser == null) {
                    Log.e("LinkRequestsVM", "User not logged in")
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "User not logged in"
                        )
                    }
                    return@launch
                }
                
                // 从 linkRequests collection 获取待审批的请求
                val result = linkRequestRepository.getPendingRequestsForCreator(currentUser.id)
                Log.d("LinkRequestsVM", "getPendingRequestsForCreator result: success=${result.isSuccess}")
                
                result.fold(
                    onSuccess = { linkRequests ->
                        Log.d("LinkRequestsVM", "Found ${linkRequests.size} pending requests")
                        val displayRequests = mutableListOf<LinkRequestDisplay>()
                        
                        // 为每个请求获取相关的 Senior 和 User 信息
                        linkRequests.forEach { request ->
                            Log.d("LinkRequestsVM", "Processing request: ${request.id}")
                            
                            // 获取 Senior 信息
                            val seniorResult = seniorRepository.getSeniorById(request.seniorId)
                            val senior = seniorResult.getOrNull()
                            
                            if (senior == null) {
                                Log.e("LinkRequestsVM", "Senior not found: ${request.seniorId}")
                                return@forEach
                            }
                            
                            // 获取请求者信息
                            val requesterResult = authRepository.getUserById(request.requesterId)
                            val requester = requesterResult.getOrNull()
                            
                            Log.d("LinkRequestsVM", "Requester info - name: '${requester?.name}', username: '${requester?.username}', email: '${requester?.email}'")
                            
                            val finalName = requester?.name?.takeIf { it.isNotEmpty() } 
                                ?: requester?.username 
                                ?: "Unknown User"
                            val finalEmail = requester?.email?.takeIf { it.isNotEmpty() } ?: "No email"
                            
                            displayRequests.add(
                                LinkRequestDisplay(
                                    id = request.id,
                                    seniorId = request.seniorId,
                                    seniorName = senior.name,
                                    requesterId = request.requesterId,
                                    requesterName = finalName,
                                    requesterEmail = finalEmail,
                                    relationship = request.relationship,
                                    message = request.message.takeIf { it.isNotEmpty() } 
                                        ?: "Would like to link to ${senior.name} as ${request.nickname}",
                                    requestedDate = "Requested on ${dateFormat.format(Date(request.createdAt))}",
                                    timeAgo = getTimeAgo(request.createdAt),
                                    timestamp = request.createdAt
                                )
                            )
                        }
                        
                        Log.d("LinkRequestsVM", "Total display requests created: ${displayRequests.size}")
                        
                        // 按时间倒序排序
                        val sortedRequests = displayRequests.sortedByDescending { it.timestamp }
                        
                        _uiState.update { 
                            it.copy(
                                pendingRequests = sortedRequests,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e("LinkRequestsVM", "Failed to load requests: ${error.message}", error)
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to load pending requests"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("LinkRequestsVM", "Exception loading requests: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "An error occurred"
                    )
                }
            }
        }
    }
    
    fun approveRequest(request: LinkRequestDisplay) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // 1. 获取 LinkRequest 
                val linkRequestResult = linkRequestRepository.getRequestById(request.id)
                val linkRequest = linkRequestResult.getOrNull()
                
                if (linkRequest == null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Request not found"
                        )
                    }
                    return@launch
                }
                
                // 2. 获取老人数据
                val seniorResult = seniorRepository.getSeniorById(request.seniorId)
                val senior = seniorResult.getOrNull()
                
                if (senior == null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Senior account not found"
                        )
                    }
                    return@launch
                }
                
                // 3. 添加到 caregiverIds 并创建 caregiverRelationship
                val updatedCaregiverIds = if (!senior.caregiverIds.contains(request.requesterId)) {
                    senior.caregiverIds + request.requesterId
                } else {
                    senior.caregiverIds
                }
                
                val updatedRelationships = senior.caregiverRelationships.toMutableMap()
                updatedRelationships[request.requesterId] = com.alvin.pulselink.domain.model.CaregiverRelationship(
                    relationship = linkRequest.relationship,
                    nickname = linkRequest.nickname,
                    linkedAt = linkRequest.createdAt,
                    status = "active",
                    message = linkRequest.message
                )
                
                // 4. 更新 Senior
                val updatedSenior = senior.copy(
                    caregiverIds = updatedCaregiverIds,
                    caregiverRelationships = updatedRelationships
                )
                
                val updateResult = seniorRepository.updateSenior(updatedSenior)
                
                if (updateResult.isSuccess) {
                    // 5. 更新 LinkRequest 状态为 approved
                    linkRequestRepository.updateRequestStatus(request.id, "approved")
                        .onSuccess {
                            Log.d("LinkRequestsVM", "Approved request: ${request.id}")
                            // 从待处理列表中移除
                            _uiState.update { state ->
                                state.copy(
                                    pendingRequests = state.pendingRequests.filter { it.id != request.id },
                                    isLoading = false,
                                    successMessage = "Link request approved successfully!"
                                )
                            }
                        }
                        .onFailure { error ->
                            Log.e("LinkRequestsVM", "Failed to update request status: ${error.message}")
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: "Failed to update request status"
                                )
                            }
                        }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = updateResult.exceptionOrNull()?.message ?: "Failed to approve request"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("LinkRequestsVM", "Exception approving request: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to approve request"
                    )
                }
            }
        }
    }
    
    fun rejectRequest(request: LinkRequestDisplay) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // 更新 LinkRequest 状态为 rejected
                linkRequestRepository.updateRequestStatus(request.id, "rejected")
                    .onSuccess {
                        Log.d("LinkRequestsVM", "Rejected request: ${request.id}")
                        // 从待处理列表中移除
                        _uiState.update { state ->
                            state.copy(
                                pendingRequests = state.pendingRequests.filter { it.id != request.id },
                                isLoading = false,
                                successMessage = "Link request rejected"
                            )
                        }
                    }
                    .onFailure { error ->
                        Log.e("LinkRequestsVM", "Failed to reject request: ${error.message}")
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to reject request"
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e("LinkRequestsVM", "Exception rejecting request: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to reject request"
                    )
                }
            }
        }
    }
    
    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} minutes ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            diff < 604800_000 -> "${diff / 86400_000} days ago"
            else -> "${diff / 604800_000} weeks ago"
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
