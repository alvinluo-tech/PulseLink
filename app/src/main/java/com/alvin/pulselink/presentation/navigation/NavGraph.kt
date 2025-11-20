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
import com.alvin.pulselink.presentation.reminder.ReminderScreen
import com.alvin.pulselink.presentation.reminderlist.ReminderListScreen
import com.alvin.pulselink.presentation.welcome.WelcomeScreen

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
                    navController.popBackStack()
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
