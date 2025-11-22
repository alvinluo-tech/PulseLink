package com.alvin.pulselink.presentation.senior.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val localDataSource: LocalDataSource
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadProfileData()
    }
    
    private fun loadProfileData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // 1️⃣ 立即从本地缓存读取 (快速显示)
            val cachedUser = localDataSource.getUser()
            val cachedName = cachedUser?.second ?: "User"
            
            _uiState.update { 
                it.copy(
                    userName = cachedName,
                    isLoading = false
                )
            }
            
            // 2️⃣ 后台静默同步 Firestore (检查更新)
            syncFromFirestore()
        }
    }
    
    /**
     * 静默同步 Firestore 数据
     * 如果有变化，更新本地缓存和 UI
     */
    private suspend fun syncFromFirestore() {
        try {
            val firestoreUser = authRepository.getCurrentUser()
            
            if (firestoreUser != null) {
                val cachedUser = localDataSource.getUser()
                
                // 检查是否有变化
                if (cachedUser?.second != firestoreUser.username) {
                    // 有变化,更新本地缓存
                    localDataSource.saveUser(
                        id = firestoreUser.id,
                        username = firestoreUser.username,
                        role = firestoreUser.role.name.lowercase()
                    )
                    
                    // 更新 UI
                    _uiState.update { 
                        it.copy(userName = firestoreUser.username)
                    }
                }
            }
        } catch (e: Exception) {
            // 静默失败,不影响用户体验
            // 用户仍然看到缓存的数据
        }
    }
}
