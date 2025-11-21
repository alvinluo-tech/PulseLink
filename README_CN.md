# PulseLink 🏥

[English](README.md) | 简体中文

一个面向老年人护理的综合健康监测和药物管理应用，采用 Jetpack Compose 和现代 Android 开发实践构建。

## 📱 概述

PulseLink 旨在帮助老年人和护理人员管理健康数据、药物提醒和监测生命体征。该应用提供直观的界面，用于跟踪血压、心率和维护用药计划。

## ✨ 功能特性

### 👤 用户管理
- **双登录系统**：老年人和护理人员分别登录
- **基于角色的访问**：根据用户角色定制体验
- **个人资料管理**：个性化用户资料和健康摘要

### 🩺 健康监测
- **血压追踪**：记录和监测收缩压/舒张压读数
- **心率监测**：追踪心率测量数据
- **健康历史**：查看带有彩色状态指示器的历史健康记录
- **数据可视化**：易于阅读的健康摘要卡片

### 💊 药物管理
- **智能提醒**：及时的用药通知
- **提醒历史**：查看今日用药计划和状态追踪
- **服用/错过追踪**：记录用药依从性
- **药物详情**：剂量、时间和药物名称追踪

### 👥 老人账户管理
- **创建老人账户**：创建老人后自动绑定创建者；显示成功提示消息，并返回子女端主页以查看已绑定老人。
- **管理老人（我创建的列表）**：仅显示由当前用户创建的老人（`creatorId`）。
- **绑定老人账户**：通过老人虚拟 ID 进行绑定；防重复绑定，并通过 `caregiverIds` 支持多个护理人员。
- **一键复制老人 ID**：在已创建老人列表中，ID 旁提供复制按钮。

### 🤖 AI 语音助手
- **对话界面**：基于聊天的健康咨询交互
- **语音输入支持**：说话提问
- **健康指导**：获取关于健康和用药的答案

### 📊 仪表盘
- **健康数据概览**：快速访问关键健康指标
- **快速操作**：从主屏幕导航到关键功能
- **每日摘要**：一目了然地查看提醒数量和健康状态

## 🏗️ 架构

PulseLink 遵循 **Clean Architecture** 原则和 **MVVM** 模式：

```
app/
├── data/                      # 数据层
├── domain/                    # 领域层（业务逻辑）
├── presentation/              # 表现层（UI）
│   ├── assistant/            # AI 语音助手功能
│   ├── health/               # 健康数据输入
│   ├── history/              # 健康历史查看
│   ├── home/                 # 主仪表盘
│   ├── login/                # 身份验证
│   ├── navigation/           # 导航配置
│   ├── profile/              # 用户资料
│   ├── reminder/             # 单个药物提醒
│   ├── reminderlist/         # 药物提醒列表
│   └── welcome/              # 欢迎/引导页
├── di/                       # 依赖注入
└── ui/                       # UI 主题和组件
```

## 🧩 数据模型

### Senior（老人模型）
- `id: String` – 唯一虚拟 ID（例如 `SNR-XXXXXXXX`）
- `name: String`、`age: Int`、`gender: String`
- `healthHistory: HealthHistory`
- `caregiverIds: List<String>` – 支持多个护理人员绑定同一老人
- `creatorId: String` – 创建该老人账户的护理人员 ID
- `createdAt: Long`

### HealthHistory（健康历史）
- `bloodPressure: BloodPressureRecord?`、`heartRate: Int?`、`bloodSugar: Double?`
- `medicalConditions: List<String>`、`medications: List<String>`、`allergies: List<String>`

### BloodPressureRecord（血压记录）
- `systolic: Int`、`diastolic: Int`、`recordedAt: Long`

## 🛠️ 技术栈

### 核心技术
- **Kotlin** - 主要编程语言
- **Jetpack Compose** - 现代声明式 UI 工具包
- **Material Design 3** - UI 设计系统

### 架构与库
- **Hilt** - 依赖注入
- **Navigation Component** - 应用内导航
- **ViewModel** - UI 状态管理
- **StateFlow** - 响应式状态管理
- **Kotlin Coroutines** - 异步编程

### 构建与工具
- **Gradle Kotlin DSL** - 构建配置
- **Version Catalogs** - 依赖管理
- **Android Studio** - 集成开发环境

## 📋 前置要求

- Android Studio Hedgehog 或更高版本
- JDK 17 或更高版本
- Android SDK 24 (Nougat) 或更高版本
- Gradle 8.13.1

## 🚀 快速开始

### 1. 克隆仓库
```bash
git clone https://github.com/alvinluo-tech/PulseLink.git
cd PulseLink
```

### 2. 在 Android Studio 中打开
- 启动 Android Studio
- 选择 "打开现有项目"
- 导航到克隆的仓库

