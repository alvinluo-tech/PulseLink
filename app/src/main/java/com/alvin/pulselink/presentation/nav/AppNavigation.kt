package com.alvin.pulselink.presentation.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.domain.model.UserRole
import com.alvin.pulselink.presentation.auth.AuthCheckScreen
import com.alvin.pulselink.presentation.auth.AuthViewModel
import com.alvin.pulselink.presentation.auth.WelcomeScreen
import com.alvin.pulselink.presentation.auth.LoginScreen
import com.alvin.pulselink.presentation.auth.RegisterScreen
import com.alvin.pulselink.presentation.auth.ForgotPasswordScreen
import com.alvin.pulselink.presentation.auth.EmailVerificationScreen
import com.alvin.pulselink.presentation.caregiver.dashboard.CareDashboardScreen
import com.alvin.pulselink.presentation.caregiver.dashboard.CareDashboardViewModel
import com.alvin.pulselink.presentation.caregiver.chat.CareChatScreen
import com.alvin.pulselink.presentation.caregiver.profile.CaregiverProfileScreen
import com.alvin.pulselink.presentation.caregiver.profile.CaregiverProfileViewModel
import com.alvin.pulselink.presentation.caregiver.settings.CareSettingsScreen
import com.alvin.pulselink.presentation.caregiver.senior.ManageSeniorsScreen
import com.alvin.pulselink.presentation.caregiver.senior.CreateSeniorScreen
import com.alvin.pulselink.presentation.caregiver.senior.LinkSeniorScreen
import com.alvin.pulselink.presentation.caregiver.senior.LinkHistoryScreen
import com.alvin.pulselink.presentation.caregiver.linkguard.FamilyRequestsScreen
import com.alvin.pulselink.presentation.senior.home.HomeScreen
import com.alvin.pulselink.presentation.senior.health.HealthReportScreen
import com.alvin.pulselink.presentation.senior.history.HealthHistoryScreen
import com.alvin.pulselink.presentation.senior.profile.ProfileScreen
import com.alvin.pulselink.presentation.senior.reminder.ReminderScreen
import com.alvin.pulselink.presentation.senior.reminder.ReminderListScreen
import com.alvin.pulselink.presentation.senior.settings.SettingsScreen
import com.alvin.pulselink.presentation.senior.voice.VoiceAssistantScreen

