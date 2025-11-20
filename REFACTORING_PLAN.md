# Presentation 层重组计划

## 目标架构

```
presentation/
├── nav/                          # 全局导航逻辑
│   ├── AppNavigation.kt          # 导航宿主 (NavHost)
│   └── Screen.kt                 # 路由定义
│
├── common/                       # 两端共用的 UI 组件和逻辑
│   ├── components/               # 通用原子组件
│   │   ├── Logo.kt
│   │   ├── PrimaryButton.kt
│   │   ├── OutlinedInputField.kt
│   │   └── LoadingIndicator.kt
│   └── theme/                    # 主题（移动 ui/theme 到这里）
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── auth/                         # 共用的认证模块
│   ├── AuthViewModel.kt          # 统一的登录/注册逻辑
│   ├── LoginScreen.kt            # 登录页 (接收 role 参数)
│   ├── RegisterScreen.kt         # 注册页 (接收 role 参数)
│   ├── ForgotPasswordScreen.kt   # 忘记密码
│   ├── EmailVerificationScreen.kt# 邮箱验证
│   └── WelcomeScreen.kt          # 角色选择页
│
├── senior/                       # 老人端专属功能
│   ├── home/                     # 老人主页
│   │   ├── SeniorHomeScreen.kt
│   │   └── SeniorHomeViewModel.kt
│   ├── health/                   # 健康数据
│   │   ├── HealthDataScreen.kt
│   │   └── HealthDataViewModel.kt
│   ├── history/                  # 健康历史
│   │   ├── HealthHistoryScreen.kt
│   │   └── HealthHistoryViewModel.kt
│   ├── reminder/                 # 提醒功能
│   │   ├── ReminderScreen.kt
│   │   ├── ReminderListScreen.kt
│   │   └── ReminderViewModel.kt
│   ├── voice/                    # 语音助手
│   │   ├── VoiceAssistantScreen.kt
│   │   └── AssistantViewModel.kt
│   ├── profile/                  # 老人个人资料
│   │   ├── SeniorProfileScreen.kt
│   │   └── ProfileViewModel.kt
│   └── SeniorMainScreen.kt       # 老人端的 Scaffold
│
└── caregiver/                    # 子女端专属功能
    ├── dashboard/                # 仪表盘
    │   ├── CareDashboardScreen.kt
    │   └── CareDashboardViewModel.kt
    ├── chat/                     # 护理聊天
    │   ├── CareChatScreen.kt
    │   └── CareChatViewModel.kt
    ├── profile/                  # 子女个人资料
    │   ├── CaregiverProfileScreen.kt
    │   └── CaregiverProfileViewModel.kt
    ├── settings/                 # 设置（报警阈值等）
    │   ├── SettingsScreen.kt
    │   └── SettingsViewModel.kt
    ├── family/                   # 家庭成员管理
    │   ├── ManageFamilyScreen.kt
    │   └── ManageFamilyViewModel.kt
    └── CaregiverMainScreen.kt    # 子女端的 Scaffold
```

## 迁移映射表

### 1. nav/ (导航)
- ✅ `navigation/Screen.kt` → `nav/Screen.kt`
- ✅ `navigation/NavGraph.kt` → `nav/AppNavigation.kt`

### 2. common/ (共用组件)
- 将创建新的通用组件
- 从 `ui/theme/` 移动主题文件

### 3. auth/ (认证)
- `login/LoginScreen.kt` → `auth/LoginScreen.kt`
- `login/LoginViewModel.kt` → `auth/AuthViewModel.kt` (合并)
- `login/LoginUiState.kt` → `auth/AuthUiState.kt`
- `register/RegisterScreen.kt` → `auth/RegisterScreen.kt`
- `register/RegisterViewModel.kt` → `auth/AuthViewModel.kt` (合并)
- `forgotpassword/` → `auth/ForgotPasswordScreen.kt`
- `verification/` → `auth/EmailVerificationScreen.kt`
- `welcome/WelcomeScreen.kt` → `auth/WelcomeScreen.kt`

### 4. senior/ (老人端)
- `home/` → `senior/home/`
- `health/` → `senior/health/`
- `history/` → `senior/history/`
- `reminder/` + `reminderlist/` → `senior/reminder/`
- `assistant/` → `senior/voice/`
- `profile/` → `senior/profile/`

### 5. caregiver/ (子女端)
- `caregiver/CareDashboardScreen.kt` → `caregiver/dashboard/`
- `caregiver/CareChatScreen.kt` → `caregiver/chat/`
- `caregiver/CaregiverProfileScreen.kt` → `caregiver/profile/`

## 执行步骤

### 阶段 1: 创建新目录结构 ✅
1. 创建 `nav/` 目录
2. 创建 `common/` 目录和子目录
3. 创建 `auth/` 目录
4. 创建 `senior/` 及其子目录
5. 创建 `caregiver/` 及其子目录

### 阶段 2: 移动和重构 auth 模块
1. 创建统一的 AuthViewModel
2. 移动 Login 相关文件
3. 移动 Register 相关文件
4. 移动其他认证相关文件

### 阶段 3: 移动 senior 模块
1. 移动 home
2. 移动 health
3. 移动 history
4. 移动 reminder
5. 移动 voice
6. 移动 profile

### 阶段 4: 移动 caregiver 模块
1. 重组 dashboard
2. 重组 chat
3. 重组 profile
4. 创建其他页面

### 阶段 5: 创建 common 组件
1. 提取通用组件
2. 移动主题文件

### 阶段 6: 更新导航
1. 更新 Screen.kt
2. 重写 AppNavigation.kt
3. 更新所有引用

### 阶段 7: 清理
1. 删除旧目录
2. 更新 import 语句
3. 测试编译
