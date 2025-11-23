package com.alvin.pulselink.domain.model

data class User(
    val id: String,
    val email: String = "",
    val name: String = "",
    val username: String,
    val role: UserRole
)

enum class UserRole {
    SENIOR,
    CAREGIVER
}
