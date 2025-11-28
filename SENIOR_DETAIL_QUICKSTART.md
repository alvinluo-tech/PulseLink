# Senior Detail Feature - 快速开始指南

## 🚀 快速集成

### 1. 添加到导航图

在你的 `NavGraph.kt` 或导航配置文件中添加：

```kotlin
import com.alvin.pulselink.presentation.caregiver.seniordetail.SeniorDetailScreen

// 在 NavHost 中添加路由
composable(
    route = "seniorDetail/{seniorId}/{seniorName}",
    arguments = listOf(
        navArgument("seniorId") { type = NavType.StringType },
        navArgument("seniorName") { type = NavType.StringType }
    )
) { backStackEntry ->
    SeniorDetailScreen(
        seniorId = backStackEntry.arguments?.getString("seniorId") ?: "",
        seniorName = backStackEntry.arguments?.getString("seniorName") ?: "",
        onNavigateBack = { navController.popBackStack() }
    )
}
```

### 2. 从其他页面跳转

```kotlin
// 从 Seniors List 跳转
navController.navigate("seniorDetail/${senior.id}/${senior.name}")

// 或使用类型安全的导航
data class SeniorDetailRoute(
    val seniorId: String,
    val seniorName: String
)

fun NavController.navigateToSeniorDetail(seniorId: String, seniorName: String) {
    navigate("seniorDetail/$seniorId/$seniorName")
}

// 使用
navController.navigateToSeniorDetail(senior.id, senior.name)
```

## 📊 当前功能状态

### ✅ 已完成
- [x] 完整的 UI 框架（3个Tab + 多个组件）
- [x] Reports Tab - 每日报告和周期摘要视图
- [x] Alerts Tab - 健康历史记录
- [x] Reminders Tab - 用药提醒管理
- [x] Mock 数据展示
- [x] 响应式布局
- [x] Material 3 设计
- [x] AI 分析 UI 区域

### ⏳ 待实现
- [ ] Firestore 数据集成
- [ ] 真实数据加载
- [ ] AI 分析 API 调用
- [ ] 通知系统
- [ ] 数据缓存
- [ ] 图表可视化

## 🎨 界面预览

### Reports Tab
```
┌─────────────────────────────────────┐
│ Mother (Mrs. Zhang)                 │
│ ◀ Health Overview                   │
├─────────────────────────────────────┤
│ [Reports] [Alerts] [Reminders]      │
├─────────────────────────────────────┤
│ [Daily Report] [Period Summary]     │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │   📅 Date Picker                │ │
│ │   Today: Nov 22, 2025           │ │
│ │   [Jump to Today]               │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ ❤️ Blood Pressure     [Normal]  │ │
│ │ Reading: 127/78 mmHg            │ │
│ │ Note: Slightly elevated...      │ │
│ │ [Show AI Analysis] ▼            │ │
│ │ 🧠 AI: Blood pressure is...     │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ 💓 Heart Rate          [Normal]  │ │
│ │ Average: 68 bpm                 │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ... (更多健康指标)                    │
└─────────────────────────────────────┘
```

