# PulseLink Presentation 层新架构

## 目录结构

```
com.alvin.pulselink.presentation/
│
├── 📁 nav/                                    # 导航层
│   ├── Screen.kt                              # 路由定义（统一管理）
│   ├── AppNavigation.kt                       # 主导航宿主
│   └── Role.kt                                # 角色常量
│
├── 📁 common/                                 # 共用模块
│   ├── 📁 components/                         # 通用 UI 组件
│   │   ├── Logo.kt                            # 应用 Logo
│   │   ├── PrimaryButton.kt                   # 主按钮
│   │   ├── OutlinedInputField.kt              # 输入框
│   │   └── LoadingIndicator.kt                # 加载指示器
│   └── 📁 theme/                              # 主题配置
│       ├── Color.kt                           # 颜色定义
│       ├── Theme.kt                           # 主题配置
│       └── Type.kt                            # 字体样式
│
├── 📁 auth/                                   # 认证模块（两端共用）
│   ├── AuthViewModel.kt                       # 统一的认证 VM
│   ├── AuthUiState.kt                         # 认证状态
│   ├── WelcomeScreen.kt                       # 角色选择页
│   ├── LoginScreen.kt                         # 登录页（接收 role 参数）
│   ├── RegisterScreen.kt                      # 注册页（接收 role 参数）
│   ├── RegisterForm.kt                        # 注册表单组件
│   ├── ForgotPasswordScreen.kt                # 忘记密码
│   ├── ForgotPasswordViewModel.kt
│   ├── ForgotPasswordUiState.kt
│   └── EmailVerificationScreen.kt             # 邮箱验证
│
├── 📁 senior/                                 # 老人端专属
│   │
│   ├── SeniorMainScreen.kt                    # 老人端主容器（Scaffold）
│   │
│   ├── 📁 home/                               # 主页
│   │   ├── SeniorHomeScreen.kt
│   │   ├── HomeViewModel.kt
│   │   └── HomeUiState.kt
│   │
│   ├── 📁 health/                             # 健康数据
│   │   ├── HealthDataScreen.kt
│   │   ├── HealthDataViewModel.kt
│   │   └── HealthDataUiState.kt
│   │
│   ├── 📁 history/                            # 健康历史
│   │   ├── HealthHistoryScreen.kt
│   │   ├── HealthHistoryViewModel.kt
│   │   └── HealthHistoryUiState.kt
│   │
│   ├── 📁 reminder/                           # 提醒功能
│   │   ├── ReminderScreen.kt                  # 添加/编辑提醒
│   │   ├── ReminderListScreen.kt              # 提醒列表
│   │   ├── ReminderViewModel.kt
│   │   ├── ReminderListViewModel.kt
│   │   └── ReminderUiState.kt
│   │
│   ├── 📁 voice/                              # 语音助手
│   │   ├── VoiceAssistantScreen.kt
│   │   ├── AssistantViewModel.kt
│   │   └── AssistantUiState.kt
│   │
│   └── 📁 profile/                            # 个人资料
│       ├── SeniorProfileScreen.kt
│       ├── ProfileViewModel.kt
│       └── ProfileUiState.kt
│
└── 📁 caregiver/                              # 子女端专属
    │
    ├── CaregiverMainScreen.kt                 # 子女端主容器（Scaffold）
    │
    ├── 📁 dashboard/                          # 仪表盘
    │   ├── CareDashboardScreen.kt
    │   ├── CareDashboardViewModel.kt
    │   ├── LovedOne.kt                        # 数据模型
    │   └── components/
    │       ├── StatusCard.kt
    │       ├── LovedOneCard.kt
    │       └── CareNavigationBar.kt
    │
    ├── 📁 chat/                               # 护理聊天
    │   ├── CareChatScreen.kt                  # 选择亲人
    │   ├── CareChatDetailScreen.kt            # 聊天详情
    │   └── CareChatViewModel.kt
    │
    ├── 📁 profile/                            # 个人资料
    │   ├── CaregiverProfileScreen.kt
    │   └── CaregiverProfileViewModel.kt
    │
    ├── 📁 settings/                           # 设置
    │   ├── SettingsScreen.kt
    │   ├── AlertThresholdScreen.kt            # 报警阈值设置
    │   └── SettingsViewModel.kt
    │
    └── 📁 family/                             # 家庭成员管理
        ├── ManageFamilyScreen.kt
        ├── AddFamilyMemberScreen.kt
        └── FamilyViewModel.kt
```

