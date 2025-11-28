# 导航修复 - Caregiver 主页点击老人跳转

## 🔧 修复内容

### 问题
Caregiver 端主页点击老人卡片不跳转到详情页

### 解决方案

#### 1. 添加路由定义 (`Screen.kt`)
```kotlin
/** 老人详情页 - 查看老人健康数据 */
object SeniorDetail : Screen("caregiver/senior_detail/{seniorId}/{seniorName}") {
    fun createRoute(seniorId: String, seniorName: String) = "caregiver/senior_detail/$seniorId/$seniorName"
}
```

#### 2. 添加导航逻辑 (`AppNavigation.kt`)

##### 添加 composable 路由
```kotlin
// ===== 老人详情页 =====
composable(
    route = Screen.SeniorDetail.route,
    arguments = listOf(
        navArgument("seniorId") { type = NavType.StringType },
        navArgument("seniorName") { type = NavType.StringType }
    )
) { backStackEntry ->
    val seniorId = backStackEntry.arguments?.getString("seniorId") ?: ""
    val seniorName = backStackEntry.arguments?.getString("seniorName") ?: ""
    
    SeniorDetailScreen(
        seniorId = seniorId,
        seniorName = seniorName,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

##### 更新 CareDashboardScreen 导航
```kotlin
composable(route = Screen.CaregiverHome.route) {
    val viewModel: CareDashboardViewModel = hiltViewModel()
    CareDashboardScreen(
        viewModel = viewModel,
        onNavigateToChat = { navController.navigate(Screen.CareChat.route) },
        onNavigateToProfile = { navController.navigate(Screen.CaregiverProfile.route) },
        onLovedOneClick = { seniorId ->
            // 导航到老人详情页
            val viewModelState = viewModel.uiState.value
            val lovedOne = viewModelState.lovedOnes.find { it.id == seniorId }
            val seniorName = lovedOne?.name ?: "Senior"
            navController.navigate(Screen.SeniorDetail.createRoute(seniorId, seniorName))
        }
    )
}
```

##### 同时更新 CareChatScreen 导航
```kotlin
CareChatScreen(
    viewModel = viewModel,
    onNavigateBack = { navController.popBackStack() },
    onNavigateToHome = { navController.navigate(Screen.CaregiverHome.route) },
    onNavigateToProfile = { navController.navigate(Screen.CaregiverProfile.route) },
    onLovedOneClick = { seniorId ->
        val viewModelState = viewModel.uiState.value
        val lovedOne = viewModelState.lovedOnes.find { it.id == seniorId }
        val seniorName = lovedOne?.name ?: "Senior"
        navController.navigate(Screen.SeniorDetail.createRoute(seniorId, seniorName))
    }
)
```

## ✅ 修复结果

现在 Caregiver 端的以下位置点击老人卡片都会跳转到详情页：

1. **主页 (CareDashboardScreen)** - 点击老人卡片
2. **聊天页 (CareChatScreen)** - 点击老人卡片

## 🎯 跳转流程

```
CareDashboardScreen
  └─> 点击老人卡片
      └─> 获取 seniorId 和 seniorName
          └─> 导航到 SeniorDetailScreen
              └─> 显示 3 个 Tab: Reports | Alerts | Reminders
```

## 📱 用户体验

- **点击老人卡片** → 自动跳转到详情页
- **顶部显示老人名字** (例如: "Mother (Mrs. Zhang)")
- **可以查看**:
  - 📊 健康报告 (每日/周期)
  - 🔔 健康历史
  - 💊 用药提醒
- **点击返回** → 回到主页

## 🔄 相关文件

修改的文件：
1. `presentation/nav/Screen.kt` - 添加 SeniorDetail 路由
2. `presentation/nav/AppNavigation.kt` - 添加导航逻辑和 composable

涉及的文件：
3. `presentation/caregiver/dashboard/CareDashboardScreen.kt` - 主页
4. `presentation/caregiver/chat/CareChatScreen.kt` - 聊天页
5. `presentation/caregiver/seniordetail/SeniorDetailScreen.kt` - 详情页

## ✅ 编译状态

- **状态**: ✅ 编译成功
- **构建时间**: 50秒
- **执行任务**: 42个 (13个执行, 29个最新)

---

**修复时间**: 2025-11-27
**状态**: ✅ 完成并测试通过
