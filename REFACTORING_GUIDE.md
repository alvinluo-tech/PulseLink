# Presentation 层重组实施指南

## 已完成的工作 ✅

1. **创建新的导航结构**
   - ✅ `presentation/nav/Screen.kt` - 新的路由定义，支持角色参数
   - ✅ 定义了 `Role` 常量对象 (SENIOR, CAREGIVER)

2. **创建统一的认证模块**
   - ✅ `presentation/auth/AuthViewModel.kt` - 统一的登录/注册逻辑
   - ✅ `presentation/auth/AuthUiState.kt` - 统一的 UI 状态

## 下一步操作指南

### 步骤 1: 移动认证相关文件到 auth/

使用 PowerShell 或手动移动以下文件：

```powershell
# 1. 移动 Welcome 页面
Move-Item "presentation\welcome\WelcomeScreen.kt" "presentation\auth\WelcomeScreen.kt"

# 2. 移动登录页面（需要修改为接收 role 参数）
Copy-Item "presentation\login\LoginScreen.kt" "presentation\auth\LoginScreen.kt"

# 3. 移动注册页面
Copy-Item "presentation\register\RegisterScreen.kt" "presentation\auth\RegisterScreen.kt"
Copy-Item "presentation\register\RegisterForm.kt" "presentation\auth\RegisterForm.kt"

# 4. 移动忘记密码页面
Copy-Item "presentation\forgotpassword\ForgotPasswordScreen.kt" "presentation\auth\ForgotPasswordScreen.kt"
Copy-Item "presentation\forgotpassword\ForgotPasswordViewModel.kt" "presentation\auth\ForgotPasswordViewModel.kt"
Copy-Item "presentation\forgotpassword\ForgotPasswordUiState.kt" "presentation\auth\ForgotPasswordUiState.kt"

# 5. 移动邮箱验证页面
Copy-Item "presentation\verification\EmailVerificationScreen.kt" "presentation\auth\EmailVerificationScreen.kt"
```

### 步骤 2: 修改 LoginScreen.kt 接收 role 参数

在 `auth/LoginScreen.kt` 中修改：

```kotlin
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    role: String, // 新增参数：senior 或 caregiver
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val userRole = if (role == Role.SENIOR) UserRole.SENIOR else UserRole.CAREGIVER
    
    // 根据 role 选择不同的配色
    val backgroundColor = if (role == Role.SENIOR) {
        Brush.verticalGradient(listOf(Color(0xFFE3E8F0), Color(0xFFD6DDEB)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFE8D7F0), Color(0xFFDDCBE8)))
    }
    
    // ... 其余代码
}
```

### 步骤 3: 重组 Senior 端文件

创建目录并移动文件：

```powershell
# 创建目录
New-Item -ItemType Directory -Path "presentation\senior\home"
New-Item -ItemType Directory -Path "presentation\senior\health"
New-Item -ItemType Directory -Path "presentation\senior\history"
New-Item -ItemType Directory -Path "presentation\senior\reminder"
New-Item -ItemType Directory -Path "presentation\senior\voice"
New-Item -ItemType Directory -Path "presentation\senior\profile"

# 移动文件
Move-Item "presentation\home\*" "presentation\senior\home\"
Move-Item "presentation\health\*" "presentation\senior\health\"
Move-Item "presentation\history\*" "presentation\senior\history\"
Move-Item "presentation\reminder\*" "presentation\senior\reminder\"
Move-Item "presentation\reminderlist\*" "presentation\senior\reminder\"
Move-Item "presentation\assistant\*" "presentation\senior\voice\"
Move-Item "presentation\profile\*" "presentation\senior\profile\"
```

### 步骤 4: 重组 Caregiver 端文件

```powershell
# 创建目录
New-Item -ItemType Directory -Path "presentation\caregiver\dashboard"
New-Item -ItemType Directory -Path "presentation\caregiver\chat"
New-Item -ItemType Directory -Path "presentation\caregiver\profile"
New-Item -ItemType Directory -Path "presentation\caregiver\settings"
New-Item -ItemType Directory -Path "presentation\caregiver\family"

# 移动现有文件
Move-Item "presentation\caregiver\CareDashboardScreen.kt" "presentation\caregiver\dashboard\"
Move-Item "presentation\caregiver\CareDashboardViewModel.kt" "presentation\caregiver\dashboard\"
Move-Item "presentation\caregiver\CareChatScreen.kt" "presentation\caregiver\chat\"
Move-Item "presentation\caregiver\CaregiverProfileScreen.kt" "presentation\caregiver\profile\"
Move-Item "presentation\caregiver\CaregiverProfileViewModel.kt" "presentation\caregiver\profile\"
```

