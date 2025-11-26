# 老人自主权限系统 - 代码更新清单

## ✅ 已完成

### 1. Firestore Rules
- ✅ 老人拥有最高权限（可以更新任何字段）
- ✅ linkRequestApprovers 列表控制谁可以审批链接请求
- ✅ 老人永远可以审批（隐式权限）
- ✅ 创建时设置默认审批人：
  - CAREGIVER_CREATED → [creatorId]
  - SELF_REGISTERED → []（老人自己有隐式权限）
- ✅ 审批操作必须记录 approvedBy/rejectedBy 和时间戳

### 2. 数据模型
- ✅ Senior.kt：添加 `linkRequestApprovers` 字段
- ✅ CaregiverRelationship：添加 `approvedBy` 和 `permissions` 字段
- ✅ CaregiverPermissions：新数据类（细粒度权限控制）
- ✅ LinkRequest.kt：添加审批记录字段（approvedBy, approvedAt, rejectedBy, rejectedAt）

## 🔄 需要更新的代码

### 3. SeniorRepositoryImpl.kt

#### createSenior() 方法
```kotlin
val seniorData = hashMapOf(
    // ... 现有字段 ...
    "linkRequestApprovers" to senior.linkRequestApprovers, // ⭐ 新增
    "registrationType" to senior.registrationType
)
```

#### getSeniorById() 方法  
```kotlin
val senior = Senior(
    // ... 现有字段 ...
    registrationType = doc.getString("registrationType") ?: "CAREGIVER_CREATED",
    linkRequestApprovers = (doc.get("linkRequestApprovers") as? List<*>)
        ?.mapNotNull { it as? String } ?: emptyList() // ⭐ 新增
)
```

#### 读取 caregiverRelationships 时添加新字段
```kotlin
val caregiverRelationships = relationshipsMap?.mapNotNull { (key, value) ->
    val caregiverId = key as? String ?: return@mapNotNull null
    val relMap = value as? Map<*, *> ?: return@mapNotNull null
    
    // 读取 permissions
    val permMap = relMap["permissions"] as? Map<*, *>
    val permissions = if (permMap != null) {
        CaregiverPermissions(
            canViewHealthData = permMap["canViewHealthData"] as? Boolean ?: true,
            canViewReminders = permMap["canViewReminders"] as? Boolean ?: true,
            canEditReminders = permMap["canEditReminders"] as? Boolean ?: true,
            canApproveLinkRequests = permMap["canApproveLinkRequests"] as? Boolean ?: false
        )
    } else {
        CaregiverPermissions() // 默认权限
    }
    
    caregiverId to CaregiverRelationship(
        relationship = relMap["relationship"] as? String ?: "",
        nickname = relMap["nickname"] as? String ?: "",
        linkedAt = relMap["linkedAt"] as? Long ?: System.currentTimeMillis(),
        status = relMap["status"] as? String ?: "active",
        message = relMap["message"] as? String ?: "",
        approvedBy = relMap["approvedBy"] as? String ?: "", // ⭐ 新增
        permissions = permissions // ⭐ 新增
    )
}?.toMap() ?: emptyMap()
```

### 4. AuthRepositoryImpl.kt

#### registerSenior() 方法
在创建 seniors 文档时，不设置 linkRequestApprovers（留空，老人自己有隐式权限）：
```kotlin
val seniorDoc = hashMapOf(
    // ... 现有字段 ...
    "registrationType" to "SELF_REGISTERED",
    "linkRequestApprovers" to emptyList<String>() // ⭐ 自注册时为空
)
```

### 5. CreateSeniorUseCase.kt

#### Caregiver 创建老人时，设置默认审批人
```kotlin
val senior = Senior(
    // ... 现有字段 ...
    registrationType = "CAREGIVER_CREATED",
    linkRequestApprovers = listOf(currentUser.uid), // ⭐ 默认创建者有审批权
    caregiverRelationships = mapOf(
        currentUser.uid to CaregiverRelationship(
            relationship = "Creator",
            linkedAt = System.currentTimeMillis(),
            status = "active",
            approvedBy = currentUser.uid, // ⭐ 标记为创建者自己批准
            permissions = CaregiverPermissions(
                canViewHealthData = true,
                canViewReminders = true,
                canEditReminders = true,
                canApproveLinkRequests = true // ⭐ 创建者默认有审批权
            )
        )
    )
)
```

### 6. SeniorLinkGuardViewModel.kt

