package com.alvin.pulselink.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.alvin.pulselink.domain.model.UserRole
import com.alvin.pulselink.presentation.health.HealthDataScreen
import com.alvin.pulselink.presentation.history.HealthHistoryScreen
import com.alvin.pulselink.presentation.home.HomeScreen
import com.alvin.pulselink.presentation.assistant.VoiceAssistantScreen
import com.alvin.pulselink.presentation.login.CaregiverLoginScreen
import com.alvin.pulselink.presentation.login.LoginViewModel
import com.alvin.pulselink.presentation.login.SeniorLoginScreen
import com.alvin.pulselink.presentation.profile.ProfileScreen
import com.alvin.pulselink.presentation.register.SeniorRegisterScreen
import com.alvin.pulselink.presentation.register.CaregiverRegisterScreen
import com.alvin.pulselink.presentation.forgotpassword.SeniorForgotPasswordScreen
import com.alvin.pulselink.presentation.forgotpassword.CaregiverForgotPasswordScreen
import com.alvin.pulselink.presentation.reminder.ReminderScreen
import com.alvin.pulselink.presentation.reminderlist.ReminderListScreen
import com.alvin.pulselink.presentation.verification.SeniorEmailVerificationScreen
import com.alvin.pulselink.presentation.verification.CaregiverEmailVerificationScreen
import com.alvin.pulselink.presentation.welcome.WelcomeScreen
import androidx.navigation.navArgument
import androidx.navigation.NavType

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Welcome.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Welcome Screen
        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToSeniorLogin = {
                    navController.navigate(Screen.SeniorLogin.route)
                },
                onNavigateToCaregiverLogin = {
                    navController.navigate(Screen.CaregiverLogin.route)
                }
            )
        }
        
        // Senior Login Screen
        composable(route = Screen.SeniorLogin.route) {
            val viewModel: LoginViewModel = hiltViewModel()
            SeniorLoginScreen(
                viewModel = viewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.SeniorLogin.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.SeniorRegister.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.SeniorForgotPassword.route)
                }
            )
        }
        
        // Caregiver Login Screen
        composable(route = Screen.CaregiverLogin.route) {
            val viewModel: LoginViewModel = hiltViewModel()
            CaregiverLoginScreen(
                viewModel = viewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.CaregiverLogin.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.CaregiverRegister.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.CaregiverForgotPassword.route)
                }
            )
        }
        
        // Senior Register Screen
        composable(route = Screen.SeniorRegister.route) {
            SeniorRegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.SeniorLogin.route) {
                        popUpTo(Screen.SeniorRegister.route) { inclusive = true }
                    }
                },
                onRegisterSuccess = { email ->
                    navController.navigate(Screen.SeniorEmailVerification.createRoute(email)) {
                        popUpTo(Screen.SeniorRegister.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Caregiver Register Screen
        composable(route = Screen.CaregiverRegister.route) {
            CaregiverRegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.CaregiverLogin.route) {
                        popUpTo(Screen.CaregiverRegister.route) { inclusive = true }
                    }
                },
                onRegisterSuccess = { email ->
                    navController.navigate(Screen.CaregiverEmailVerification.createRoute(email)) {
                        popUpTo(Screen.CaregiverRegister.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Senior Email Verification Screen
        composable(
            route = Screen.SeniorEmailVerification.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            SeniorEmailVerificationScreen(
                email = email,
                onNavigateBack = {
                    navController.navigate(Screen.SeniorLogin.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = false }
                    }
                },
                onGotEmail = {
                    navController.navigate(Screen.SeniorLogin.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = false }
                    }
                }
            )
        }
        
        // Caregiver Email Verification Screen
        composable(
            route = Screen.CaregiverEmailVerification.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            CaregiverEmailVerificationScreen(
                email = email,
                onNavigateBack = {
                    navController.navigate(Screen.CaregiverLogin.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = false }
                    }
                },
                onGotEmail = {
                    navController.navigate(Screen.CaregiverLogin.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = false }
                    }
                }
            )
        }
        
        // Senior Forgot Password Screen
        composable(route = Screen.SeniorForgotPassword.route) {
            SeniorForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Caregiver Forgot Password Screen
        composable(route = Screen.CaregiverForgotPassword.route) {
            CaregiverForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Home Screen
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToHealthData = {
                    navController.navigate(Screen.HealthData.route)
                },
                onNavigateToHealthHistory = {
                    navController.navigate(Screen.HealthHistory.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToAssistant = {
                    navController.navigate(Screen.Assistant.route)
                },
                onNavigateToReminder = {
                    navController.navigate(Screen.Reminder.route)
                }
            )
        }
        
        // Health Data Screen
        composable(route = Screen.HealthData.route) {
            HealthDataScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToAssistant = {
                    navController.navigate(Screen.Assistant.route)
                }
            )
        }
        
        // Profile Screen
        composable(route = Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToAssistant = {
                    navController.navigate(Screen.Assistant.route)
                },
                onNavigateToReminderList = {
                    navController.navigate(Screen.ReminderList.route)
                },
                onLogout = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Voice Assistant Screen
        composable(route = Screen.Assistant.route) {
            VoiceAssistantScreen(
                onClose = { navController.popBackStack() },
                onNavigateHome = { navController.navigate(Screen.Home.route) },
                onNavigateProfile = { navController.navigate(Screen.Profile.route) }
            )
        }
        
        // Health History Screen
        composable(route = Screen.HealthHistory.route) {
            HealthHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = { navController.navigate(Screen.Home.route) },
                onNavigateProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateAssistant = { navController.navigate(Screen.Assistant.route) }
            )
        }
        
        // Reminder Screen
        composable(route = Screen.Reminder.route) {
            ReminderScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Reminder List Screen
        composable(route = Screen.ReminderList.route) {
            ReminderListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigate(Screen.Home.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }
    }
}
