# UI 反馈系统 - 快速参考

## 1分钟快速上手

### Step 1: ViewModel 继承 BaseViewModel

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor() : BaseViewModel() {
    
    fun saveData() {
        viewModelScope.launch {
            try {
                repository.save()
                showSuccess("保存成功")  // ✅ 绿色成功提示
            } catch (e: Exception) {
                showError("保存失败", actionLabel = "重试")  // ❌ 红色错误提示
            }
        }
    }
}
```

### Step 2: UI 使用 PulseLinkScaffold

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    PulseLinkScaffold(
        uiEventFlow = viewModel.uiEvent,  // 就这一行！
        topBar = { /* TopAppBar */ }
    ) { paddingValues ->
        // 你的内容
    }
}
```

### Step 3: 完成！🎉

## API 速查表

### Caregiver 端（子女）

| 方法 | 用途 | 外观 |
|------|------|------|
| `showSuccess("消息")` | 操作成功 | 🟢 绿色胶囊 |
| `showError("消息", "重试")` | 操作失败 | 🔴 红色胶囊 + 按钮 |
| `showWarning("消息")` | 警告提示 | 🟠 橙色胶囊 |
| `showInfo("消息")` | 一般信息 | 🔵 蓝色胶囊 |
| `showLoading("处理中")` | 显示加载 | ⏳ 全屏遮罩 + 进度条 |
| `hideLoading()` | 隐藏加载 | - |

### Senior 端（老人）- 特殊关键操作

| 方法 | 用途 | 外观 |
|------|------|------|
| `showHeroSuccess("消息")` | 关键成功（吃药、语音） | 🌟 全屏大卡片（绿） |
| `showHeroError("消息")` | 关键失败 | 🌟 全屏大卡片（红） |

## 使用场景示例

### ✅ 数据保存
```kotlin
showSuccess("数据保存成功")
```

### ❌ 网络错误
```kotlin
showError("网络连接失败", actionLabel = "重试")
```

### ⚠️ 权限警告
```kotlin
showWarning("您没有权限删除此项")
```

### ℹ️ 自动同步
```kotlin
showInfo("数据已自动同步")
```

### ⏳ 长时间操作
```kotlin
showLoading("正在上传...")
delay(2000)
hideLoading()
showSuccess("上传完成")
```

### 🌟 Senior 关键操作
```kotlin
// 吃药打卡
showHeroSuccess("吃药打卡成功！\n按时服药身体好")

// 语音输入
showHeroSuccess("已收到您的消息")
```

## 注意事项

1. ✅ **推荐**：继承 `BaseViewModel` 使用内置方法
2. ✅ Senior 端关键操作用 `showHeroSuccess`
3. ❌ **避免**：连续触发多个 Snackbar
4. ❌ **避免**：滥用英雄式反馈（每个流程最多1-2次）

## 完整示例

查看 `ExampleUsageScreen.kt` 了解所有反馈类型的实际效果。
