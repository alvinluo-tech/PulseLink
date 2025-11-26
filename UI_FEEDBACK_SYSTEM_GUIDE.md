# PulseLink 状态反馈系统使用指南

## 概述

本系统废弃了系统原生的 `Toast` 和默认的黑底白字 `Snackbar`，实现了一套**分级、美观且适老化**的状态反馈机制。

## 核心组件

### 1. 反馈类型 (`SnackbarType`)

```kotlin
enum class SnackbarType {
    SUCCESS,  // 绿色 - 操作成功
    ERROR,    // 红色 - 操作失败
    WARNING,  // 橙色 - 警告提示
    INFO      // 蓝色 - 一般信息
}
```

### 2. UI 事件 (`UiEvent`)

```kotlin
sealed class UiEvent {
    // 胶囊式 Snackbar（适用于 Caregiver 和 Senior 轻量提示）
    data class ShowSnackbar(
        val message: String,
        val type: SnackbarType = SnackbarType.INFO
    )
    
    // 全屏英雄式反馈（仅用于 Senior 端关键操作）
    data class ShowHeroOverlay(
        val message: String,
        val type: SnackbarType = SnackbarType.SUCCESS
    )
    
    // 加载中覆盖层
    data class ShowLoading(val message: String = "处理中...")
    object HideLoading
}
```

## 使用方法

### 方式一：继承 `BaseViewModel`（推荐）

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    // ... dependencies
) : BaseViewModel() {  // 继承 BaseViewModel

    fun saveData() {
        viewModelScope.launch {
            try {
                // 显示加载
                showLoading("正在保存...")
                
                // 执行操作
                repository.save(data)
                
                // 隐藏加载
                hideLoading()
                
                // 显示成功
                showSuccess("数据保存成功！")
                
            } catch (e: Exception) {
                hideLoading()
                showError("保存失败：${e.message}", actionLabel = "重试")
            }
        }
    }
    
    // Senior 端专用：关键操作使用英雄式反馈
    fun medicationCheckIn() {
        viewModelScope.launch {
            try {
                repository.recordMedication()
                showHeroSuccess("吃药打卡成功！\n按时服药身体好")
            } catch (e: Exception) {
                showHeroError("打卡失败\n请稍后重试")
            }
        }
    }
}
```

### 方式二：手动创建 Channel（灵活方式）

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor() : ViewModel() {
    
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()
    
    fun doSomething() {
        viewModelScope.launch {
            _uiEvent.send(
                UiEvent.ShowSnackbar(
                    message = "操作成功",
                    type = SnackbarType.SUCCESS
                )
            )
        }
    }
}
```

### UI 层集成：使用 `PulseLinkScaffold`

```kotlin
@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel()
) {
    PulseLinkScaffold(
        uiEventFlow = viewModel.uiEvent,  // 传入 ViewModel 的事件流
        topBar = {
            TopAppBar(title = { Text("我的页面") })
        }
    ) { paddingValues ->
        // 你的页面内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Button(onClick = { viewModel.saveData() }) {
                Text("保存数据")
            }
        }
    }
}
```

## 设计规范

### Caregiver 端反馈规范

| 操作类型 | 反馈方式 | 示例 |
|---------|---------|------|
| 数据保存成功 | 绿色 Snackbar | "保存成功" |
| 删除操作成功 | 绿色 Snackbar | "已删除老人账户" |
| 网络请求失败 | 红色 Snackbar + "重试"按钮 | "网络错误，请稍后重试" |
| 权限不足 | 橙色 Warning Snackbar | "您没有权限执行此操作" |
| 一般提示 | 蓝色 Info Snackbar | "数据已同步" |

### Senior 端反馈规范

| 操作类型 | 反馈方式 | 示例 |
|---------|---------|------|
| 吃药打卡 | **英雄式绿色覆盖层** | "吃药打卡成功！" |
| 语音录入完成 | **英雄式绿色覆盖层** | "已收到您的消息" |
| 轻量提示 | 大字号 Snackbar | "记得多喝水哦" |
| 长时间操作 | 加载覆盖层 | "正在识别语音..." |

### 关键原则

1. **Caregiver 端**：
   - 优先使用胶囊式 Snackbar
   - 文字简洁专业
   - 允许快速关闭

2. **Senior 端**：
   - 关键操作必须使用英雄式覆盖层
   - 文字大、图标大、色彩鲜明
   - 给予强烈的安全感和成就感
   - 自动消失，无需手动关闭

3. **字体规范**：
   - Snackbar 文字：16sp（粗体）
   - 英雄式标题：28sp（特粗）
   - 加载文字：24sp（中等）

4. **图标规范**：
   - Snackbar 图标：32dp
   - 英雄式图标：120dp
   - 加载指示器：80dp

## 迁移指南

### 替换旧的 Toast

**旧代码：**
```kotlin
Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
```

**新代码：**
```kotlin
// 在 ViewModel 中
showSuccess("保存成功")
```

### 替换旧的 Snackbar

**旧代码：**
```kotlin
val snackbar = Snackbar.make(view, "操作失败", Snackbar.LENGTH_LONG)
snackbar.setAction("重试") { retry() }
snackbar.show()
```

**新代码：**
```kotlin
// 在 ViewModel 中
showError("操作失败", actionLabel = "重试")
```

### 添加英雄式反馈（Senior 端新功能）

```kotlin
// Senior 端关键操作
fun onMedicationTaken() {
    viewModelScope.launch {
        repository.recordMedication()
        showHeroSuccess("吃药打卡成功！\n按时服药身体好")
    }
}
```

## 完整示例

查看以下文件的完整实现：
- `ExampleUsageScreen.kt` - 展示所有反馈类型的示例页面
- Preview 功能可直接在 Android Studio 中预览各种状态

## 性能优化建议

1. 使用 `BaseViewModel` 避免重复代码
2. 英雄式覆盖层只用于关键操作（每个流程最多1-2次）
3. 避免连续触发多个 Snackbar（会排队显示）
4. 长时间操作使用 Loading 覆盖层防止重复点击
