package com.alvin.pulselink.domain.model

data class HealthData(
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int,
    val timestamp: Long
)
