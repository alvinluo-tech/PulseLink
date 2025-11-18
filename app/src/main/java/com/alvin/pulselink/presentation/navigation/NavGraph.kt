package com.alvin.pulselink.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.alvin.pulselink.domain.model.UserRole
import com.alvin.pulselink.presentation.home.HomeScreen
import com.alvin.pulselink.presentation.login.CaregiverLoginScreen
import com.alvin.pulselink.presentation.login.LoginViewModel
import com.alvin.pulselink.presentation.login.SeniorLoginScreen
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
            HomeScreen()
        }
    }
}
