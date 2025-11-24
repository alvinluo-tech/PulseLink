# PulseLink - Clean Architecture + Single Activity

## 📐 项目架构（基于当前代码）

本项目采用 Clean Architecture（整洁架构） + Single Activity Architecture（单 Activity 架构）。以下目录结构与当前代码保持一致：

```
app/src/main/java/com/alvin/pulselink/
├── domain/                         # 领域层（业务实体、接口、用例）
│   ├── model/
│   │   └── Senior.kt              # 包含 Senior、HealthHistory、BloodPressureRecord
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   ├── HealthRepository.kt
│   │   └── SeniorRepository.kt
│   └── usecase/
│       ├── CreateSeniorUseCase.kt
│       ├── GetCreatedSeniorsUseCase.kt
│       ├── GetSeniorsUseCase.kt
│       ├── GetHealthDataUseCase.kt
│       ├── LoginUseCase.kt / RegisterUseCase.kt
│       ├── ResetPasswordUseCase.kt / ChangePasswordUseCase.kt
│       ├── DeleteAccountUseCase.kt
│       └── TestFirestoreConnectionUseCase.kt
│
├── data/                           # 数据层（数据源与仓库实现）
│   ├── local/
│   │   └── LocalDataSource.kt
│   └── repository/
│       ├── AuthRepositoryImpl.kt
│       ├── HealthRepositoryImpl.kt
│       └── SeniorRepositoryImpl.kt # Firestore 持久化与查询
│
├── presentation/                   # 表现层（UI、ViewModel、导航）
│   ├── auth/                       # 认证模块（两端通用）
│   │   ├── LoginScreen.kt / AuthViewModel.kt
│   │   ├── RegisterScreen.kt / RegisterForm.kt
│   │   ├── ForgotPasswordScreen.kt / ForgotPasswordViewModel.kt
│   │   └── WelcomeScreen.kt
│   ├── caregiver/                  # 子女端（Caregiver）
│   │   ├── dashboard/
│   │   │   ├── CareDashboardScreen.kt
│   │   │   └── CareDashboardViewModel.kt
│   │   ├── senior/
│   │   │   ├── ManageSeniorsScreen.kt / ManageSeniorsViewModel.kt
│   │   │   ├── CreateSeniorScreen.kt
│   │   │   ├── LinkSeniorScreen.kt / LinkSeniorViewModel.kt
│   │   │   └── LinkSeniorUiState.kt / ManageSeniorsUiState.kt
│   │   ├── profile/
│   │   │   ├── CaregiverProfileScreen.kt / CaregiverProfileViewModel.kt
│   │   ├── settings/
│   │   │   ├── CareSettingsScreen.kt / CareSettingsViewModel.kt
│   │   └── chat/
│   │       └── CareChatScreen.kt
│   ├── senior/                     # 老人端（Senior）
│   │   ├── home/ HomeScreen.kt / HomeViewModel.kt
│   │   ├── health/ HealthReportScreen.kt / HealthReportViewModel.kt
│   │   ├── history/ HealthHistoryScreen.kt / HealthHistoryViewModel.kt
│   │   ├── profile/ ProfileScreen.kt / ProfileViewModel.kt
│   │   ├── reminder/ Reminder* 屏与 ViewModel
│   │   └── voice/ VoiceAssistantScreen.kt / AssistantViewModel.kt
│   ├── common/
│   │   ├── components/ SeniorBottomNavigationBar.kt, UiFeedback.kt
│   │   └── theme/ RoleTheme.kt, RoleThemeProvider.kt
│   └── nav/
│       ├── Screen.kt               # 路由常量
│       └── AppNavigation.kt        # 导航图与路由跳转
│
├── di/
│   └── AppModule.kt                # Hilt 依赖提供者（KSP 代码生成）
│
├── MainActivity.kt                 # 单一 Activity（NavHost 入口）
└── PulseLinkApplication.kt         # @HiltAndroidApp 应用入口
```

## 🏗️ 分层说明（与当前实现对齐）

**Domain Layer（领域层）**
- 实体：`Senior`, `HealthHistory`, `BloodPressureRecord`
- 关键字段：`caregiverIds: List<String>`（支持多个护理人），`creatorId: String`（创建者）
- 仓库接口：`AuthRepository`, `HealthRepository`, `SeniorRepository`
- 用例：认证、健康数据、老人账户创建/查询等业务逻辑封装

**Data Layer（数据层）**
- `SeniorRepositoryImpl`：
  - 创建：初始化 `caregiverIds=[creatorId]`，写入 `creatorId`
  - 查询（按护理人）：`whereArrayContains("caregiverIds", caregiverId)`
  - 查询（按创建者）：`whereEqualTo("creatorId", creatorId)`
  - 更新：保持 `caregiverIds`/`creatorId` 一致性
- 认证与健康数据实现：`AuthRepositoryImpl`, `HealthRepositoryImpl`

**Presentation Layer（表现层）**
- Navigation Compose + 单 Activity；ViewModel 通过用例驱动 UI 状态
- 流程：
  - 创建老人：成功后自动绑定创建者 → 显示成功提示 → 导航 `CaregiverHome`
  - 管理老人：`GetCreatedSeniorsUseCase` 展示“我创建的”列表；支持复制老人 ID
  - 绑定老人：通过 ID 绑定，若已存在于 `caregiverIds` 列表则防重复；更新为 `(caregiverIds + caregiverId).distinct()`

**Dependency Injection（依赖注入）**
- Dagger Hilt（KSP）；`AppModule.kt` 提供仓库与用例；`@HiltAndroidApp` 入口，`@AndroidEntryPoint`/`@HiltViewModel` 注入点

## 🔄 数据流（当前实现）

```
UI (Composable Screen)
    ↓ 用户操作
ViewModel
    ↓ 调用
UseCase（业务逻辑）
    ↓ 调用
Repository 接口
    ↓ 实现
Repository 实现（Firestore/DataStore 等）
    ↓ 读写
数据源（Firebase Firestore / 本地）
```

## 🧭 导航与路由

- 路由常量在 `presentation/nav/Screen.kt`
- 导航图与跳转在 `presentation/nav/AppNavigation.kt`
- 关键路由：`Welcome`、`CaregiverHome`（CareDashboard）、`ManageSeniors`、`CreateSenior`、`LinkSenior`、以及老人端的 `SeniorHome`、`HealthData`、`HealthHistory` 等

## 📦 技术栈

- UI：Jetpack Compose（Material 3）
- 架构：Clean Architecture + MVVM + Single Activity
- 导航：Navigation Compose
- 依赖注入：Dagger Hilt（KSP）
- 异步：Kotlin Coroutines + Flow
- 数据：Firebase Firestore；本地 DataStore（可扩展）
- 构建：Gradle Kotlin DSL + Version Catalog（`gradle/libs.versions.toml`）

## 🧪 测试账号（示例）

- 用户名：`alvin`
- 密码：`123456`

## 🚀 运行项目

1. 打开 Android Studio
2. 同步 Gradle：File → Sync Project with Gradle Files
3. 构建调试包：在根目录运行 `./gradlew assembleDebug -x test`
4. 运行应用：Run → Run 'app'

## 📝 代码规范

- 遵循整洁架构，每层仅依赖内层
- 全局 Single Activity，页面为 Composable
- ViewModel 通过 UseCase 访问仓库
- UI State 使用不可变 `data class`
- 路由集中定义，导航在单处维护

## 🎯 选择 Single Activity 的原因

- 更低的切换开销与更流畅的转场
- 更清晰的生命周期与作用域管理
- 更少的内存占用，代码更简洁
- 符合 Google 推荐的 Compose 最佳实践