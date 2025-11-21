package com.alvin.pulselink.presentation.caregiver.senior

import com.alvin.pulselink.domain.model.Senior

/**
 * 管理老人页面 UI 状态
 */
data class ManageSeniorsUiState(
    val createdSeniors: List<Senior> = emptyList(),
    val linkedSeniors: List<Senior> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentUserId: String = ""
)

/**
 * 创建老人页面 UI 状态
 */
data class CreateSeniorUiState(
    // 基本信息
    val name: String = "",
    val age: String = "",
    val gender: String = "Male",
    
    // 健康历史
    val systolicBP: String = "",
    val diastolicBP: String = "",
    val heartRate: String = "",
    val bloodSugar: String = "",
    val medicalConditions: String = "",
    val medications: String = "",
    val allergies: String = "",
    
    // 错误信息
    val nameError: String? = null,
    val ageError: String? = null,
    val bpError: String? = null,
    
    // 加载状态
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    
    // 已创建的老人列表
    val createdSeniors: List<Senior> = emptyList()
)