### 步骤 5: 更新包名

所有移动的文件需要更新 package 声明：

- `auth/` 下的文件: `package com.alvin.pulselink.presentation.auth`
- `senior/home/` 下的文件: `package com.alvin.pulselink.presentation.senior.home`
- `senior/health/` 下的文件: `package com.alvin.pulselink.presentation.senior.health`
- 等等...

### 步骤 6: 创建 SeniorMainScreen 和 CaregiverMainScreen

这两个文件作为各自端的主容器，包含 Scaffold 和底部导航栏。

**SeniorMainScreen.kt:**
```kotlin
package com.alvin.pulselink.presentation.senior

@Composable
fun SeniorMainScreen(
    navController: NavHostController
) {
    Scaffold(
        bottomBar = {
            SeniorBottomNavigation(navController)
        }
    ) { paddingValues ->
        SeniorNavHost(
            navController = navController,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
```

**CaregiverMainScreen.kt:**
```kotlin
package com.alvin.pulselink.presentation.caregiver

@Composable
fun CaregiverMainScreen(
    navController: NavHostController
) {
    Scaffold(
        bottomBar = {
            CaregiverBottomNavigation(navController)
        }
    ) { paddingValues ->
        CaregiverNavHost(
            navController = navController,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
```

### 步骤 7: 创建主导航图

**AppNavigation.kt:**
```kotlin
package com.alvin.pulselink.presentation.nav

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        // 认证流程
        authNavGraph(navController)
        
        // 老人端
        seniorNavGraph(navController)
        
        // 子女端
        caregiverNavGraph(navController)
    }
}
```

### 步骤 8: 更新 import 语句

使用全局查找替换：

1. 将所有 `com.alvin.pulselink.presentation.navigation.Screen` 替换为 `com.alvin.pulselink.presentation.nav.Screen`
2. 将所有 `com.alvin.pulselink.presentation.login` 替换为 `com.alvin.pulselink.presentation.auth`
3. 将所有 `com.alvin.pulselink.presentation.register` 替换为 `com.alvin.pulselink.presentation.auth`
4. 等等...

### 步骤 9: 删除旧目录

确认所有文件都已正确迁移后，删除旧目录：

```powershell
Remove-Item "presentation\navigation" -Recurse
Remove-Item "presentation\login" -Recurse
Remove-Item "presentation\register" -Recurse
Remove-Item "presentation\welcome" -Recurse
Remove-Item "presentation\forgotpassword" -Recurse
Remove-Item "presentation\verification" -Recurse
Remove-Item "presentation\home" -Recurse
Remove-Item "presentation\health" -Recurse
Remove-Item "presentation\history" -Recurse
Remove-Item "presentation\reminder" -Recurse
Remove-Item "presentation\reminderlist" -Recurse
Remove-Item "presentation\assistant" -Recurse
Remove-Item "presentation\profile" -Recurse
```

### 步骤 10: 创建通用组件

在 `presentation/common/components/` 下创建：

1. **Logo.kt** - 应用 Logo 组件
2. **PrimaryButton.kt** - 主要按钮样式
3. **OutlinedInputField.kt** - 输入框组件
4. **LoadingIndicator.kt** - 加载指示器

### 步骤 11: 移动主题文件

```powershell
New-Item -ItemType Directory -Path "presentation\common\theme"
Move-Item "ui\theme\*" "presentation\common\theme\"
```

更新 package 为 `com.alvin.pulselink.presentation.common.theme`

## 测试清单

完成重组后，请测试：

- [ ] 欢迎页正常显示
- [ ] 老人端登录流程正常
- [ ] 子女端登录流程正常
- [ ] 老人端注册流程正常
- [ ] 子女端注册流程正常
- [ ] 老人端主页导航正常
- [ ] 子女端仪表盘导航正常
- [ ] 底部导航栏功能正常
- [ ] 页面间跳转无误
- [ ] 所有 import 都正确

## 优势

完成重组后，您将获得：

1. **更清晰的代码结构** - 一目了然地区分两端功能
2. **减少代码重复** - 共用的认证逻辑统一管理
3. **更好的可维护性** - 修改一处即可影响两端
4. **更容易扩展** - 添加新功能时知道放在哪里
5. **更好的团队协作** - 不同开发者可以独立工作在不同模块

## 注意事项

1. 建议在 Git 中创建新分支进行重组
2. 每完成一个步骤就提交一次
3. 确保每次提交都能编译通过
4. 保留旧代码直到确认新代码完全工作

## 需要帮助？

如果在重组过程中遇到问题，可以：
1. 查看错误日志
2. 检查 package 声明是否正确
3. 确认 import 语句已更新
4. 验证导航路由配置
