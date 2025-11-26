# PulseLink Database Schema - Actual Implementation

> 本文档基于代码实际写入的字段生成，确保与实现完全一致。
> 
> 生成日期：2025-11-26
> 架构版本：Plan C (Flat Collections)

---

## 架构概览

Plan C采用**扁平化独立集合**架构，所有数据存储在顶级集合中，通过ID字段建立关联关系。

### 核心设计原则

- ✅ **独立集合**：每个集合独立存在，不使用嵌套文档（除chat_history）
- ✅ **明确关系**：通过 `seniorId`、`caregiverId` 等字段建立关联
- ✅ **权限分离**：权限规则直接基于集合级别和字段验证
- ✅ **可扩展性**：新功能可直接添加新集合，不影响现有结构

---

## 集合结构总览

```
firestore/
├── users/                      # 用户认证信息（Auth UID → 用户基础数据）
├── senior_profiles/            # 老人档案（Profile ID → 老人基础信息）
├── caregiver_relations/        # 护理者关系（关系ID → 权限和称呼）
│                               # ⭐ 虚拟账户密码存储在创建者的关系记录中
├── health_records/             # 健康记录（记录ID → 健康数据）
└── chat_history/               # 聊天历史（嵌套子集合）
    └── {userId}/messages/
```

---

## 1. users 集合

**用途**：存储所有用户（Caregiver和Senior）的Firebase Auth基础信息

**集合路径**：`/users/{authUID}`

### 字段定义

| 字段名 | 类型 | 必填 | 说明 | 代码来源 |
|-------|------|------|------|---------|
| `uid` | string | ✅ | Firebase Auth UID（与文档ID相同） | `AuthRepositoryImpl.kt:98` |
| `email` | string | ✅ | 用户邮箱地址 | `AuthRepositoryImpl.kt:100` |
| `username` | string | ✅ | 用户名（Senior注册时使用真实姓名） | `AuthRepositoryImpl.kt:101` |
| `role` | string | ✅ | 用户角色：`SENIOR` 或 `CAREGIVER` | `AuthRepositoryImpl.kt:102` |
| `seniorId` | string | 🔶 | 老人Profile ID（仅SENIOR角色，格式：SNR-XXXXXXXXXXXX） | `AuthRepositoryImpl.kt:270` |
| `createdAt` | number | ✅ | 创建时间戳（毫秒） | `AuthRepositoryImpl.kt:103` |
| `emailVerified` | boolean | ✅ | 邮箱是否已验证 | `AuthRepositoryImpl.kt:104` |

### 文档ID规则

- **文档ID = Firebase Auth UID**
- 示例：`users/xyz789abc123def456`

### 数据示例

**Caregiver用户**：
```json
{
  "uid": "abc123def456",
  "email": "caregiver@example.com",
  "username": "John Smith",
  "role": "CAREGIVER",
  "createdAt": 1732579200000,
  "emailVerified": true
}
```

**Senior用户**：
```json
{
  "uid": "xyz789abc123",
  "email": "senior_SNR-KXM2VQW7ABCD@pulselink.app",
  "username": "张三",
  "role": "SENIOR",
  "seniorId": "SNR-KXM2VQW7ABCD",
  "createdAt": 1732579200000,
  "emailVerified": false
}
```

### 索引

- `uid`：文档ID，自动索引
- `seniorId`：单字段索引（用于反向查询：从Profile ID找Auth UID）

### 代码位置

- **写入**：`AuthRepositoryImpl.kt` (registerSenior(), register(), login())
- **查询**：`SeniorProfileRepositoryImpl.kt:216` (getAuthUid)

---

## 2. senior_profiles 集合

**用途**：存储老人档案的基础信息（可由护理者代创建，也可老人自注册）

**集合路径**：`/senior_profiles/{profileId}`

### 字段定义

