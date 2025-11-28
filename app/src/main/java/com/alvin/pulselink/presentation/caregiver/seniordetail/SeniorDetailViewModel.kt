package com.alvin.pulselink.presentation.caregiver.seniordetail

import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.presentation.common.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Senior Detail ViewModel
 * 管理老人详情页面的总体状态
 */
@HiltViewModel
class SeniorDetailViewModel @Inject constructor(
    // TODO: Inject repositories
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SeniorDetailUiState())
    val uiState: StateFlow<SeniorDetailUiState> = _uiState.asStateFlow()

    fun loadSeniorDetails(seniorId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                seniorId = seniorId,
                isLoading = true
            ) }
            
            // TODO: Load senior profile and basic info
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}

/**
 * Senior Detail UI State
 */
data class SeniorDetailUiState(
    val seniorId: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
