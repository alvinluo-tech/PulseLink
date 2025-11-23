# 调试查询问题指南

## 🐛 当前问题
1. Link Senior 提示权限问题
2. Manage Seniors 查看不到任何老人  
3. Create Senior Account 查看不到任何老人

## ✅ 已修复的问题

### 1. Firestore 规则权限问题

**问题：**
- `linkRequests` collection 没有任何安全规则
- `seniors` collection 的 create 规则不允许 `pendingCaregiversIds` 字段

**修复：**
```javascript
// 添加了 linkRequests collection 规则
match /linkRequests/{requestId} {
  allow create: if isAuthenticated() && requesterId == currentUser
  allow read: if isRequester() || isCreator()
  allow update: if isCreator()
  allow delete: if isRequester() || isCreator()
}

// 更新 seniors create 规则，允许 pendingCaregiversIds
allow create: if ... && keys().hasOnly([
  'id','name','age','gender','avatarType','healthHistory',
  'caregiverIds','pendingCaregiversIds','caregiverRelationships',  // ✅ 添加了 pendingCaregiversIds
  'creatorId','createdAt','password'
])
```

**部署状态：** ✅ 已部署 (firebase deploy --only firestore:rules)

---

## 🔍 调试步骤

### 步骤1: 检查 Logcat 日志

在 Android Studio 中打开 Logcat，过滤以下标签：

```
ManageSeniorsVM
LinkSeniorVM  
SeniorRepository
LinkRequestRepository
```

**预期日志：**
```
ManageSeniorsVM: Loading seniors for caregiver: [userId]
ManageSeniorsVM: Active seniors result: true
ManageSeniorsVM: Created seniors result: true
ManageSeniorsVM: Pending requests result: true
ManageSeniorsVM: Active count: X
ManageSeniorsVM: Created count: Y
ManageSeniorsVM: Pending requests count: Z
ManageSeniorsVM: Total unique seniors: N
ManageSeniorsVM: Final - Created: X, Linked: Y
```

**如果看到错误日志：**
```
ManageSeniorsVM: Failed to load seniors: [error message]
```

→ 检查错误信息是权限问题还是查询问题

---

### 步骤2: 在 Firebase Console 检查数据

访问：https://console.firebase.google.com/project/pulselink-b0f16/firestore

#### 检查 `seniors` collection
- 是否有文档？
- 每个文档的 `creatorId` 是否匹配当前用户？
- `caregiverIds` 数组是否包含当前用户？

#### 检查 `linkRequests` collection  
- 是否有文档？
- `requesterId` 和 `creatorId` 是否正确？
- `status` 是 "pending" / "approved" / "rejected"？

#### 检查 `users` collection
- 当前用户的 UID 是什么？（在 Authentication 标签查看）

---

### 步骤3: 测试查询方法

#### 方法1: 在应用中添加调试日志

已在 ViewModel 中添加详细日志，运行应用后检查 Logcat。

#### 方法2: 手动创建测试数据

在 Firebase Console 中手动创建一个 senior 文档：

```json
{
  "id": "SNR-TEST0001",
  "name": "测试老人",
  "age": 70,
  "gender": "Male",
  "avatarType": "elderly_male",
  "creatorId": "[你的 userId]",
  "caregiverIds": ["[你的 userId]"],
  "pendingCaregiversIds": [],
  "caregiverRelationships": {
    "[你的 userId]": {
      "relationship": "Son",
      "nickname": "爸爸",
      "linkedAt": 1732320000000,
      "status": "active",
      "message": ""
    }
  },
  "createdAt": 1732320000000,
  "password": "test123",
  "healthHistory": {
    "bloodPressure": null,
    "heartRate": null,
    "bloodSugar": null,
    "medicalConditions": [],
    "medications": [],
    "allergies": []
  }
}
```

然后刷新应用，查看是否显示。

---

### 步骤4: 检查网络请求

在 Logcat 中搜索：
```
Firestore
firebase
```

查看是否有网络错误或权限被拒绝的信息。

---

## 🎯 常见问题解决方案

### 问题1: "Permission denied" 错误

**原因：** Firestore 规则不允许操作

**解决：**
1. 确认已部署最新规则：`firebase deploy --only firestore:rules`
2. 检查规则是否正确（见上方修复）
3. 在 Firebase Console → Firestore → Rules 标签查看当前规则

---

### 问题2: 查询返回空列表但数据存在

**可能原因：**
1. `creatorId` 或 `caregiverIds` 与当前用户 UID 不匹配
2. 查询条件错误
3. 索引未创建（虽然简单查询不需要）

**调试：**
```kotlin
// 添加到 SeniorRepositoryImpl.kt
Log.d("SeniorRepo", "Querying seniors with caregiverId: $caregiverId")
val snapshot = seniorsCollection
    .whereArrayContains("caregiverIds", caregiverId)
    .get()
    .await()
Log.d("SeniorRepo", "Query returned ${snapshot.size()} documents")
snapshot.documents.forEach { doc ->
    Log.d("SeniorRepo", "Document: ${doc.id}, caregiverIds: ${doc.get("caregiverIds")}")
}
```

---

### 问题3: ManageSeniorsViewModel 没有初始化

**检查：**
1. ViewModel 是否正确注入？（使用 `hiltViewModel()`）
2. `init {}` 块是否执行？（添加日志验证）
3. 是否有异常导致初始化失败？

---

### 问题4: UI 没有更新

**检查：**
1. StateFlow 是否正确 collect？
```kotlin
val state by viewModel.manageSeniorsState.collectAsStateWithLifecycle()
```

2. UI 是否响应 state 变化？
```kotlin
when {
    state.isLoading -> ShowLoading()
    state.createdSeniors.isEmpty() -> ShowEmptyState()
    else -> ShowList(state.createdSeniors)
}
```

---

## 📝 验证清单

- [ ] Firestore 规则已部署
- [ ] 应用已重新编译并安装
- [ ] 在 Firebase Console 确认有数据
- [ ] 在 Logcat 看到查询日志
- [ ] 检查用户 UID 与数据中的 ID 匹配
- [ ] 检查网络连接
- [ ] 清除应用缓存并重启

---

## 🚀 下一步

如果以上步骤都检查过了仍有问题，请提供：
1. Logcat 完整错误日志
2. Firebase Console 中的数据截图
3. 当前用户的 UID
4. 具体的错误信息或异常堆栈

这样可以更精准地定位问题。