| 字段名 | 类型 | 必填 | 说明 | 代码来源 |
|-------|------|------|------|---------|
| `id` | string | ✅ | 老人Profile ID（格式：SNR-XXXXXXXXXXXX） | `SeniorProfile.kt:17` |
| `userId` | string | 🔶 | 关联的Firebase Auth UID（老人登录后绑定，代创建时为null） | `SeniorProfile.kt:18` |
| `name` | string | ✅ | 老人姓名 | `SeniorProfile.kt:19` |
| `age` | number | ✅ | 年龄（整数） | `SeniorProfile.kt:20` |
| `gender` | string | ✅ | 性别：`Male` 或 `Female` | `SeniorProfile.kt:21` |
| `avatarType` | string | ✅ | 头像类型：`ELDERLY_MALE` / `ELDERLY_FEMALE` 等 | `SeniorProfile.kt:22` |
| `creatorId` | string | ✅ | 创建者的Auth UID（自注册时是自己，代创建时是护理者） | `SeniorProfile.kt:23` |
| `createdAt` | number | ✅ | 创建时间戳（毫秒） | `SeniorProfile.kt:24` |
| `registrationType` | string | ✅ | 注册类型：`SELF_REGISTERED` / `CAREGIVER_CREATED` | `SeniorProfile.kt:25` |

### 文档ID规则

- **文档ID = Profile ID**（与 `id` 字段相同）
- 示例：`senior_profiles/SNR-KXM2VQW7ABCD`

### Profile ID生成规则

- **格式**：`SNR-{时间戳36进制8位}{随机字母4位}`
- **示例**：`SNR-KXM2VQW7ABCD`
- **生成方法**：`AuthRepositoryImpl.kt:358-368` (generateSeniorId)
- **特点**：唯一性（时间戳 + 随机数），不可预测

### 数据示例

**自注册老人**：
```json
{
  "id": "SNR-KXM2VQW7ABCD",
  "userId": "xyz789abc123",
  "name": "张三",
  "age": 72,
  "gender": "Male",
  "avatarType": "ELDERLY_MALE",
  "creatorId": "xyz789abc123",
  "createdAt": 1732579200000,
  "registrationType": "SELF_REGISTERED"
}
```

**护理者代创建老人**：
```json
{
  "id": "SNR-ABC123DEF456",
  "userId": null,
  "name": "李四",
  "age": 68,
  "gender": "Female",
  "avatarType": "ELDERLY_FEMALE",
  "creatorId": "abc123def456",
  "createdAt": 1732579200000,
  "registrationType": "CAREGIVER_CREATED"
}
```

### 索引

- `id`：文档ID，自动索引
- `userId`：单字段索引（用于查询：Auth UID → Profile）
- `creatorId`：单字段索引（用于查询护理者创建的所有老人）

### 代码位置

- **模型**：`domain/model/SeniorProfile.kt`
- **Repository**：`SeniorProfileRepositoryImpl.kt`
- **写入**：`createProfile()`, `AuthRepositoryImpl.registerSenior()`

---

## 3. caregiver_relations 集合

**用途**：管理护理者与老人的关系、权限和称呼

**集合路径**：`/caregiver_relations/{relationId}`

### 字段定义

| 字段名 | 类型 | 必填 | 说明 | 代码来源 |
|-------|------|------|------|---------|
| `id` | string | ✅ | 关系ID（格式：`{caregiverId}_{seniorId}`） | `CaregiverRelation.kt:19` |
| `caregiverId` | string | ✅ | 护理者的Auth UID | `CaregiverRelation.kt:20` |
| `seniorId` | string | ✅ | 老人的Profile ID | `CaregiverRelation.kt:21` |
| `relationship` | string | ✅ | 护理者是老人的什么（Son/Daughter/Friend等） | `CaregiverRelation.kt:24` |
| `nickname` | string | ✅ | 护理者对老人的称呼（Father/Mother等） | `CaregiverRelation.kt:25` |
| `status` | string | ✅ | 状态：`pending` / `active` / `rejected` | `CaregiverRelation.kt:28` |
| `createdAt` | number | ✅ | 创建时间戳（毫秒） | `CaregiverRelation.kt:29` |
| `approvedAt` | number | 🔶 | 审批时间戳（status=active时有值） | `CaregiverRelation.kt:30` |
| `approvedBy` | string | 🔶 | 审批人Auth UID | `CaregiverRelation.kt:31` |
| `rejectedAt` | number | 🔶 | 拒绝时间戳（status=rejected时有值） | `CaregiverRelation.kt:32` |
| `rejectedBy` | string | 🔶 | 拒绝人Auth UID | `CaregiverRelation.kt:33` |
| `message` | string | ✅ | 申请消息（默认空字符串） | `CaregiverRelation.kt:34` |
| `canViewHealthData` | boolean | ✅ | 可查看健康数据（默认：true） | `CaregiverRelation.kt:37` |
| `canEditHealthData` | boolean | ✅ | 可编辑健康数据（默认：false） | `CaregiverRelation.kt:38` |
| `canViewReminders` | boolean | ✅ | 可查看提醒（默认：true） | `CaregiverRelation.kt:39` |
| `canEditReminders` | boolean | ✅ | 可编辑提醒（默认：true） | `CaregiverRelation.kt:40` |
| `canApproveRequests` | boolean | ✅ | 可审批申请（默认：false） | `CaregiverRelation.kt:41` |
| `virtualAccountPassword` | string | 🔶 | 虚拟账户密码（⭐ **仅创建者关系记录存储，明文**） | `CaregiverRelation.kt:44` |

