package com.alvin.pulselink.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.alvin.pulselink.data.local.LocalDataSource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 启动时的身份验证检查页面
 * 
 * 功能：
 * 1. 检查 Firebase Auth 的 currentUser
 * 2. 如果已登录，读取 LocalDataSource 中的角色信息
 * 3. 根据角色导航到对应的主页（Senior Home 或 Caregiver Home）
 * 4. 如果未登录，导航到欢迎页
 */
@Composable
fun AuthCheckScreen(
    localDataSource: LocalDataSource,
    onNavigateToSeniorHome: () -> Unit,
    onNavigateToCaregiverHome: () -> Unit,
    onNavigateToWelcome: () -> Unit
) {
    // 显示加载指示器
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
    
    // 启动时执行身份验证检查
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // 检查 Firebase Auth 的 currentUser
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            
            if (firebaseUser != null) {
                // 用户已登录，读取本地缓存的角色信息
                val userInfo = localDataSource.getUser()
                
                if (userInfo != null) {
                    val (_, _, role) = userInfo
                    
                    // 根据角色导航到对应的主页
                    withContext(Dispatchers.Main) {
                        when (role?.uppercase()) {
                            "SENIOR" -> onNavigateToSeniorHome()
                            "CAREGIVER" -> onNavigateToCaregiverHome()
                            else -> {
                                // 角色信息异常，返回欢迎页
                                onNavigateToWelcome()
                            }
                        }
                    }
                } else {
                    // 本地缓存为空，可能是首次登录或数据异常
                    // 返回欢迎页让用户重新登录
                    withContext(Dispatchers.Main) {
                        onNavigateToWelcome()
                    }
                }
            } else {
                // 用户未登录，导航到欢迎页
                withContext(Dispatchers.Main) {
                    onNavigateToWelcome()
                }
            }
        }
    }
}
