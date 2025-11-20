package com.alvin.pulselink.presentation.caregiver

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.alvin.pulselink.presentation.navigation.Screen

/**
 * 护理者端导航图扩展函数
 * 用于在主导航图中添加护理者相关的界面
 */
fun NavGraphBuilder.caregiverNavGraph(
    navController: NavHostController
) {
    // 护理仪表板
    composable(Screen.CareDashboard.route) {
        val viewModel: CareDashboardViewModel = hiltViewModel()
        CareDashboardScreen(
            viewModel = viewModel,
            onNavigateToChat = {
                navController.navigate(Screen.CareChat.route)
            },
            onNavigateToProfile = {
                navController.navigate(Screen.CaregiverProfile.route)
            },
            onLovedOneClick = { lovedOneId ->
                // TODO: 导航到亲人详情页
                // navController.navigate(Screen.LovedOneDetail.createRoute(lovedOneId))
            }
        )
    }
    
    // 护理聊天
    composable(Screen.CareChat.route) {
        val viewModel: CareDashboardViewModel = hiltViewModel()
        CareChatScreen(
            viewModel = viewModel,
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToHome = {
                navController.navigate(Screen.CareDashboard.route) {
                    popUpTo(Screen.CareDashboard.route) { inclusive = true }
                }
            },
            onNavigateToProfile = {
                navController.navigate(Screen.CaregiverProfile.route)
            },
            onLovedOneClick = { lovedOneId ->
                // TODO: 导航到聊天详情页
                // navController.navigate(Screen.ChatDetail.createRoute(lovedOneId))
            }
        )
    }
    
    // 护理者个人资料
    composable(Screen.CaregiverProfile.route) {
        val viewModel: CaregiverProfileViewModel = hiltViewModel()
        CaregiverProfileScreen(
            viewModel = viewModel,
            onNavigateToHome = {
                navController.navigate(Screen.CareDashboard.route) {
                    popUpTo(Screen.CareDashboard.route) { inclusive = true }
                }
            },
            onNavigateToChat = {
                navController.navigate(Screen.CareChat.route)
            },
            onNavigateToSettings = {
                navController.navigate(Screen.CareSettings.route)
            },
            onNavigateToManageFamily = {
                navController.navigate(Screen.ManageFamily.route)
            },
            onNavigateToPrivacySecurity = {
                navController.navigate(Screen.PrivacySecurity.route)
            },
            onNavigateToHelpCenter = {
                navController.navigate(Screen.HelpCenter.route)
            },
            onLogout = {
                // TODO: 执行登出逻辑
                navController.navigate(Screen.Welcome.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }
}