### 密码存储机制

⚠️ **重要**：老人虚拟账户的密码存储在 `caregiver_relations` 集合中，而非独立的密码集合

**存储规则**：
- 只有**创建者的关系记录**才存储 `virtualAccountPassword` 字段
- 其他护理者的关系记录此字段为 `null`
- 密码以**明文**形式存储（TODO: 应加密或哈希）

**获取密码**：
```kotlin
// ManageSeniorsViewModel.kt:408
val relationId = CaregiverRelation.generateId(currentUserId, seniorProfileId)
val relationDoc = firestore.collection("caregiver_relations").document(relationId).get()
val password = relationDoc.getString("virtualAccountPassword")
```

**代码位置**：
- **写入**：`CreateSeniorProfileUseCase.kt:129`
- **读取**：`ManageSeniorsViewModel.kt:408` (getSeniorCredentials)

### 文档ID规则

- **文档ID = `{caregiverId}_{seniorId}`**
- **生成方法**：`CaregiverRelation.generateId(caregiverId, seniorId)`
- 示例：`caregiver_relations/abc123def456_SNR-KXM2VQW7ABCD`

### 关系类型（relationship）

由 `RelationshipHelper.kt` 定义，支持以下选项：
- `Son` / `Daughter` / `Spouse` / `Parent` / `Grandchild` / `Sibling` / `Friend` / `Caregiver` / `Other`

### 称呼（nickname）

根据relationship和gender自动映射：
- Son + Male → Father
- Daughter + Female → Mother
- Spouse + Male → Husband / Female → Wife
- 等等（详见 `RelationshipHelper.getDefaultAddressTitle()`）

### 数据示例

**创建者关系**：
```json
{
  "id": "abc123def456_SNR-KXM2VQW7ABCD",
  "caregiverId": "abc123def456",
  "seniorId": "SNR-KXM2VQW7ABCD",
  "relationship": "Son",
  "nickname": "Father",
  "status": "active",
  "createdAt": 1732579200000,
  "approvedAt": null,
  "approvedBy": null,
  "rejectedAt": null,
  "rejectedBy": null,
  "message": "",
  "canViewHealthData": true,
  "canEditHealthData": true,
  "canViewReminders": true,
  "canEditReminders": true,
  "canApproveRequests": true,
  "virtualAccountPassword": "GeneratedPassword123"
}
```

**待审核关系**：
```json
{
  "id": "xyz789ghi012_SNR-KXM2VQW7ABCD",
  "caregiverId": "xyz789ghi012",
  "seniorId": "SNR-KXM2VQW7ABCD",
  "relationship": "Friend",
  "nickname": "Elder Zhang",
  "status": "pending",
  "createdAt": 1732579200000,
  "approvedAt": null,
  "approvedBy": null,
  "rejectedAt": null,
  "rejectedBy": null,
  "message": "I would like to help with medication reminders",
  "canViewHealthData": true,
  "canEditHealthData": false,
  "canViewReminders": true,
  "canEditReminders": false,
  "canApproveRequests": false,
  "virtualAccountPassword": null
}
```

