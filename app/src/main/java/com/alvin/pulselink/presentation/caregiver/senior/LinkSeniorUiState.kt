package com.alvin.pulselink.presentation.caregiver.senior

import com.alvin.pulselink.domain.model.CaregiverRelation
import com.alvin.pulselink.domain.model.SeniorProfile

/**
 * Link Senior Account UI State
 */
data class LinkSeniorUiState(
    val seniorId: String = "",
    val seniorIdError: String? = null,
    val isSearching: Boolean = false,
    val foundSenior: SeniorProfile? = null,
    val relationship: String = "",
    val relationshipError: String? = null,
    val nickname: String = "",
    val caregiverName: String = "",
    val caregiverNameError: String? = null,
    val message: String = "",
    val isLinking: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val linkedSeniors: List<SeniorProfile> = emptyList(),
    val isLoadingList: Boolean = false,
    val showNotFoundDialog: Boolean = false,
    
    // Link history
    val linkHistory: List<LinkHistoryItem> = emptyList(),
    val isLoadingHistory: Boolean = false
)

/**
 * Link History Item
 */
data class LinkHistoryItem(
    val relation: CaregiverRelation,
    val seniorName: String,
    val seniorAvatarType: String
)
