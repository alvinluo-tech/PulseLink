package com.alvin.pulselink.presentation.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object SeniorLogin : Screen("senior_login")
    object CaregiverLogin : Screen("caregiver_login")
    object Home : Screen("home")
}