### 索引

- `id`：文档ID，自动索引
- `caregiverId`：单字段索引（查询护理者的所有关系）
- `seniorId`：单字段索引（查询老人的所有护理者）
- 复合索引：`caregiverId` + `status`（查询护理者的活跃关系）
- 复合索引：`seniorId` + `status`（查询老人的待审核申请）

### 代码位置

- **模型**：`domain/model/CaregiverRelation.kt`
- **Repository**：`CaregiverRelationRepositoryImpl.kt`
- **Helper**：`util/RelationshipHelper.kt`

---

## 4. health_records 集合

**用途**：存储所有类型的健康数据记录（血压、心率、血糖、体重等）

**集合路径**：`/health_records/{recordId}`

### 字段定义

| 字段名 | 类型 | 必填 | 说明 | 代码来源 |
|-------|------|------|------|---------|
| `id` | string | ✅ | 记录ID（Firestore自动生成或UUID） | `HealthRecord.kt:18` |
| `seniorId` | string | ✅ | 老人Profile ID | `HealthRecord.kt:19` |
| `type` | string | ✅ | 类型：`BLOOD_PRESSURE`/`HEART_RATE`/`BLOOD_SUGAR`/`WEIGHT` | `HealthRecord.kt:20` |
| `recordedAt` | number | ✅ | 记录时间戳（毫秒） | `HealthRecord.kt:21` |
| `recordedBy` | string | ✅ | 记录者Auth UID（老人或护理者） | `HealthRecord.kt:22` |
| `systolic` | number | 🔶 | 收缩压（仅type=BLOOD_PRESSURE） | `HealthRecord.kt:25` |
| `diastolic` | number | 🔶 | 舒张压（仅type=BLOOD_PRESSURE） | `HealthRecord.kt:26` |
| `heartRate` | number | 🔶 | 心率（bpm，可在BLOOD_PRESSURE或HEART_RATE记录） | `HealthRecord.kt:29` |
| `bloodSugar` | number | 🔶 | 血糖（mmol/L，仅type=BLOOD_SUGAR） | `HealthRecord.kt:32` |
| `weight` | number | 🔶 | 体重（kg，仅type=WEIGHT） | `HealthRecord.kt:35` |
| `notes` | string | ✅ | 备注（默认空字符串） | `HealthRecord.kt:38` |

### 文档ID规则

- **使用Firestore自动生成的ID或UUID**
- 示例：`health_records/aBcDeFgHiJkLmNoPqRsT`

### type字段值规范

⚠️ **重要**：type字段**必须使用全大写格式**

- ✅ 正确：`BLOOD_PRESSURE`, `HEART_RATE`, `BLOOD_SUGAR`, `WEIGHT`
- ❌ 错误：`blood_pressure`, `Blood_Pressure`, `bloodPressure`

这是因为：
1. 数据模型定义使用大写常量（`HealthRecord.TYPE_BLOOD_PRESSURE`）
2. 查询时使用大写进行匹配
3. 曾因大小写不一致导致查询失败（见 `TROUBLESHOOTING_AI.md`）

### 数据示例

**血压记录**：
```json
{
  "id": "rec123abc456def",
  "seniorId": "SNR-KXM2VQW7ABCD",
  "type": "BLOOD_PRESSURE",
  "recordedAt": 1732579200000,
  "recordedBy": "xyz789abc123",
  "systolic": 120,
  "diastolic": 80,
  "heartRate": 72,
  "bloodSugar": null,
  "weight": null,
  "notes": "Morning measurement, feeling good"
}
```

**心率记录**：
```json
{
  "id": "rec789ghi012jkl",
  "seniorId": "SNR-KXM2VQW7ABCD",
  "type": "HEART_RATE",
  "recordedAt": 1732579200000,
  "recordedBy": "SNR-KXM2VQW7ABCD",
  "systolic": null,
  "diastolic": null,
  "heartRate": 68,
  "bloodSugar": null,
  "weight": null,
  "notes": "After exercise"
}
```

### 索引

- `id`：文档ID，自动索引
- `seniorId`：单字段索引（查询某老人的所有记录）
- 复合索引：`seniorId` + `type` + `recordedAt DESC`（按类型查询并倒序）
- 复合索引：`seniorId` + `recordedAt DESC`（时间序列查询）