/**
 * 应用全局导航配置
 * 统一管理所有页面的导航逻辑
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.AuthCheck.route
) {
    val context = LocalContext.current
    val localDataSource = LocalDataSource(context)
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ===== 启动时的身份验证检查 =====
        composable(route = Screen.AuthCheck.route) {
            AuthCheckScreen(
                localDataSource = localDataSource,
                onNavigateToSeniorHome = {
                    navController.navigate(Screen.SeniorHome.route) {
                        // 清空返回栈，防止用户返回到 AuthCheck 页面
                        popUpTo(Screen.AuthCheck.route) { inclusive = true }
                    }
                },
                onNavigateToCaregiverHome = {
                    navController.navigate(Screen.CaregiverHome.route) {
                        // 清空返回栈，防止用户返回到 AuthCheck 页面
                        popUpTo(Screen.AuthCheck.route) { inclusive = true }
                    }
                },
                onNavigateToWelcome = {
                    navController.navigate(Screen.Welcome.route) {
                        // 清空返回栈，防止用户返回到 AuthCheck 页面
                        popUpTo(Screen.AuthCheck.route) { inclusive = true }
                    }
                }
            )
        }
        
        // ===== 欢迎页 - 角色选择入口 =====
        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToSeniorLogin = {
                    navController.navigate(Screen.Login.createRoute("senior"))
                },
                onNavigateToCaregiverLogin = {
                    navController.navigate(Screen.Login.createRoute("caregiver"))
                }
            )
        }
        
        // ===== 登录页 - 接收 role 参数 =====
        composable(
            route = Screen.Login.route,
            arguments = listOf(
                navArgument("role") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "senior"
            val viewModel: AuthViewModel = hiltViewModel()
            
            LoginScreen(
                role = role,
                viewModel = viewModel,
                onNavigateToHome = {
                    // 根据角色导航到不同的 Home 页面
                    val homeRoute = when (role) {
                        "senior" -> Screen.SeniorHome.route
                        "caregiver" -> Screen.CaregiverHome.route
                        else -> Screen.SeniorHome.route
                    }
                    
                    navController.navigate(homeRoute) {
                        // 清空返回栈，防止用户按返回键回到登录页
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.createRoute(role))
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.createRoute(role))
                }
            )
        }
        
        // ===== 注册页 - 接收 role 参数 =====
        composable(
            route = Screen.Register.route,
            arguments = listOf(
                navArgument("role") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "senior"
            val viewModel: AuthViewModel = hiltViewModel()
            
            RegisterScreen(
                role = role,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.createRoute(role)) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onRegisterSuccess = { email ->
                    navController.navigate(Screen.EmailVerification.createRoute(email)) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }
        
        // ===== 忘记密码 - 接收 role 参数 =====
        composable(
            route = Screen.ForgotPassword.route,
            arguments = listOf(
                navArgument("role") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "senior"
            
            ForgotPasswordScreen(
                role = role,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onResetSuccess = {
                    navController.navigate(Screen.Login.createRoute(role)) {
                        popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                    }
                }
            )
        }
        
        // ===== 邮箱验证页 =====
        composable(
            route = Screen.EmailVerification.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            
            EmailVerificationScreen(
                email = email,
                onNavigateToLogin = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.EmailVerification.route) { inclusive = true }
                    }
                }
            )
        }
        
        // ===== 老人端主页 =====
        composable(route = Screen.SeniorHome.route) {
            HomeScreen(
                onNavigateToHealthData = {
                    navController.navigate(Screen.HealthData.route)
                },
                onNavigateToHealthHistory = {
                    navController.navigate(Screen.HealthHistory.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.SeniorProfile.route)
                },
                onNavigateToReminder = {
                    navController.navigate(Screen.Reminder.route)
                },
                onNavigateToAssistant = {
                    navController.navigate(Screen.VoiceAssistant.route)
                }
            )
        }
        
        // ===== 子女端主页 =====
        composable(route = Screen.CaregiverHome.route) {
            val viewModel: CareDashboardViewModel = hiltViewModel()
            CareDashboardScreen(
                viewModel = viewModel,
                onNavigateToChat = {
                    navController.navigate(Screen.CareChat.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.CaregiverProfile.route)
                },
                onLovedOneClick = { /* TODO: Navigate to specific loved one detail */ }
            )
        }
        
        // ===== 老人端功能页 =====
        composable(route = Screen.HealthData.route) {
            HealthReportScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigate(Screen.SeniorHome.route) },
                onNavigateToProfile = { navController.navigate(Screen.SeniorProfile.route) },
                onNavigateToAssistant = { navController.navigate(Screen.VoiceAssistant.route) }
            )
        }
        
        composable(route = Screen.HealthHistory.route) {
            HealthHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = { navController.navigate(Screen.SeniorHome.route) },
                onNavigateProfile = { navController.navigate(Screen.SeniorProfile.route) },
                onNavigateAssistant = { navController.navigate(Screen.VoiceAssistant.route) },
                onNavigateToHealthReport = { navController.navigate(Screen.HealthData.route) }
            )
        }
        
        composable(route = Screen.SeniorProfile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigate(Screen.SeniorHome.route) },
                onNavigateToAssistant = { navController.navigate(Screen.VoiceAssistant.route) },
                onNavigateToReminderList = { navController.navigate(Screen.ReminderList.route) },
                onNavigateToSettings = { navController.navigate(Screen.SeniorSettings.route) },
                onLogout = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.SeniorHome.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(route = Screen.SeniorSettings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = { navController.navigate(Screen.SeniorHome.route) },
                onNavigateToAssistant = { navController.navigate(Screen.VoiceAssistant.route) },
                onNavigateToProfile = { navController.navigate(Screen.SeniorProfile.route) }
            )
        }
        
        composable(route = Screen.Reminder.route) {
            ReminderScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(route = Screen.ReminderList.route) {
            ReminderListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigate(Screen.SeniorHome.route) },
                onNavigateToProfile = { navController.navigate(Screen.SeniorProfile.route) }
            )
        }
        
        composable(route = Screen.VoiceAssistant.route) {
            VoiceAssistantScreen(
                onClose = { navController.popBackStack() },
                onNavigateHome = { navController.navigate(Screen.SeniorHome.route) },
                onNavigateProfile = { navController.navigate(Screen.SeniorProfile.route) }
            )
        }
        
        // ===== 子女端功能页 =====
        composable(route = Screen.CareChat.route) {
            val viewModel: CareDashboardViewModel = hiltViewModel()
            CareChatScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigate(Screen.CaregiverHome.route) },
                onNavigateToProfile = { navController.navigate(Screen.CaregiverProfile.route) },
                onLovedOneClick = { /* TODO: Navigate to specific loved one detail */ }
            )
        }
        
        composable(route = Screen.CaregiverProfile.route) {
            val viewModel: CaregiverProfileViewModel = hiltViewModel()
            CaregiverProfileScreen(
                viewModel = viewModel,
                onNavigateToHome = { navController.navigate(Screen.CaregiverHome.route) },
                onNavigateToChat = { navController.navigate(Screen.CareChat.route) },
                onNavigateToSettings = { navController.navigate(Screen.CareSettings.route) },
                onNavigateToManageSeniors = { navController.navigate(Screen.ManageSeniors.route) },
                onNavigateToCreateSenior = { navController.navigate(Screen.CreateSenior.route) },
                onNavigateToManageFamily = { navController.navigate(Screen.LinkSenior.route) },
                onNavigateToLinkGuard = { navController.navigate(Screen.FamilyRequests.route) },
                onNavigateToPrivacySecurity = { /* TODO: Implement */ },
                onNavigateToHelpCenter = { /* TODO: Implement */ },
                onLogout = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.CaregiverHome.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(route = Screen.CareSettings.route) {
            CareSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onAccountDeleted = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.CaregiverHome.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(route = Screen.ManageSeniors.route) {
            ManageSeniorsScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateSenior = {
                    navController.navigate(Screen.CreateSenior.route)
                },
                onEditSenior = { id ->
                    // TODO: Implement edit functionality if needed
                }
            )
        }
        
        composable(route = Screen.CreateSenior.route) {
            CreateSeniorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(route = Screen.LinkSenior.route) {
            LinkSeniorScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate(Screen.LinkHistory.route) }
            )
        }
        
        composable(route = Screen.LinkHistory.route) {
            LinkHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(route = Screen.FamilyRequests.route) {
            FamilyRequestsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
