# Senior Detail Feature - 老人详情页面系统

## 📁 文件结构

```
presentation/caregiver/seniordetail/
├── SeniorDetailScreen.kt          // 主页面容器（带 Tab 切换）
├── SeniorDetailViewModel.kt       // 主 ViewModel
├── tabs/
│   ├── ReportsTab.kt              // 健康报告 Tab
│   ├── AlertsTab.kt               // 健康历史 Tab
│   └── RemindersTab.kt            // 用药提醒 Tab
├── components/
│   ├── DailyReportView.kt         // 每日报告视图
│   ├── PeriodSummaryView.kt       // 周期摘要视图
│   ├── AlertItem.kt               // 健康历史项
│   └── ReminderItem.kt            // 提醒项
└── viewmodels/
    ├── ReportsViewModel.kt        // 报告相关逻辑
    ├── AlertsViewModel.kt         // 历史记录逻辑
    └── RemindersViewModel.kt      // 提醒管理逻辑
```

## 🎯 功能概述

### 1. Reports Tab - 健康报告
- **Daily Report**: 查看某天的详细健康数据
  - 血压读数 + AI 分析
  - 心率监测 + AI 分析
  - 用药记录 + AI 分析
  - 活动数据 + AI 分析
  
- **Period Summary**: 查看一段时间的健康趋势
  - AI 驱动的整体健康评分
  - 血压趋势分析
  - 心率趋势分析
  - 用药依从性统计
  - 活动摘要
  - 关键观察
  - AI 推荐建议

### 2. Alerts Tab - 健康历史
- 显示所有健康上报历史记录
- 支持按类型筛选（全部/血压/心率/用药/活动）
- 每条记录包含：
  - 健康指标类型和数值
  - 状态标签（Normal/Warning/Critical）
  - 备注信息
  - 时间戳

### 3. Reminders Tab - 用药提醒
- 查看所有设定的提醒
- 添加新提醒（药名、时间、频率）
- 启用/禁用提醒
- 删除提醒

## 🎨 设计特点

### 色彩系统
- **主色调**: Purple (`#8B5CF6`) - 品牌色
- **血压**: Red (`#EF4444`)
- **心率**: Blue (`#3B82F6`)
- **用药**: Green (`#10B981`)
- **活动**: Purple (`#8B5CF6`)

### UI 组件
- **Material 3** Design
- **卡片式布局** - 清晰的信息层级
- **圆角设计** - 友好的视觉体验
- **状态指示器** - 直观的健康状态展示
- **AI 分析区域** - 可展开的智能分析

## 🔌 集成指南

### 1. 导航集成

在你的 Navigation Graph 中添加：

```kotlin
// 在 NavHost 中添加
composable(
    route = "seniorDetail/{seniorId}/{seniorName}",
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

### 2. 从其他页面跳转

```kotlin
// 从 Seniors List 或其他页面跳转
navController.navigate("seniorDetail/${senior.id}/${senior.name}")
```

### 3. Repository 集成 (TODO)

需要创建以下 Repository：

```kotlin
// 1. HealthDataRepository
interface HealthDataRepository {
    suspend fun getDailyReport(seniorId: String, date: Date): DailyHealthReport
    suspend fun getPeriodSummary(seniorId: String, startDate: Date, endDate: Date): PeriodHealthSummary
}

// 2. HealthHistoryRepository
interface HealthHistoryRepository {
    suspend fun getHealthAlerts(seniorId: String): List<HealthAlert>
    fun observeHealthAlerts(seniorId: String): Flow<List<HealthAlert>>
}

// 3. RemindersRepository
interface RemindersRepository {
    suspend fun getReminders(seniorId: String): List<MedicationReminder>
    suspend fun addReminder(seniorId: String, reminder: MedicationReminder)
    suspend fun updateReminder(reminderId: String, isEnabled: Boolean)
    suspend fun deleteReminder(reminderId: String)
}

// 4. AIAnalysisRepository
interface AIAnalysisRepository {
    suspend fun generateHealthSummary(seniorId: String, startDate: Date, endDate: Date): PeriodHealthSummary
}
```

### 4. Firestore 数据结构建议

```
/seniors/{seniorId}/
  /healthReports/
    /{date}/
      - bloodPressure: { value, timestamp, note }
      - heartRate: { value, timestamp, note }
      - medication: { taken, timestamp, medications[] }
      - activity: { steps, activeTime, timestamp }
  
  /reminders/
    /{reminderId}/
      - medicationName: string
      - time: string
      - frequency: string
      - isEnabled: boolean
      - createdAt: timestamp
```

## 🚀 下一步开发任务

### Phase 1: 数据层
1. ✅ 创建 UI 框架和组件
2. ⏳ 创建 Firestore 数据模型
3. ⏳ 实现 Repository 层
4. ⏳ 集成 Firebase Functions (AI 分析)

### Phase 2: 功能完善
5. ⏳ 实现数据加载和缓存
6. ⏳ 添加下拉刷新
7. ⏳ 添加错误处理和重试
8. ⏳ 实现提醒通知系统

### Phase 3: AI 集成
9. ⏳ 集成 OpenAI/Gemini API 进行健康分析
10. ⏳ 实现健康趋势预测
11. ⏳ 添加个性化建议

### Phase 4: 优化
12. ⏳ 添加数据可视化图表
13. ⏳ 实现数据导出功能
14. ⏳ 添加分享功能
15. ⏳ 性能优化和测试

## 🧪 测试

目前使用 Mock 数据进行 UI 测试。需要创建：

1. **Unit Tests**: ViewModel 逻辑测试
2. **Integration Tests**: Repository 集成测试
3. **UI Tests**: Compose UI 测试

## 📝 注意事项

1. **数据安全**: 健康数据需要严格的权限控制
2. **实时更新**: 使用 Firestore Realtime Listeners
3. **离线支持**: 考虑添加本地缓存
4. **隐私合规**: 遵守 HIPAA/GDPR 等健康数据法规
5. **AI 成本**: 控制 AI API 调用频率

## 🎯 用户故事

### Caregiver 用户
- 作为 Caregiver，我可以查看老人的每日健康数据
- 作为 Caregiver，我可以查看一段时间的健康趋势
- 作为 Caregiver，我可以获得 AI 驱动的健康建议
- 作为 Caregiver，我可以查看所有健康上报历史
- 作为 Caregiver，我可以为老人设置用药提醒

### Senior 用户（关联功能）
- 老人端需要相应的健康上报界面
- 老人端需要接收用药提醒
- 老人端需要确认用药完成

## 📚 相关文档

- [UI Feedback System Guide](../../../UI_FEEDBACK_SYSTEM_GUIDE.md)
- [BaseViewModel Pattern](../../../common/base/BaseViewModel.kt)
- [PulseLinkScaffold Usage](../../../common/components/PulseLinkScaffold.kt)