### 代码位置

- **模型**：`domain/model/HealthRecord.kt`
- **Repository**：`HealthRecordRepositoryImpl.kt`, `HealthRepositoryImpl.kt`
- **写入**：`saveHealthData()`, `createRecord()`

---

## 5. chat_history 集合

**用途**：存储用户与AI助手的聊天记录

**集合路径**：`/chat_history/{userId}/messages/{messageId}`

### 集合结构（嵌套）

```
chat_history/
  {userId}/                 # 用户Auth UID（文档）
    messages/               # 消息子集合
      {messageId}/          # 单条消息（自动生成ID）
```

### 字段定义

| 字段名 | 类型 | 必填 | 说明 | 代码来源 |
|-------|------|------|------|---------|
| `text` | string | ✅ | 消息文本内容 | `ChatRepositoryImpl.kt:86` |
| `fromAssistant` | boolean | ✅ | 是否来自AI（true=AI，false=用户） | `ChatRepositoryImpl.kt:87` |
| `timestamp` | number | ✅ | 消息时间戳（毫秒） | `ChatRepositoryImpl.kt:88` |

### 文档ID规则

- **父文档ID = Auth UID**
- **消息ID = Firestore自动生成**
- 示例：`chat_history/abc123def456/messages/msg789ghi012`

### 数据示例

**用户消息**：
```json
{
  "text": "我今天测的血压是120/80",
  "fromAssistant": false,
  "timestamp": 1732579200000
}
```

**AI回复**：
```json
{
  "text": "您的血压数据很正常，收缩压120mmHg和舒张压80mmHg都在健康范围内。建议继续保持良好的生活习惯。",
  "fromAssistant": true,
  "timestamp": 1732579205000
}
```

### 查询方式

- 获取聊天记录：`chat_history/{userId}/messages` 按 `timestamp ASC` 排序
- 保存消息：自动生成messageId或使用指定ID（覆盖更新）
- 清空历史：批量删除所有messages子文档

### 索引

- `timestamp ASC`：单字段索引（按时间正序显示对话）

### 代码位置

- **模型**：`domain/model/ChatMessage.kt`
- **Repository**：`ChatRepositoryImpl.kt`
- **查询**：`getChatHistory()`（实时Flow监听）
- **写入**：`saveMessage()`

---

## 关系映射

### Senior Profile ↔ User

```
senior_profiles/{profileId}          users/{authUID}
┌──────────────────────┐             ┌──────────────────────┐
│ id: SNR-ABC123       │ ←userId─────│ uid: xyz789          │
│ userId: xyz789       │─────┐       │ seniorId: SNR-ABC123 │
│ name: "张三"         │     └──────→│ role: SENIOR         │
└──────────────────────┘             └──────────────────────┘
```

- **双向关联**：`senior_profiles.userId` ↔ `users.uid`
- **反向查询**：`users.seniorId` → `senior_profiles.id`

### Caregiver ↔ Senior (通过 caregiver_relations)

```
users/{caregiverUID}                 caregiver_relations/{relationId}      senior_profiles/{profileId}
┌──────────────────┐                 ┌────────────────────────────────┐    ┌──────────────────────┐
│ uid: abc123      │────caregiverId─→│ id: abc123_SNR-ABC123          │←──┐│ id: SNR-ABC123       │
│ role: CAREGIVER  │                 │ caregiverId: abc123            │   ││ name: "张三"         │
└──────────────────┘                 │ seniorId: SNR-ABC123           │───┘│ creatorId: abc123    │
                                     │ relationship: Son              │    └──────────────────────┘
                                     │ nickname: Father               │
                                     │ status: active                 │
                                     │ virtualAccountPassword: "..." │
                                     └────────────────────────────────┘
```

### Senior ↔ Health Records

```
senior_profiles/{profileId}          health_records/{recordId}
┌──────────────────────┐             ┌──────────────────────────┐
│ id: SNR-ABC123       │←─seniorId───│ seniorId: SNR-ABC123     │
│ name: "张三"         │             │ type: BLOOD_PRESSURE     │
└──────────────────────┘             │ systolic: 120            │
                                     │ diastolic: 80            │
                                     │ recordedBy: xyz789       │
                                     └──────────────────────────┘
```