### 3. 同步依赖
```bash
./gradlew build
```

### 4. 运行应用
- 连接 Android 设备或启动模拟器
- 在 Android Studio 中点击 "运行" 或使用：
```bash
./gradlew installDebug
```

## 📱 应用界面

### 欢迎与认证
- **欢迎屏幕**：选择老年人或护理人员登录
- **登录屏幕**：使用手机号码进行身份验证

### 主要功能
- **主仪表盘**：健康指标概览和快速操作
- **健康数据录入**：输入血压和心率
- **健康历史**：查看带有状态指示器的历史健康记录
- **个人资料**：用户信息和应用设置
- **提醒**：查看和管理药物提醒
- **语音助手**：AI 驱动的健康助手

## 🎨 设计亮点

- **适老化 UI**：大字体、高对比度、简单导航
- **彩色状态编码**：
  - 🟢 绿色：正常/已服用
  - 🔵 蓝色：待处理
  - 🔴 红色：偏高/已错过
  - 🟡 黄色：偏低
- **底部导航**：轻松访问关键功能
- **浮动操作按钮**：快速访问语音助手
- **卡片式设计**：清晰的信息分隔

## 🔐 安全与隐私

- 需要用户身份验证
- 基于角色的访问控制
- 安全的数据处理
- 隐私优先设计

## 📦 项目结构

```
PulseLink/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/alvin/pulselink/
│   │   │   │   ├── data/
│   │   │   │   ├── domain/
│   │   │   │   ├── presentation/
│   │   │   │   ├── di/
│   │   │   │   └── ui/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/
│   │   └── test/
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── ARCHITECTURE.md
├── MIGRATION_GUIDE.md
├── README.md
└── README_CN.md
```

## 🧪 测试

运行单元测试：
```bash
./gradlew test
```

运行仪器测试：
```bash
./gradlew connectedAndroidTest
```

## 🤝 贡献

欢迎贡献！请遵循以下步骤：

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 详见 LICENSE 文件。

## 👥 作者

- **Alvin Luo** - *初始工作* - [alvinluo-tech](https://github.com/alvinluo-tech)

## 🙏 致谢

- Material Design 3 设计指南
- Jetpack Compose 社区
- Android 开发最佳实践

## 📞 支持

如需支持，请在 GitHub 仓库中开启 issue 或联系开发团队。

## 🗺️ 路线图

- [ ] 集成可穿戴设备
- [ ] 实时健康监测
- [ ] 护理人员通知系统
- [ ] 多语言支持
- [ ] 云数据同步
- [ ] 紧急联系人功能
- [ ] 健康报告生成
- [ ] 与医疗服务提供商集成

## 📱 应用截图

*（在此添加应用截图）*

## 🔧 配置

### Firebase 设置（必需）
本项目使用 Firebase 服务。设置步骤：

1. **创建 Firebase 项目**：
   - 访问 [Firebase 控制台](https://console.firebase.google.com/)
   - 创建新项目或使用现有项目

2. **添加 Android 应用**：
   - 使用包名注册应用：`com.alvin.pulselink`
   - 下载 `google-services.json`

3. **添加配置文件**：
   ```bash
   # 将下载的文件复制到 app 目录
   cp /path/to/google-services.json app/
   ```
   
   或重命名示例文件：
   ```bash
   cp app/google-services.json.example app/google-services.json
   # 然后使用实际的 Firebase 凭据更新
   ```

4. **启用 Firebase 服务**（在 Firebase 控制台中）：
   - Authentication（如需要）
   - Firestore Database（如需要）
   - Cloud Storage（如需要）

⚠️ **重要**：切勿将 `google-services.json` 提交到版本控制，因为它包含 API 密钥！

### API 密钥
在 `local.properties` 中配置 API 密钥：
```properties
API_KEY=your_api_key_here
```

---

**注意**：这是一个面向老年人健康护理管理的教育/演示项目。如需医疗建议，请咨询医疗专业人员。

---

## 🛠️ 开发者说明

### Hilt 代码生成：KAPT → KSP
- 项目使用 Kotlin `2.0.x`；Hilt 代码生成通过 KSP 配置：
  - 在 Gradle 插件中将 `kotlin-kapt` 替换为 `com.google.devtools.ksp`。
  - 依赖层改为 `ksp("com.google.dagger:hilt-compiler:<version>")`。
- 切换后建议执行：
```bash
./gradlew clean assembleDebug
```

### 仓库与查询
- `SeniorRepository` 新增 `getSeniorsByCreator(creatorId)`。
- `getSeniorsByCaregiver(caregiverId)` 使用 `whereArrayContains("caregiverIds", caregiverId)` 查询。

### 导航行为
- 创建老人成功后，应用导航到 `CaregiverHome` 展示已绑定老人。