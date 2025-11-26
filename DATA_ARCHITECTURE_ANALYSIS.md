# PulseLink 数据架构分析与优化方案

## 📋 目录
1. [当前架构概述](#1-当前架构概述)
2. [问题诊断](#2-问题诊断)
3. [性能瓶颈分析](#3-性能瓶颈分析)
4. [权限控制复杂度分析](#4-权限控制复杂度分析)
5. [优化方案](#5-优化方案)
6. [推荐的新架构](#6-推荐的新架构)
7. [迁移路径](#7-迁移路径)

---

## 1. 当前架构概述

### 1.1 核心数据模型

```
Firestore Collections:
├── users/{userId}           # 用户基本信息
├── seniors/{seniorId}       # 老人账户（核心集合，嵌套数据多）
├── linkRequests/{requestId} # 链接请求
├── health_data/{userId}/... # 健康数据
├── reminders/{userId}/...   # 提醒数据
└── chat_history/{userId}/...# 聊天记录
```

### 1.2 Senior 文档结构（当前）

```kotlin
Senior(
    id: String,                    // 虚拟ID: SNR-XXXXXXXX
    name: String,
    age: Int,
    gender: String,
    avatarType: String,
    password: String,              // ⚠️ 密码存储在文档中
    creatorId: String,             // 创建者 UID
    registrationType: String,      // CAREGIVER_CREATED | SELF_REGISTERED
    createdAt: Long,
    
    // ⚠️ 嵌套数组和 Map（核心问题）
    caregiverIds: List<String>,              // 已绑定护理者列表
    pendingCaregiversIds: List<String>,      // 待审核护理者列表（已废弃？）
    caregiverRelationships: Map<String, CaregiverRelationship>, // 关系详情
    healthHistory: HealthHistory             // 健康数据快照
)

CaregiverRelationship(
    relationship: String,     // "Son", "Daughter"
    nickname: String,         // "爸爸", "妈妈"
    linkedAt: Long,
    status: String,           // pending/active/rejected
    message: String,
    approvedBy: String,
    permissions: CaregiverPermissions(
        canViewHealthData: Boolean,
        canViewReminders: Boolean,
        canEditReminders: Boolean,
        canApproveLinkRequests: Boolean
    )
)
```

---

## 2. 问题诊断

### 2.1 ❌ 字段冗余与职责不清

| 问题 | 描述 | 影响 |
|------|------|------|
| `caregiverIds` vs `caregiverRelationships` | 同一信息存储两处 | 数据不一致风险，更新需同步两处 |
| `pendingCaregiversIds` vs `caregiverRelationships.status` | 待审核状态冗余存储 | 字段可能永远不同步 |
| `pendingCaregiversIds` vs `linkRequests` 集合 | 已有独立集合存储请求 | `pendingCaregiversIds` 完全多余 |
| `healthHistory` 嵌套在 Senior | 健康数据应该独立 | 每次读 Senior 都读取健康数据 |

### 2.2 ❌ 查询效率低下

**当前查询逻辑（getSeniorsByCaregiver）：**
```kotlin
// 需要执行 2 次查询！
val snapshot1 = seniorsCollection
    .whereArrayContains("caregiverIds", caregiverId)  // 查询1
    .get().await()
    
val snapshot2 = seniorsCollection
    .whereEqualTo("creatorId", caregiverId)           // 查询2
    .get().await()

// 然后在客户端合并去重
val allDocs = (snapshot1.documents + snapshot2.documents).distinctBy { it.id }
```

**问题：**
- 每次加载需要 **2 次网络请求**
- Firestore **不支持 OR 查询**，必须分开查
- 客户端合并增加处理时间
- 数组查询 `whereArrayContains` **无法与其他条件组合**

### 2.3 ❌ Firestore Rules 过于复杂

```javascript
// 当前规则需要多次 get() 调用来验证权限
function isSeniorSelf() {
    return isAuthenticated() 
           && get(/databases/$(database)/documents/users/$(request.auth.uid))
              .data.get('seniorId', null) == seniorId;
}

function canApprove() {
    let senior = get(/databases/$(database)/documents/seniors/$(resource.data.seniorId));
    return isSeniorOwner() 
        || (isAuthenticated() && request.auth.uid in senior.data.get('linkRequestApprovers', []));
}
```

**问题：**
- 每次请求可能触发 **2-3 次额外的 Firestore 读取**
- 增加延迟和计费成本
- 规则复杂难以维护和调试

### 2.4 ❌ 数据一致性风险

```
场景：批准链接请求后需要更新：
1. linkRequests/{id}.status = "approved"
2. seniors/{id}.caregiverIds.add(requesterId)
3. seniors/{id}.caregiverRelationships[requesterId].status = "active"

⚠️ 如果步骤 2 或 3 失败，数据不一致！
```

---

## 3. 性能瓶颈分析

### 3.1 加载 Dashboard 的请求链

```
用户打开 Caregiver 主页：
  │
  ├─→ Query 1: seniors.whereArrayContains("caregiverIds", uid)  [~200ms]
  ├─→ Query 2: seniors.whereEqualTo("creatorId", uid)           [~200ms]
  │
  ├─→ 客户端合并去重                                              [~50ms]
  │
  └─→ 对每个 Senior 解析 caregiverRelationships Map              [~20ms × N]
  
总计：~400ms + 20ms × 老人数量
```

### 3.2 文档大小问题

一个典型的 Senior 文档可能达到 **5-10KB**：
- `caregiverRelationships` Map 每个护理者 ~500 bytes
- `healthHistory` 嵌套对象 ~1KB
- 每次查询都读取全部数据

### 3.3 缺失的索引

```json
// firestore.indexes.json - 当前为空！
{
  "indexes": [],
  "fieldOverrides": []
}
```

**应该添加的索引：**
- `seniors` 集合：`caregiverIds` (Array) + `createdAt` (DESC)
- `seniors` 集合：`creatorId` + `createdAt` (DESC)
- `linkRequests` 集合：`seniorId` + `status` + `createdAt`

---

## 4. 权限控制复杂度分析

### 4.1 当前权限模型

```
权限来源：
├── creatorId          → 创建者权限
├── caregiverIds[]     → 是否已绑定
├── caregiverRelationships[uid].status → 绑定状态
├── caregiverRelationships[uid].permissions.canXxx → 细粒度权限
└── (已删除) linkRequestApprovers[] → 审批权限

问题：权限分散在多个字段，判断逻辑复杂
```

### 4.2 规则复杂度对比

**当前（复杂）：**
```javascript
allow update: if isSeniorSelf()
              || (isCreator() && !request.resource.data.diff(resource.data)
                  .changedKeys().hasAny(['caregiverRelationships']))
              || (isAuthenticated()
                  && !(request.auth.uid in resource.data.caregiverIds)
                  && !(request.auth.uid in request.resource.data.caregiverIds)
                  && request.resource.data.creatorId == resource.data.creatorId
                  && request.resource.data.diff(resource.data)
                     .changedKeys().hasOnly(['caregiverRelationships'])
                  && request.resource.data.caregiverIds == resource.data.caregiverIds)
              || (isCaregiverBound() && hasPermissionToUpdate());
```

**理想（简洁）：**
```javascript
allow update: if isOwner() || hasRole('admin') || hasRole('caregiver');
```

---

## 5. 优化方案

### 5.1 方案对比

| 方案 | 改动量 | 效果 | 推荐度 |
|------|--------|------|--------|
| A. 添加索引 + 小重构 | 小 | 中等 | ⭐⭐⭐ 短期 |
| B. 引入关系集合 | 中 | 显著 | ⭐⭐⭐⭐ 中期 |
| C. 完全重构 | 大 | 最优 | ⭐⭐⭐⭐⭐ 长期 |

### 5.2 方案 A：快速优化（1-2 天）

#### 5.2.1 添加复合索引

```json
// firestore.indexes.json
{
  "indexes": [
    {
      "collectionGroup": "seniors",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "creatorId", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "linkRequests",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "requesterId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "linkRequests",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "seniorId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    }
  ]
}
```

#### 5.2.2 移除冗余字段

```kotlin
// 删除 pendingCaregiversIds（完全由 linkRequests 集合管理）
data class Senior(
    // ... 保留其他字段
    // val pendingCaregiversIds: List<String> = emptyList(),  // 删除
)
```

#### 5.2.3 优化解析逻辑

```kotlin
// 创建扩展函数，避免重复代码
fun DocumentSnapshot.toSenior(): Senior? {
    return try {
        // ... 统一的解析逻辑
    } catch (e: Exception) {
        Log.e("SeniorRepo", "Failed to parse senior ${id}", e)
        null
    }
}
```

### 5.3 方案 B：引入关系集合（1 周）

#### 核心思想：将 caregiverRelationships Map 拆分为独立集合

**新增集合：`caregiver_senior_relations/{relationId}`**

```kotlin
data class CaregiverSeniorRelation(
    val id: String,                // 关系ID
    val caregiverId: String,       // 护理者 UID (indexed)
    val seniorId: String,          // 老人 ID (indexed)
    val relationship: String,      // "Son", "Daughter"
    val nickname: String,          // 称呼
    val status: String,            // pending/active/rejected
    val linkedAt: Long,
    val approvedBy: String,
    
    // 权限（扁平化）
    val canViewHealthData: Boolean,
    val canViewReminders: Boolean,
    val canEditReminders: Boolean,
    val canApproveLinkRequests: Boolean
)
```

**优势：**
```kotlin
// 单次查询获取所有关联的老人
val relations = relationsCollection
    .whereEqualTo("caregiverId", caregiverId)
    .whereEqualTo("status", "active")
    .get().await()

val seniorIds = relations.map { it.seniorId }

// 批量获取老人信息
val seniors = seniorsCollection
    .whereIn("id", seniorIds)  // 单次查询！
    .get().await()
```

**Firestore Rules 简化：**
```javascript
match /seniors/{seniorId} {
    function hasActiveRelation() {
        return exists(/databases/$(database)/documents/caregiver_senior_relations/$(request.auth.uid + "_" + seniorId))
            && get(/databases/$(database)/documents/caregiver_senior_relations/$(request.auth.uid + "_" + seniorId)).data.status == 'active';
    }
    
    allow read: if isAuthenticated() && (isOwner() || hasActiveRelation());
}
```

### 5.4 方案 C：完全重构（2-3 周）

#### 新架构设计

```
Collections:
├── users/{userId}
│   ├── id: string
│   ├── email: string
│   ├── name: string
│   ├── role: "senior" | "caregiver"
│   └── seniorProfileId?: string  (仅 senior 有)
│
├── senior_profiles/{profileId}
│   ├── id: string
│   ├── userId: string  (关联 Firebase Auth UID)
│   ├── name: string
│   ├── age: int
│   ├── gender: string
│   ├── avatarType: string
│   ├── createdAt: timestamp
│   └── creatorId: string  (创建者 UID)
│
├── caregiver_relations/{relationId}  ← 核心：关系独立
│   ├── id: string = `${caregiverId}_${seniorProfileId}`
│   ├── caregiverId: string (indexed)
│   ├── seniorProfileId: string (indexed)
│   ├── status: "pending" | "active" | "rejected"
│   ├── relationship: string
│   ├── nickname: string
│   ├── permissions: map
│   ├── createdAt: timestamp
│   ├── approvedAt?: timestamp
│   └── approvedBy?: string
│
├── health_records/{recordId}  ← 健康数据独立
│   ├── seniorProfileId: string (indexed)
│   ├── type: "blood_pressure" | "heart_rate" | "blood_sugar"
│   ├── value: map
│   ├── recordedAt: timestamp
│   └── recordedBy: string
│
└── link_requests/{requestId}  ← 保持不变
```

#### 查询优化效果

| 场景 | 当前 | 优化后 |
|------|------|--------|
| 加载 Dashboard | 2 查询 + 客户端合并 | 1 查询 relations → 1 查询 seniors |
| 获取老人健康数据 | 读取整个 Senior 文档 | 只读取 health_records |
| 检查权限 | 解析 Map 找对应关系 | 直接读取 relation 文档 |
| 添加新护理者 | 更新 Senior 文档 | 创建新 relation 文档 |

---

## 6. 推荐的新架构

### 6.1 简化后的 Senior 模型

```kotlin
data class SeniorProfile(
    val id: String,
    val userId: String,        // Firebase Auth UID
    val name: String,
    val age: Int,
    val gender: String,
    val avatarType: String,
    val creatorId: String,
    val createdAt: Long
    // ❌ 不再包含 caregiverIds
    // ❌ 不再包含 caregiverRelationships
    // ❌ 不再包含 healthHistory
    // ❌ 不再包含 password（移到 users 或单独存储）
)
```

### 6.2 独立的关系集合

```kotlin
data class CaregiverRelation(
    val id: String,  // `${caregiverId}_${seniorId}`
    val caregiverId: String,
    val seniorId: String,
    val status: String,
    val relationship: String,
    val nickname: String,
    val linkedAt: Long,
    val approvedBy: String?,
    val approvedAt: Long?,
    
    // 扁平权限
    val canViewHealthData: Boolean = true,
    val canEditHealthData: Boolean = false,
    val canViewReminders: Boolean = true,
    val canEditReminders: Boolean = true,
    val canApproveRequests: Boolean = false
)
```

### 6.3 简化的 Firestore Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // 用户
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }
    
    // 老人档案
    match /senior_profiles/{profileId} {
      allow read: if request.auth != null && (
        resource.data.userId == request.auth.uid ||
        resource.data.creatorId == request.auth.uid ||
        hasActiveRelation(profileId)
      );
      allow create: if request.auth != null && request.resource.data.creatorId == request.auth.uid;
      allow update: if request.auth != null && (
        resource.data.userId == request.auth.uid ||
        resource.data.creatorId == request.auth.uid
      );
      allow delete: if request.auth != null && resource.data.creatorId == request.auth.uid;
    }
    
    // 护理关系
    match /caregiver_relations/{relationId} {
      allow read: if request.auth != null && (
        resource.data.caregiverId == request.auth.uid ||
        isSeniorOwner(resource.data.seniorId)
      );
      allow create: if request.auth != null && request.resource.data.caregiverId == request.auth.uid;
      allow update: if request.auth != null && isSeniorOwner(resource.data.seniorId);
      allow delete: if request.auth != null && (
        resource.data.caregiverId == request.auth.uid ||
        isSeniorOwner(resource.data.seniorId)
      );
    }
    
    // 辅助函数
    function hasActiveRelation(seniorId) {
      let relationId = request.auth.uid + '_' + seniorId;
      return exists(/databases/$(database)/documents/caregiver_relations/$(relationId))
          && get(/databases/$(database)/documents/caregiver_relations/$(relationId)).data.status == 'active';
    }
    
    function isSeniorOwner(seniorId) {
      let profile = get(/databases/$(database)/documents/senior_profiles/$(seniorId));
      return profile.data.userId == request.auth.uid || profile.data.creatorId == request.auth.uid;
    }
  }
}
```

---

## 7. 迁移路径

### 7.1 阶段 1：快速优化（本周）

1. ✅ 添加 Firestore 索引
2. ✅ 移除 `pendingCaregiversIds` 字段
3. ✅ 统一数据解析代码（提取公共方法）
4. ✅ 添加日志监控查询耗时

### 7.2 阶段 2：引入关系集合（下周）

1. 创建 `caregiver_relations` 集合
2. 编写数据迁移脚本
3. 修改 Repository 层使用新集合
4. 保持旧字段兼容，双写一段时间
5. 更新 Firestore Rules

### 7.3 阶段 3：清理旧字段（2 周后）

1. 停止写入旧字段
2. 运行清理脚本删除旧字段
3. 移除兼容代码

---

## 📊 预期改进效果

| 指标 | 当前 | 优化后 |
|------|------|--------|
| Dashboard 加载 | ~500ms | ~200ms |
| 单次查询数 | 2 次 | 1 次 |
| Firestore Rules 行数 | ~150 行 | ~50 行 |
| 权限判断 get() 调用 | 2-3 次 | 0-1 次 |
| Senior 文档大小 | 5-10KB | 1-2KB |
| 数据一致性风险 | 高 | 低 |

---

## 🎯 立即行动项

1. **今天**：部署 firestore.indexes.json
2. **明天**：移除 pendingCaregiversIds，统一解析代码
3. **本周**：评估方案 B/C，确定长期方向
4. **下周**：开始实施关系集合方案

---

*文档生成时间：2025-11-26*
*作者：GitHub Copilot*