---

## Firestore索引配置

### 必需的复合索引

```json
{
  "indexes": [
    {
      "collectionGroup": "caregiver_relations",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "caregiverId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" }
      ]
    },
    {
      "collectionGroup": "caregiver_relations",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "seniorId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" }
      ]
    },
    {
      "collectionGroup": "health_records",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "seniorId", "order": "ASCENDING" },
        { "fieldPath": "type", "order": "ASCENDING" },
        { "fieldPath": "recordedAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "health_records",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "seniorId", "order": "ASCENDING" },
        { "fieldPath": "recordedAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "messages",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "timestamp", "order": "ASCENDING" }
      ]
    }
  ]
}
```

### 单字段索引（自动创建）

- `users.seniorId`
- `senior_profiles.userId`
- `senior_profiles.creatorId`
- `caregiver_relations.caregiverId`
- `caregiver_relations.seniorId`
- `health_records.seniorId`

---

## Firestore安全规则

### 核心规则摘要

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // 1. users集合：仅本人读写
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // 2. senior_profiles：创建者和关联老人可读写
    match /senior_profiles/{profileId} {
      allow read: if request.auth != null && (
        resource.data.userId == request.auth.uid ||
        resource.data.creatorId == request.auth.uid ||
        hasActiveRelation(request.auth.uid, profileId)
      );
      allow write: if request.auth != null && (
        resource.data.creatorId == request.auth.uid ||
        resource.data.userId == request.auth.uid
      );
    }
    
    // 3. caregiver_relations：关系双方可读，创建者可写
    match /caregiver_relations/{relationId} {
      allow read: if request.auth != null && (
        resource.data.caregiverId == request.auth.uid ||
        isSeniorOwner(resource.data.seniorId, request.auth.uid)
      );
      allow create: if request.auth != null && request.resource.data.caregiverId == request.auth.uid;
      allow update: if request.auth != null && isSeniorOwner(resource.data.seniorId, request.auth.uid);
      allow delete: if request.auth != null && resource.data.caregiverId == request.auth.uid;
    }
    
    // 4. health_records：老人和有权限的护理者可读写
    match /health_records/{recordId} {
      allow read: if request.auth != null && (
        isSeniorOwner(resource.data.seniorId, request.auth.uid) ||
        canViewHealthData(request.auth.uid, resource.data.seniorId)
      );
      allow create: if request.auth != null && (
        isSeniorOwner(request.resource.data.seniorId, request.auth.uid) ||
        canEditHealthData(request.auth.uid, request.resource.data.seniorId)
      );
      allow update, delete: if request.auth != null && (
        resource.data.recordedBy == request.auth.uid ||
        canEditHealthData(request.auth.uid, resource.data.seniorId)
      );
    }
    
    // 5. chat_history：仅本人可读写
    match /chat_history/{userId}/messages/{messageId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

完整规则见：`firestore.rules`

---

## 数据迁移说明

### 从旧架构（嵌套结构）迁移

#### 迁移映射

| 旧集合路径 | 新集合路径 | 说明 |
|-----------|-----------|------|
| `seniors/{seniorId}` | `senior_profiles/{profileId}` | 档案信息 |
| `seniors/{seniorId}/health_data/...` | `health_records/{recordId}` | 健康数据独立集合 |
| `seniors/{seniorId}/caregivers/...` | `caregiver_relations/{relationId}` | 关系独立集合 |
| `link_requests/{requestId}` | `caregiver_relations/{relationId}` | status=pending |

#### 迁移脚本位置

（TODO：添加数据迁移脚本到 `scripts/` 目录）

---

## 最佳实践

### 1. 查询优化

✅ **使用索引查询**：
```kotlin
// 好：使用复合索引
firestore.collection("health_records")
    .whereEqualTo("seniorId", profileId)
    .whereEqualTo("type", "BLOOD_PRESSURE")
    .orderBy("recordedAt", Query.Direction.DESCENDING)
    .limit(10)
```

❌ **避免全集合扫描**：
```kotlin
// 差：没有索引支持
firestore.collection("health_records")
    .orderBy("recordedAt")
    .get()
```

### 2. 批量操作

使用WriteBatch减少网络往返：
```kotlin
val batch = firestore.batch()
records.forEach { record ->
    val ref = recordsCollection.document(record.id)
    batch.set(ref, record.toFirestoreMap())
}
batch.commit().await()
```

### 3. 实时监听

使用Snapshot Listener获取实时更新：
```kotlin
firestore.collection("chat_history")
    .document(userId)
    .collection("messages")
    .orderBy("timestamp", Query.Direction.ASCENDING)
    .addSnapshotListener { snapshot, error ->
        // 处理实时更新
    }
```

### 4. 错误处理

始终包裹try-catch并返回Result：
```kotlin
override suspend fun getProfile(id: String): Result<SeniorProfile> {
    return try {
        val doc = profilesCollection.document(id).get().await()
        if (!doc.exists()) {
            Result.failure(Exception("Profile not found"))
        } else {
            Result.success(doc.toSeniorProfile())
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getting profile", e)
        Result.failure(e)
    }
}
```

---

## 常见问题

### Q1: 为什么type字段必须大写？

A: 数据模型使用常量定义（`HealthRecord.TYPE_BLOOD_PRESSURE`），查询时必须匹配。曾因大小写不一致导致查询返回空结果（详见 `TROUBLESHOOTING_AI.md`）。

### Q2: userId和seniorId的区别？

A:
- **userId**：Firebase Auth UID（用于身份验证）
- **seniorId**：老人Profile ID（SNR-前缀，用于业务逻辑）
- Senior用户同时拥有两者，Caregiver只有userId

### Q3: 密码为什么存储在 caregiver_relations 而不是独立集合？

A: 
- **当前实现**：密码存储在创建者的 `caregiver_relations.virtualAccountPassword` 字段
- **原因**：创建者需要获取密码才能帮助老人登录
- **安全性**：只有创建者可以读取密码，其他护理者看不到
- **TODO**：未来应改为加密存储或迁移到专用密码管理系统

### Q4: 如何查询护理者管理的所有老人？

A: 组合两种查询：
```kotlin
// 1. 通过关系查询
val relations = firestore.collection("caregiver_relations")
    .whereEqualTo("caregiverId", uid)
    .whereEqualTo("status", "active")
    .get()

// 2. 通过创建者查询
val createdProfiles = firestore.collection("senior_profiles")
    .whereEqualTo("creatorId", uid)
    .get()
```

---

## 代码参考

### 关键文件

- **数据模型**：
  - `app/src/main/java/com/alvin/pulselink/domain/model/`
    - `SeniorProfile.kt`
    - `CaregiverRelation.kt`
    - `HealthRecord.kt`
    - `ChatMessage.kt`
    - `User.kt`

- **Repository实现**：
  - `app/src/main/java/com/alvin/pulselink/data/repository/`
    - `SeniorProfileRepositoryImpl.kt`
    - `CaregiverRelationRepositoryImpl.kt`
    - `HealthRecordRepositoryImpl.kt`
    - `ChatRepositoryImpl.kt`
    - `AuthRepositoryImpl.kt`
    - `HealthRepositoryImpl.kt`

- **工具类**：
  - `app/src/main/java/com/alvin/pulselink/util/`
    - `SnrIdGenerator.kt`
    - `RelationshipHelper.kt`

### 测试用例

（TODO：添加集成测试到 `app/src/androidTest/`）

---

## 版本历史

- **v1.0** (2025-11-26): Plan C架构实现完成
  - 扁平化集合结构
  - 关系独立管理
  - 健康记录独立存储
  - 权限系统重构

---

## 维护说明

### 更新此文档

当修改数据库字段时，请同步更新：
1. 本文档对应的字段说明
2. 添加代码来源引用（文件名 + 行号）
3. 更新数据示例
4. 检查索引配置是否需要调整
5. 更新安全规则（如有必要）

### 文档生成

本文档基于以下代码生成：
```bash
git commit: a465219
branch: refactor/schema-c
date: 2025-11-26
```

---

**文档结束**
