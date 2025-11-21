package com.alvin.pulselink.presentation.senior.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    // TODO: Inject SharedPreferences or DataStore for persistence
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setFontSize(fontSize: FontSize) {
        _uiState.update { it.copy(fontSize = fontSize) }
        // TODO: Save to preferences
    }

    fun toggleShareDataWithFamily(enabled: Boolean) {
        _uiState.update { it.copy(shareDataWithFamily = enabled) }
        // TODO: Save to preferences
    }

    fun toggleHealthData(enabled: Boolean) {
        _uiState.update { it.copy(healthData = enabled) }
        // TODO: Save to preferences
    }

    fun toggleActivityData(enabled: Boolean) {
        _uiState.update { it.copy(activityData = enabled) }
        // TODO: Save to preferences
    }

    fun clearChatHistory() {
        // TODO: Implement chat history clearing
        // Call repository to delete chat messages
    }
}