## 架构优势

### 1. 清晰的关注点分离

- **nav/** - 只关心路由和导航
- **common/** - 可复用的组件和主题
- **auth/** - 认证逻辑统一管理
- **senior/** - 老人端功能独立
- **caregiver/** - 子女端功能独立

### 2. 避免代码重复

- 登录/注册逻辑统一在 `AuthViewModel`
- 通用 UI 组件在 `common/components/`
- 主题配置统一管理

### 3. 易于维护和扩展

- 添加老人端功能：在 `senior/` 下创建新目录
- 添加子女端功能：在 `caregiver/` 下创建新目录
- 修改认证流程：只需修改 `auth/` 模块

### 4. 支持角色切换

```kotlin
// 登录时传入角色
Screen.Login.createRoute(Role.SENIOR)     // 老人端登录
Screen.Login.createRoute(Role.CAREGIVER)  // 子女端登录

// 根据角色导航到不同主页
if (role == Role.SENIOR) {
    navController.navigate(Screen.SeniorHome.route)
} else {
    navController.navigate(Screen.CareDashboard.route)
}
```

## 导航流程

### 老人端流程
```
Welcome → Login(senior) → SeniorHome
              ↓
        EmailVerification
              ↓
          SeniorHome → Health/History/Reminder/Voice/Profile
```

### 子女端流程
```
Welcome → Login(caregiver) → CareDashboard
              ↓
        EmailVerification
              ↓
          CareDashboard → Chat/Profile/Settings/Family
```

## 共用认证流程

```
WelcomeScreen
    ↓
选择角色 (Senior/Caregiver)
    ↓
LoginScreen(role)  ← 同一个组件，根据 role 显示不同样式
    ↓
AuthViewModel.login(role)  ← 同一个 VM，处理两种角色
    ↓
EmailVerificationScreen(role)
    ↓
导航到对应的主页
```

## 主题切换

```kotlin
// 在 common/theme/Theme.kt
@Composable
fun PulseLinkTheme(
    role: String = Role.SENIOR,
    content: @Composable () -> Unit
) {
    val colorScheme = if (role == Role.SENIOR) {
        seniorColorScheme()
    } else {
        caregiverColorScheme()
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

## ViewModel 作用域

- **AuthViewModel**: Application 级别（整个应用共享）
- **SeniorHomeViewModel**: Senior 导航图级别
- **CareDashboardViewModel**: Caregiver 导航图级别
- **其他 ViewModels**: Screen 级别

## 数据流

```
UI (Screen)
    ↓
ViewModel
    ↓
UseCase (Domain Layer)
    ↓
Repository (Data Layer)
    ↓
DataSource (Firebase/Local)
```

## 测试策略

1. **Unit Tests**
   - ViewModels
   - UseCases
   - Repositories

2. **UI Tests**
   - Screen 组件
   - Navigation 流程

3. **Integration Tests**
   - 端到端用户流程

## 迁移检查清单

- [ ] 文件已移动到新目录
- [ ] Package 声明已更新
- [ ] Import 语句已更新
- [ ] 导航路由已更新
- [ ] ViewModel 注入正确
- [ ] 编译通过
- [ ] 所有功能正常工作
- [ ] 主题正确应用
- [ ] 角色切换正常
- [ ] 删除旧文件/目录
