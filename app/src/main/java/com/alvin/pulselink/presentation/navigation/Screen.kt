package com.alvin.pulselink.presentation.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object SeniorLogin : Screen("senior_login")
    object CaregiverLogin : Screen("caregiver_login")
    object Home : Screen("home")
    object HealthData : Screen("health_data")
    object HealthHistory : Screen("health_history")
    object Profile : Screen("profile")
    object Assistant : Screen("assistant")
    object Reminder : Screen("reminder")
    object ReminderList : Screen("reminder_list")
}