#### approveRequest() 方法更新
```kotlin
fun approveRequest(request: LinkRequest) {
    viewModelScope.launch {
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
        
        try {
            val currentUser = authRepository.getCurrentUser()
            val currentUid = currentUser?.uid ?: return@launch
            
            // 1. 获取 senior 信息
            val seniorResult = seniorRepository.getSeniorById(request.seniorId)
            // ...
            
            // 2. 更新 senior 文档
            val updatedSenior = senior.copy(
                caregiverIds = updatedCaregiverIds,
                caregiverRelationships = updatedRelationships.toMap()
            )
            seniorRepository.updateSenior(updatedSenior)
            
            // 3. 更新 linkRequest 状态，记录审批人
            linkRequestRepository.updateRequestStatus(
                requestId = request.id,
                status = "approved",
                approvedBy = currentUid, // ⭐ 记录审批人
                approvedAt = System.currentTimeMillis() // ⭐ 记录时间
            )
            
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    successMessage = "已批准绑定请求"
                )
            }
            loadPendingRequests()
        } catch (e: Exception) {
            // ...
        }
    }
}

fun rejectRequest(request: LinkRequest) {
    viewModelScope.launch {
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
        
        try {
            val currentUser = authRepository.getCurrentUser()
            val currentUid = currentUser?.uid ?: return@launch
            
            linkRequestRepository.updateRequestStatus(
                requestId = request.id,
                status = "rejected",
                rejectedBy = currentUid, // ⭐ 记录拒绝人
                rejectedAt = System.currentTimeMillis() // ⭐ 记录时间
            )
            
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    successMessage = "已拒绝绑定请求"
                )
            }
            loadPendingRequests()
        } catch (e: Exception) {
            // ...
        }
    }
}
```

### 7. LinkRequestRepository.kt

#### 更新接口
```kotlin
interface LinkRequestRepository {
    // 更新方法签名
    suspend fun updateRequestStatus(
        requestId: String,
        status: String,
        approvedBy: String? = null,
        approvedAt: Long? = null,
        rejectedBy: String? = null,
        rejectedAt: Long? = null
    ): Result<Unit>
}
```

### 8. LinkRequestRepositoryImpl.kt

```kotlin
override suspend fun updateRequestStatus(
    requestId: String,
    status: String,
    approvedBy: String?,
    approvedAt: Long?,
    rejectedBy: String?,
    rejectedAt: Long?
): Result<Unit> {
    return try {
        val updates = hashMapOf<String, Any>(
            "status" to status,
            "updatedAt" to System.currentTimeMillis()
        )
        
        // 添加审批记录
        if (status == "approved" && approvedBy != null) {
            updates["approvedBy"] = approvedBy
            updates["approvedAt"] = approvedAt ?: System.currentTimeMillis()
        }
        
        if (status == "rejected" && rejectedBy != null) {
            updates["rejectedBy"] = rejectedBy
            updates["rejectedAt"] = rejectedAt ?: System.currentTimeMillis()
        }
        
        firestore.collection("linkRequests")
            .document(requestId)
            .update(updates)
            .await()
            
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

## 🎨 未来 UI 功能（Phase 2）

### 老人端设置页面 - 审批权限管理
```
┌─────────────────────────────────────┐
│ 链接请求审批设置                      │
│                                      │
│ 当前审批人：                         │
│ ✓ 我自己（总是可以审批）              │
│ ✓ 张三 (女儿)                        │
│ ✗ 李四 (儿子)                        │
│                                      │
│ [管理审批人]                         │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ 护理者权限管理                        │
│                                      │
│ 👤 张三 (女儿) - 创建者               │
│    ✓ 查看健康数据                    │
│    ✓ 查看用药提醒                    │
│    ✓ 编辑用药提醒                    │
│    ✓ 审批链接请求                    │
│    [编辑权限]                        │
│                                      │
│ 👤 李四 (儿子)                        │
│    ✓ 查看健康数据                    │
│    ✗ 查看用药提醒                    │
│    ✗ 编辑用药提醒                    │
│    ✗ 审批链接请求                    │
│    [编辑权限] [暂停] [移除]          │
└─────────────────────────────────────┘
```

### Link Guard 显示审批人信息
```
┌─────────────────────────────────────┐
│ 链接请求                             │
│                                      │
│ 👤 王五 (朋友)                        │
│    "我想帮助照顾您的健康"             │
│    发送时间: 2025-11-24 10:30        │
│                                      │
│    [批准] [拒绝]                     │
│                                      │
│ 👤 赵六 (邻居)                        │
│    "我可以帮忙提醒用药"               │
│    发送时间: 2025-11-23 15:20        │
│    审批人: 张三 已批准 ✓              │
│    批准时间: 2025-11-23 15:25        │
└─────────────────────────────────────┘
```

## 📋 测试清单

- [ ] Caregiver 创建老人账户时，linkRequestApprovers 自动包含创建者
- [ ] 老人自注册时，linkRequestApprovers 为空（老人自己有隐式权限）
- [ ] 老人可以在设置中添加/移除审批人
- [ ] 审批请求时正确记录 approvedBy 和时间戳
- [ ] 拒绝请求时正确记录 rejectedBy 和时间戳
- [ ] 老人可以查看每个 caregiver 的权限
- [ ] 老人可以修改 caregiver 的权限
- [ ] Firestore Rules 正确限制权限（老人 > 其他人）
