# 护理者端界面说明

## 已创建的文件

### 界面文件（Screens）
1. **CareDashboardScreen.kt** - 护理仪表板主界面
   - 显示所有被照顾亲人的健康状态
   - 包含三种状态统计：良好、注意、紧急
   - 可点击亲人卡片查看详情

2. **CareChatScreen.kt** - 护理聊天选择界面
   - 选择要咨询的亲人
   - 每个亲人有独立的聊天入口

3. **CaregiverProfileScreen.kt** - 护理者个人资料界面
   - 显示护理者信息和管理的成员数量
   - 护理概览统计
   - 设置、管理家庭成员、隐私安全等功能入口
   - 退出登录功能

### ViewModel 文件
1. **CareDashboardViewModel.kt** - 护理仪表板视图模型
2. **CaregiverProfileViewModel.kt** - 护理者个人资料视图模型

### 导航文件
1. **CaregiverNavigation.kt** - 护理者端导航图扩展

### 资源文件
1. **values/strings.xml** - 英文字符串资源（已更新）
2. **values-zh/strings.xml** - 中文字符串资源（新创建）

## 界面特性

### 1. 护理仪表板（Care Dashboard）
- **顶部标题**："Care Dashboard" / "护理仪表板"
- **副标题**：显示管理的亲人数量
- **状态卡片**：
  - 良好（绿色）
  - 注意（黄色）
  - 紧急（红色）
- **亲人列表**：
  - 每个亲人有头像表情符号
  - 显示姓名、关系和健康状态
  - 不同状态有不同颜色边框
  - 点击可查看详情
- **底部导航栏**：Home、Chat、Profile

### 2. 护理聊天（Care Chat）
- **返回按钮**：左上角紫色返回按钮
- **标题说明**：选择要咨询的对象
- **亲人列表**：
  - 大头像显示
  - 显示姓名和关系
  - 右侧聊天图标
  - 不同状态的颜色边框
- **底部导航栏**：选中 Chat 标签

### 3. 护理者个人资料（Caregiver Profile）
- **紫色渐变头部**：
  - 圆形头像图标
  - 护理者姓名
  - 账户类型和管理成员数
- **护理概览卡片**：
  - 2x2 网格布局
  - 显示：良好状态、需要注意、紧急、活动警报
- **菜单选项**：
  - 设置
  - 管理家庭成员
  - 隐私与安全
  - 帮助中心
- **退出登录按钮**：红色文字
- **底部导航栏**：选中 Profile 标签

## 使用方法

### 1. 在主导航图中集成

在您的 `NavGraph.kt` 文件中添加：

```kotlin
import com.alvin.pulselink.presentation.caregiver.caregiverNavGraph

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ... 现有的导航路由 ...
        
        // 添加护理者端导航
        caregiverNavGraph(navController)
    }
}
```

### 2. 从登录成功后导航到护理仪表板

在 `CaregiverLoginScreen` 成功登录后：

```kotlin
navController.navigate(Screen.CareDashboard.route) {
    popUpTo(Screen.Welcome.route) { inclusive = true }
}
```

### 3. 数据集成

当前使用的是模拟数据。要集成真实数据，需要：

1. 创建数据模型和 Repository
2. 在 ViewModel 中注入 Repository
3. 从 Firestore 获取亲人列表和健康状态
4. 更新 UI 状态

示例：
```kotlin
@HiltViewModel
class CareDashboardViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val healthRepository: HealthRepository
) : ViewModel() {
    
    init {
        loadLovedOnes()
    }
    
    private fun loadLovedOnes() {
        viewModelScope.launch {
            familyRepository.getLovedOnes().collect { lovedOnes ->
                // 更新 UI 状态
            }
        }
    }
}
```

## 颜色方案

- **紫色主题**：`Color(0xFF9333EA)` - 护理者主色
- **良好状态**：`Color(0xFF10B981)` - 绿色
- **注意状态**：`Color(0xFFF59E0B)` - 黄色
- **紧急状态**：`Color(0xFFEF4444)` - 红色
- **背景色**：`Color(0xFFF5F7FA)` - 浅灰色
- **文本主色**：`Color(0xFF2C3E50)` - 深蓝灰
- **文本副色**：`Color(0xFF6B7280)` - 灰色

## 待完成功能

1. **亲人详情页**：点击亲人卡片后显示详细健康报告
2. **聊天详情页**：点击聊天图标后的 AI 咨询界面
3. **设置页面**：账户设置、通知设置等
4. **管理家庭成员页面**：添加/删除/编辑家庭成员
5. **隐私与安全页面**：密码修改、数据隐私设置
6. **帮助中心页面**：常见问题、联系支持

## 国际化支持

已添加中英文双语支持：
- 英文：`values/strings.xml`
- 中文：`values-zh/strings.xml`

系统会根据设备语言自动选择对应的语言资源。

## 测试

可以通过修改 `CareDashboardViewModel` 中的模拟数据来测试不同场景：
- 不同数量的亲人
- 不同的健康状态组合
- 空列表状态

## 下一步开发建议

1. 实现数据层（Repository、Data Source）
2. 集成 Firebase Firestore 获取真实数据
3. 添加下拉刷新功能
4. 添加加载状态和错误处理
5. 实现推送通知功能
6. 完善待办功能页面
