# PulseLink - Clean Architecture + Single Activity

## 📐 项目架构

本项目采用 **Clean Architecture（整洁架构）** + **Single Activity Architecture（单 Activity 架构）** 设计模式。

```
app/src/main/java/com/alvin/pulselink/
├── domain/              # 领域层（Domain Layer）
│   ├── model/          # 领域实体
│   │   ├── User.kt
│   │   ├── UserRole.kt
│   │   └── HealthData.kt
│   ├── repository/     # 仓库接口
│   │   ├── AuthRepository.kt
│   │   └── HealthRepository.kt
│   └── usecase/        # 用例（业务逻辑）
│       ├── LoginUseCase.kt
│       └── GetHealthDataUseCase.kt
│
├── data/                # 数据层（Data Layer）
│   ├── local/          # 本地数据源
│   │   └── LocalDataSource.kt
│   └── repository/     # 仓库实现
│       ├── AuthRepositoryImpl.kt
│       └── HealthRepositoryImpl.kt
│
├── presentation/        # 表现层（Presentation Layer）
│   ├── navigation/     # 导航
│   │   ├── Screen.kt
│   │   └── NavGraph.kt
│   ├── welcome/
│   │   └── WelcomeScreen.kt
│   ├── login/
│   │   ├── LoginScreen.kt
│   │   ├── LoginViewModel.kt
│   │   └── LoginUiState.kt
│   └── home/
│       ├── HomeScreen.kt
│       ├── HomeViewModel.kt
│       └── HomeUiState.kt
│
├── di/                  # 依赖注入（Dependency Injection）
│   └── AppModule.kt
│
├── ui/theme/           # UI 主题
│   ├── Color.kt
│   ├── Theme.kt
│   └── Type.kt
│
├── MainActivity.kt      # 单一 Activity
└── PulseLinkApplication.kt
```

## 🏗️ 架构说明

### 1. Domain Layer（领域层）
**职责**：包含业务逻辑和业务规则
- **实体（Entities）**：`User`, `HealthData`, `UserRole`
- **用例（Use Cases）**：`LoginUseCase`, `GetHealthDataUseCase`
- **仓库接口（Repository Interfaces）**：定义数据操作的抽象

**特点**：
- ✅ 不依赖任何其他层
- ✅ 纯 Kotlin 代码，无 Android 依赖
- ✅ 可以独立测试

### 2. Data Layer（数据层）
**职责**：处理数据的存储和检索
- **数据源（Data Sources）**：`LocalDataSource`（DataStore）
- **仓库实现（Repository Implementations）**：实现 Domain 层定义的接口

**特点**：
- ✅ 依赖 Domain 层的接口
- ✅ 处理数据持久化（DataStore, Room, Network）
- ✅ 数据转换和映射

### 3. Presentation Layer（表现层）
**职责**：UI 和用户交互
- **Navigation**：使用 Jetpack Navigation Compose 管理页面跳转
- **Screens**：纯 Composable 函数，无 Activity 依赖
- **ViewModel**：管理 UI 状态，调用 Use Cases
- **UI State**：定义 UI 的状态

**特点**：
- ✅ Single Activity Architecture
- ✅ Navigation Compose 管理路由
- ✅ 依赖 Domain 层的 Use Cases
- ✅ 通过 StateFlow 管理状态
- ✅ 响应式 UI 更新
- ✅ 更好的动画和转场效果

### 4. Dependency Injection（依赖注入）
**技术栈**：Dagger Hilt
- **AppModule**：提供所有依赖
- **@HiltAndroidApp**：应用入口
- **@AndroidEntryPoint**：注入点
- **@HiltViewModel**：ViewModel 注入

## 🔄 数据流

```
UI (Composable Screen)
    ↓ User Action
ViewModel
    ↓ calls
UseCase (Business Logic)
    ↓ calls
Repository Interface
    ↓ implements
Repository Implementation
    ↓ calls
Data Source (DataStore/API)
```

## 🧭 导航流程

```
MainActivity (Single Activity)
    └── NavHost
        ├── WelcomeScreen (起始页)
        ├── SeniorLoginScreen
        ├── CaregiverLoginScreen
        └── HomeScreen
```

**导航优势**：
- ✅ 统一的导航管理
- ✅ 更流畅的页面转场动画
- ✅ 更好的 ViewModel 生命周期管理
- ✅ 类型安全的参数传递
- ✅ 更少的内存开销（无需多个 Activity）

## 📦 技术栈

- **UI**: Jetpack Compose + Material Design 3
- **架构**: Clean Architecture + MVVM + Single Activity
- **导航**: Navigation Compose
- **依赖注入**: Dagger Hilt
- **异步处理**: Kotlin Coroutines + Flow
- **本地存储**: DataStore Preferences
- **状态管理**: StateFlow + Compose State

## 🧪 测试账号

**用户名**: `alvin`  
**密码**: `123456`

适用于 Senior 和 Caregiver 两种角色。

## 🚀 运行项目

1. 打开 Android Studio
2. 同步 Gradle: File → Sync Project with Gradle Files
3. 运行应用：Run → Run 'app'

## 📝 代码规范

- 遵循 Clean Architecture 原则
- 每层只依赖内层，不依赖外层
- 使用 Single Activity Architecture
- 所有页面都是 Composable 函数
- 使用 Navigation Compose 管理导航
- ViewModel 不直接访问 Repository，通过 UseCase
- UI State 使用不可变数据类（data class）

## 🎯 为什么使用 Single Activity?

1. **性能更好**：Activity 切换开销大，Composable 切换更快
2. **动画更流畅**：Navigation Compose 提供更好的转场动画
3. **内存占用更少**：只需维护一个 Activity
4. **生命周期更简单**：ViewModel 作用域更清晰
5. **代码更简洁**：无需 Intent 传递数据
6. **行业标准**：Google 推荐的 Jetpack Compose 最佳实践