### Alerts Tab
```
┌─────────────────────────────────────┐
│ [All] [Blood Pressure] [Heart Rate] │
│ [Medication] [Activity]             │
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │ ❤️ Blood Pressure      [Normal] │ │
│ │ 127/78 mmHg                     │ │
│ │ Note: Slightly elevated...      │ │
│ │ 🕐 Nov 22, 2025 • 08:30 AM     │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ 💊 Medication          [Normal] │ │
│ │ Blood Pressure Pill             │ │
│ │ Taken at 08:00 as scheduled     │ │
│ │ 🕐 Nov 22, 2025 • 08:00 AM     │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

### Reminders Tab
```
┌─────────────────────────────────────┐
│ Medication Reminders      [+ Add]   │
│ Manage reminders for the senior     │
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │ 💊 Blood Pressure Pill          │ │
│ │ 🕐 08:00  [Daily]               │ │
│ │                      [🔔] [🗑️]  │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ 💊 Vitamin D                    │ │
│ │ 🕐 12:00  [Daily]               │ │
│ │                      [🔔] [🗑️]  │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ℹ️ Reminders will be sent to...    │
└─────────────────────────────────────┘
```

## 💡 使用示例

### 从 ManageSeniorsScreen 跳转

```kotlin
// 在 ManageSeniorsScreen.kt 中
LazyColumn {
    items(seniors) { senior ->
        SeniorCard(
            senior = senior,
            onClick = {
                // 跳转到详情页
                navController.navigate("seniorDetail/${senior.id}/${senior.name}")
            }
        )
    }
}
```

### 自定义顶部栏

如果你想自定义顶部栏，可以修改 `SeniorDetailScreen.kt`:

```kotlin
TopAppBar(
    title = {
        Column {
            Text(
                text = seniorName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            // 添加自定义副标题
            Text(
                text = "Last updated: 2 hours ago",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    },
    // ... 其他配置
)
```

## 🔧 自定义配置

### 修改 Mock 数据

在对应的 ViewModel 中修改：

```kotlin
// ReportsViewModel.kt
val mockReport = DailyHealthReport(
    date = date,
    bloodPressure = HealthMetric(
        value = "120/80 mmHg",  // 修改这里
        status = MetricStatus.NORMAL,
        note = "Perfect reading!",
        aiAnalysis = "Your custom AI analysis"
    ),
    // ... 其他指标
)
```

### 修改颜色主题

在各个 Tab 文件中修改颜色：

```kotlin
// 主色调
Color(0xFF8B5CF6) // 紫色

// 健康指标颜色
Color(0xFFEF4444) // 红色 - 血压
Color(0xFF3B82F6) // 蓝色 - 心率
Color(0xFF10B981) // 绿色 - 用药
Color(0xFF8B5CF6) // 紫色 - 活动
```

### 添加新的健康指标

1. 在 `ReportsViewModel.kt` 添加新指标：
```kotlin
data class DailyHealthReport(
    val date: Date,
    val bloodPressure: HealthMetric,
    val heartRate: HealthMetric,
    val medication: HealthMetric,
    val activity: HealthMetric,
    val bloodSugar: HealthMetric  // 新增
)
```

2. 在 `DailyReportView.kt` 添加显示：
```kotlin
item {
    HealthMetricCard(
        icon = Icons.Default.Bloodtype,
        iconColor = Color(0xFFEC4899),
        title = "Blood Sugar",
        metric = uiState.dailyReport.bloodSugar
    )
}
```

## 📱 测试

### 运行测试数据

当前使用 Mock 数据，可以直接运行查看效果：

1. 编译项目：`./gradlew assembleDebug`
2. 安装到设备：`./gradlew installDebug`
3. 从 Seniors List 点击进入

### 验证功能

- [ ] Tab 切换正常
- [ ] 日期选择器工作正常
- [ ] AI 分析可以展开/折叠
- [ ] 提醒可以添加/删除/切换
- [ ] 筛选功能正常
- [ ] 返回按钮正常

## 🐛 常见问题

### Q: 为什么看不到数据？
A: 当前使用 Mock 数据，需要集成 Firestore 才能显示真实数据。

### Q: AI 分析是真的吗？
A: 目前是静态文本，需要集成 OpenAI/Gemini API。

### Q: 提醒会真的发送吗？
A: 需要实现通知系统和 Firebase Cloud Messaging。

### Q: 如何修改 Tab 顺序？
A: 在 `SeniorDetailScreen.kt` 中调整 Tab 的顺序。

### Q: 如何添加更多筛选选项？
A: 在 `AlertFilterType` 枚举中添加新类型，并更新 `filterAlerts` 方法。

## 🎯 下一步

1. **数据层集成**
   - 创建 Firestore collections
   - 实现 Repository 层
   - 添加数据同步

2. **AI 功能**
   - 集成 AI API
   - 实现健康分析
   - 添加趋势预测

3. **通知系统**
   - 实现提醒通知
   - 添加推送功能
   - 完善用药追踪

4. **图表可视化**
   - 添加趋势图表
   - 实现数据对比
   - 增强数据展示

## 📞 需要帮助？

参考以下文档：
- [完整架构文档](SENIOR_DETAIL_FEATURE.md)
- [UI Feedback System](UI_FEEDBACK_SYSTEM_GUIDE.md)
- [BaseViewModel Pattern](app/src/main/java/com/alvin/pulselink/presentation/common/base/BaseViewModel.kt)
