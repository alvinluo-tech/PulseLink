package com.alvin.pulselink.presentation.senior.profile

data class ProfileUiState(
    val userName: String = "User",  // 从 Firestore Senior 数据加载
    val age: Int = 0,  // 从 Firestore Senior 数据加载
    val gender: String = "",  // 从 Firestore Senior 数据加载
    val avatarType: String = "",  // 从 Firestore Senior 数据加载
    val avatarEmoji: String = "🧓",  // 根据 avatarType 通过 AvatarHelper 获取
    val daysUsed: Int = 0,  // 根据 createdAt 计算
    val seniorId: String = "",  // Senior ID for QR code sharing
    val bloodPressure: String = "--/--",  // 从 health_data 集合获取最新数据
    val bloodPressureStatus: String = "No Data",  // 根据血压值分析状态
    val heartRate: Int = 0,  // 从 health_data 集合获取最新数据
    val isLoading: Boolean = false,
    val error: String? = null
)
