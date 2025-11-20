package com.alvin.pulselink.presentation.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object SeniorLogin : Screen("senior_login")
    object CaregiverLogin : Screen("caregiver_login")
    object SeniorRegister : Screen("senior_register")
    object CaregiverRegister : Screen("caregiver_register")
    object SeniorForgotPassword : Screen("senior_forgot_password")
    object CaregiverForgotPassword : Screen("caregiver_forgot_password")
    object SeniorEmailVerification : Screen("senior_email_verification/{email}") {
        fun createRoute(email: String) = "senior_email_verification/$email"
    }
    object CaregiverEmailVerification : Screen("caregiver_email_verification/{email}") {
        fun createRoute(email: String) = "caregiver_email_verification/$email"
    }
    object Home : Screen("home")
    object HealthData : Screen("health_data")
    object HealthHistory : Screen("health_history")
    object Profile : Screen("profile")
    object Assistant : Screen("assistant")
    object Reminder : Screen("reminder")
    object ReminderList : Screen("reminder_list")
    
    // Caregiver Screens
    object CareDashboard : Screen("care_dashboard")
    object CareChat : Screen("care_chat")
    object CaregiverProfile : Screen("caregiver_profile")
    object CareSettings : Screen("care_settings")
    object ManageFamily : Screen("manage_family")
    object PrivacySecurity : Screen("privacy_security")
    object HelpCenter : Screen("help_center")
}
