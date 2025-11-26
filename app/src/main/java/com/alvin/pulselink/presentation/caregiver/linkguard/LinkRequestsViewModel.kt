package com.alvin.pulselink.presentation.caregiver.linkguard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import com.alvin.pulselink.domain.repository.SeniorProfileRepository
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
    private val caregiverRelationRepository: CaregiverRelationRepository,
    private val seniorProfileRepository: SeniorProfileRepository,
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
                
                // 获取当前用户创建的所有老人资料
                val profilesResult = seniorProfileRepository.getProfilesByCreator(currentUser.id)
                val createdProfiles = profilesResult.getOrNull() ?: emptyList()
                Log.d("LinkRequestsVM", "Found ${createdProfiles.size} created profiles")
                
                val displayRequests = mutableListOf<LinkRequestDisplay>()
                
                // 对于每个老人资料，获取 pending 状态的关系请求
                createdProfiles.forEach { profile ->
                    val relationsResult = caregiverRelationRepository.getPendingRelationsBySenior(profile.id)
                    relationsResult.getOrNull()
                        ?.forEach { relation ->
                            Log.d("LinkRequestsVM", "Processing pending relation: ${relation.id}")
                            
                            // 获取请求者信息
                            val requesterResult = authRepository.getUserById(relation.caregiverId)
                            val requester = requesterResult.getOrNull()
                            
                            val finalName = requester?.name?.takeIf { it.isNotEmpty() } 
                                ?: requester?.username 
                                ?: "Unknown User"
                            val finalEmail = requester?.email?.takeIf { it.isNotEmpty() } ?: "No email"
                            
                            displayRequests.add(
                                LinkRequestDisplay(
                                    id = relation.id,
                                    seniorId = profile.id,
                                    seniorName = profile.name,
                                    requesterId = relation.caregiverId,
                                    requesterName = finalName,
                                    requesterEmail = finalEmail,
                                    relationship = relation.relationship,
                                    message = relation.message.takeIf { it.isNotEmpty() } 
                                        ?: "Would like to link to ${profile.name} as ${relation.nickname}",
                                    requestedDate = "Requested on ${dateFormat.format(Date(relation.createdAt))}",
                                    timeAgo = getTimeAgo(relation.createdAt),
                                    timestamp = relation.createdAt
                                )
                            )
                        }
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
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _uiState.update { 
                        it.copy(isLoading = false, errorMessage = "User not logged in")
                    }
                    return@launch
                }
                
                // 更新关系状态为 approved
                caregiverRelationRepository.approveRelation(request.id, currentUser.id)
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
                        Log.e("LinkRequestsVM", "Failed to approve request: ${error.message}")
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to approve request"
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
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _uiState.update { 
                        it.copy(isLoading = false, errorMessage = "User not logged in")
                    }
                    return@launch
                }
                
                // 更新关系状态为 rejected
                caregiverRelationRepository.rejectRelation(request.id, currentUser.id)
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
