# 无感登录实现文档 (Auto-Login Implementation)

## 概述 (Overview)

实现了双端（老人端 & 子女端）的无感登录功能。当用户启动 App 时，系统会自动检查 Firebase Auth 的登录状态，如果已登录则直接进入主页，无需再次输入账号密码。

## 实现原理 (Implementation Logic)

### 1. 启动流程 (Startup Flow)

```
App 启动
    ↓
AuthCheckScreen (身份验证检查页)
    ↓
检查 Firebase Auth currentUser
    ↓
    ├─ 已登录 (Authenticated)
    │       ↓
    │   读取 LocalDataSource 缓存的角色信息
    │       ↓
    │   根据角色导航:
    │   ├─ SENIOR → 老人端主页 (SeniorHome)
    │   └─ CAREGIVER → 子女端主页 (CaregiverHome)
    │
    └─ 未登录 (Not Authenticated)
            ↓
        欢迎页 (Welcome Screen)
```

### 2. 核心组件 (Core Components)

#### AuthCheckScreen.kt
- **位置**: `presentation/auth/AuthCheckScreen.kt`
- **功能**: 
  - 显示加载指示器
  - 检查 Firebase Auth 的 `currentUser`
  - 读取 LocalDataSource 中的角色信息
  - 根据角色导航到对应页面
- **特点**: 
  - 使用 `LaunchedEffect(Unit)` 确保只在首次加载时执行
  - 使用 `withContext(Dispatchers.IO)` 处理异步操作
  - 自动清理返回栈，防止用户返回到检查页

#### Screen.kt 更新
- **新增**: `Screen.AuthCheck` 路由定义
- **用途**: 作为 App 的默认启动页面

#### AppNavigation.kt 更新
- **默认启动页**: 从 `Screen.Welcome` 改为 `Screen.AuthCheck`
- **导航配置**: 
  - AuthCheck 成功后清空返回栈
  - 登出后导航到 Welcome 页面
- **注入 LocalDataSource**: 用于读取本地缓存的用户信息

### 3. 登录信息缓存 (Login Cache)

#### LocalDataSource
- **存储内容**:
  - `user_id`: 用户 UID
  - `username`: 用户名
  - `user_role`: 用户角色 (SENIOR/CAREGIVER)
- **使用场景**:
  - 登录成功时保存 (`AuthRepositoryImpl.login()`)
  - 启动时读取 (`AuthCheckScreen`)
  - 登出时清除 (`AuthRepositoryImpl.logout()`)

## 使用场景 (Use Cases)

### 场景 1: 首次安装 / 登出后
```
App 启动 → AuthCheck → Firebase Auth (无用户) → Welcome Screen → 用户选择角色登录
```

### 场景 2: 已登录用户重启 App
```
App 启动 → AuthCheck → Firebase Auth (有用户) + LocalDataSource (有角色) 
         → 直接进入对应主页 (老人端/子女端)
```

### 场景 3: 用户登出
```
点击登出 → AuthRepository.logout() → 清除 LocalDataSource → Firebase Auth 登出 
        → 导航到 Welcome Screen
```

### 场景 4: 缓存异常 (Firebase 有用户但 LocalDataSource 为空)
```
App 启动 → AuthCheck → Firebase Auth (有用户) + LocalDataSource (空) 
         → 返回 Welcome Screen → 用户重新登录
```

## 技术细节 (Technical Details)

### 1. 避免闪屏 (Avoiding Flash)
- AuthCheckScreen 显示简洁的加载指示器
- 使用 `LaunchedEffect` 立即执行检查，最小化等待时间
- LocalDataSource 使用 DataStore，读取速度极快 (< 10ms)

### 2. 线程管理 (Thread Management)
```kotlin
LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
        // 读取 Firebase Auth 和 LocalDataSource
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userInfo = localDataSource.getUser()
        
        withContext(Dispatchers.Main) {
            // 在主线程执行导航
            when (role) {
                "SENIOR" -> onNavigateToSeniorHome()
                "CAREGIVER" -> onNavigateToCaregiverHome()
            }
        }
    }
}
```

### 3. 返回栈管理 (Back Stack Management)
```kotlin
navController.navigate(Screen.SeniorHome.route) {
    // 清空返回栈，防止用户按返回键回到 AuthCheck
    popUpTo(Screen.AuthCheck.route) { inclusive = true }
}
```

## 测试验证 (Testing)

### 测试步骤:

1. **首次安装测试**
   - 清除 App 数据
   - 启动 App
   - 预期: 显示 Welcome Screen

2. **登录后重启测试**
   - 登录账号 (老人端或子女端)
   - 关闭 App
   - 重新启动 App
   - 预期: 直接进入对应主页，无需再次登录

3. **登出测试**
   - 已登录状态下点击"登出"
   - 预期: 导航到 Welcome Screen
   - 再次启动 App
   - 预期: 显示 Welcome Screen (需要重新登录)

4. **双端切换测试**
   - 使用老人端账号登录
   - 重启 App → 进入老人端主页
   - 登出 → 使用子女端账号登录
   - 重启 App → 进入子女端主页

## 注意事项 (Notes)

1. **安全性**: 
   - LocalDataSource 仅存储角色信息，不存储密码
   - 实际身份验证仍依赖 Firebase Auth
   - 如果 Firebase Session 过期，用户需要重新登录

2. **数据一致性**:
   - 登录时同步保存到 LocalDataSource
   - 登出时同步清除 LocalDataSource
   - 避免缓存与实际登录状态不一致

3. **用户体验**:
   - AuthCheckScreen 显示加载指示器，避免白屏
   - 检查时间极短 (< 100ms)，用户几乎无感知
   - 清空返回栈，避免返回键导致的逻辑混乱

## 相关文件 (Related Files)

```
app/src/main/java/com/alvin/pulselink/
├── presentation/
│   ├── auth/
│   │   └── AuthCheckScreen.kt         # NEW: 启动时的身份验证检查
│   └── nav/
│       ├── Screen.kt                   # MODIFIED: 添加 AuthCheck 路由
│       └── AppNavigation.kt            # MODIFIED: 启动页改为 AuthCheck
├── data/
│   ├── local/
│   │   └── LocalDataSource.kt          # EXISTING: 用户信息缓存
│   └── repository/
│       └── AuthRepositoryImpl.kt       # EXISTING: 登录/登出时操作缓存
└── domain/
    └── repository/
        └── AuthRepository.kt           # EXISTING: getCurrentUser() 接口
```

## 版本历史 (Version History)

- **v1.0** (2025-01-XX): 初始实现，支持双端无感登录
