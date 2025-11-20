package com.alvin.pulselink.presentation.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvin.pulselink.domain.usecase.TestFirestoreConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FirebaseTestUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class FirebaseTestViewModel @Inject constructor(
    private val testFirestoreConnectionUseCase: TestFirestoreConnectionUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FirebaseTestUiState())
    val uiState: StateFlow<FirebaseTestUiState> = _uiState.asStateFlow()
    
    fun testFirestoreConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            
            val result = testFirestoreConnectionUseCase()
            
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        message = "✅ Firestore connection successful! Check Firebase Console for the test log."
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        message = "❌ Connection failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }
}
