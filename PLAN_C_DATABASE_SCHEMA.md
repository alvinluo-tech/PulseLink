# Plan C Database Schema Documentation

## Architecture Overview

Plan C采用**扁平化独立集合**架构，将原本嵌套的数据结构拆分为独立的顶级集合，每个集合只负责单一职责。

### Core Principles
- ✅ **独立性**: 每个集合独立存在，不依赖嵌套结构
- ✅ **可查询性**: 所有集合支持直接查询和索引
- ✅ **关系明确**: 通过ID字段建立关联关系
- ✅ **扩展性**: 易于添加新功能和新集合

---

## Collection Structure

```
firestore/
├── users/                      # 用户认证信息
├── senior_profiles/            # 老人档案（核心）
├── caregiver_relations/        # 护理者关系
├── health_records/             # 健康记录
├── chat_history/               # 聊天历史
└── reminders/                  # 用药提醒
```

---

## 1. users Collection

**用途**: 存储 Firebase Auth 用户的基础信息和角色

### Document Structure

```typescript
/users/{userId}
{
  // 基础信息
  id: string                    // 与 documentId 相同
  email: string                 // 用户邮箱
  role: "CAREGIVER" | "SENIOR"  // 用户角色
  createdAt: number             // 创建时间戳
  
  // 老人特有字段
  seniorId?: string             // 老人的 profile ID (仅 SENIOR 角色)
  name?: string                 // 老人姓名 (仅 SENIOR 角色)
}
```

### Field Details

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | ✅ | Firebase Auth UID |
| `email` | string | ✅ | 用户邮箱地址 |
| `role` | string | ✅ | 用户角色：CAREGIVER 或 SENIOR |
| `createdAt` | number | ✅ | Unix 时间戳（毫秒） |
| `seniorId` | string | ⚪ | 老人账号的 profile ID，仅老人角色有此字段 |
| `name` | string | ⚪ | 老人姓名，仅老人角色有此字段 |

### Usage Examples

```kotlin
// Caregiver 用户
{
  "id": "abc123",
  "email": "caregiver@example.com",
  "role": "CAREGIVER",
  "createdAt": 1700000000000
}

// Senior 用户
{
  "id": "xyz789",
  "email": "senior_SNR-ABC123@pulselink.app",
  "role": "SENIOR",
  "seniorId": "SNR-ABC123",
  "name": "张三",
  "createdAt": 1700000000000
}
```

### Indexes
- `email` (default)
- `role` (auto-created)

---

## 2. senior_profiles Collection

**用途**: 存储老人的基本档案信息

### Document Structure

```typescript
/senior_profiles/{profileId}
{
  // 基础信息
  id: string                    // 老人档案ID，格式: SNR-XXXXXXXXXXXX
  userId: string | null         // 对应的 Firebase Auth UID (如果老人已登录)
  name: string                  // 老人姓名
  age: number                   // 年龄
  gender: "Male" | "Female"     // 性别
  avatarType: string            // 头像类型，如 "GRANDFATHER", "GRANDMOTHER"
  
  // 管理信息
  creatorId: string             // 创建者的 Firebase Auth UID (caregiver)
  createdAt: number             // 创建时间戳
  registrationType: string      // 注册类型: "CAREGIVER_CREATED" | "SELF_REGISTERED"
}
```

### Field Details

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | ✅ | 老人档案唯一ID，格式 SNR-XXXXXXXXXXXX |
| `userId` | string/null | ⚪ | Firebase Auth UID，老人登录后才有值 |
| `name` | string | ✅ | 老人真实姓名 |
| `age` | number | ✅ | 年龄 |
| `gender` | string | ✅ | 性别：Male 或 Female |
| `avatarType` | string | ✅ | 头像类型，根据年龄性别自动选择 |
| `creatorId` | string | ✅ | 创建此档案的 caregiver UID |
| `createdAt` | number | ✅ | Unix 时间戳（毫秒） |
| `registrationType` | string | ✅ | 注册方式 |

### Avatar Types

```kotlin
GRANDFATHER     // 爷爷 (60+ 男性)
GRANDMOTHER     // 奶奶 (60+ 女性)
FATHER          // 父亲 (40-59 男性)
MOTHER          // 母亲 (40-59 女性)
UNCLE           // 叔叔 (18-39 男性)
AUNT            // 阿姨 (18-39 女性)
```

### Usage Example

```kotlin
{
  "id": "SNR-ABC123XYZ456",
  "userId": "firebase_uid_xyz789",
  "name": "张三",
  "age": 75,
  "gender": "Male",
  "avatarType": "GRANDFATHER",
  "creatorId": "caregiver_uid_abc123",
  "createdAt": 1700000000000,
  "registrationType": "CAREGIVER_CREATED"
}
```

### Indexes

```javascript
{
  "collectionGroup": "senior_profiles",
  "fields": [
    { "fieldPath": "creatorId", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

## 3. caregiver_relations Collection

**用途**: 管理 caregiver 和 senior 之间的关系和权限

### Document Structure

```typescript
/caregiver_relations/{relationId}
{
  // 关系标识
  id: string                    // 关系ID，格式: {caregiverId}_{seniorId}
  caregiverId: string           // 护理者的 Firebase Auth UID
  seniorId: string              // 老人的 profile ID
  
  // 关系信息
  status: string                // 状态: "pending" | "active" | "rejected"
  relationship: string          // 关系类型: "Son", "Daughter", "Grandson"等
  nickname: string              // 护理者对老人的称呼: "Dad", "Mom", "Grandpa"等
  message: string               // 绑定请求时的留言
  
  // 权限控制
  canViewHealthData: boolean    // 是否可查看健康数据
  canEditHealthData: boolean    // 是否可编辑健康数据
  canViewReminders: boolean     // 是否可查看用药提醒
  canEditReminders: boolean     // 是否可编辑用药提醒
  canApproveRequests: boolean   // 是否可审批绑定请求
  
  // 时间戳
  createdAt: number             // 创建时间
  updatedAt: number             // 最后更新时间
  approvedAt?: number           // 批准时间
  rejectedAt?: number           // 拒绝时间
  approvedBy?: string           // 批准人 UID
  rejectedBy?: string           // 拒绝人 UID
  
  // 虚拟账号密码（仅创建者）
  virtualAccountPassword?: string  // 老人账号密码（仅创建者的关系记录有此字段）
}
```

### Field Details

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | ✅ | 格式: {caregiverId}_{seniorId} |
| `caregiverId` | string | ✅ | 护理者 Firebase Auth UID |
| `seniorId` | string | ✅ | 老人 profile ID |
| `status` | string | ✅ | pending/active/rejected |
| `relationship` | string | ✅ | 关系类型 |
| `nickname` | string | ✅ | 对老人的称呼 |
| `message` | string | ⚪ | 绑定请求留言 |
| `canViewHealthData` | boolean | ✅ | 查看健康数据权限 |
| `canEditHealthData` | boolean | ✅ | 编辑健康数据权限 |
| `canViewReminders` | boolean | ✅ | 查看提醒权限 |
| `canEditReminders` | boolean | ✅ | 编辑提醒权限 |
| `canApproveRequests` | boolean | ✅ | 审批请求权限 |
| `createdAt` | number | ✅ | 创建时间戳 |
| `updatedAt` | number | ✅ | 更新时间戳 |
| `approvedAt` | number | ⚪ | 批准时间戳 |
| `rejectedAt` | number | ⚪ | 拒绝时间戳 |
| `approvedBy` | string | ⚪ | 批准人 UID |
| `rejectedBy` | string | ⚪ | 拒绝人 UID |
| `virtualAccountPassword` | string | ⚪ | 仅创建者有此字段 |

### Relationship Types

```kotlin
Son               // 儿子
Daughter          // 女儿
Grandson          // 孙子
Granddaughter     // 孙女
Spouse            // 配偶
Sibling           // 兄弟姐妹
Friend            // 朋友
Caregiver         // 护工
Other             // 其他
```

### Relationship to Nickname Mapping

通过 `RelationshipHelper.getDefaultAddressTitle(relationship, gender)` 自动映射：

| Relationship | Senior Gender | Default Nickname |
|-------------|---------------|------------------|
| Son/Daughter | Male | Father |
| Son/Daughter | Female | Mother |
| Grandson/Granddaughter | Male | Grandfather |
| Grandson/Granddaughter | Female | Grandmother |
| Spouse | Any | Spouse |
| Sibling | Male | Brother |
| Sibling | Female | Sister |

### Usage Examples

```kotlin
// 创建者的关系（active，有完整权限和密码）
{
  "id": "caregiver123_SNR-ABC123",
  "caregiverId": "caregiver123",
  "seniorId": "SNR-ABC123",
  "status": "active",
  "relationship": "Son",
  "nickname": "Father",
  "message": "",
  "canViewHealthData": true,
  "canEditHealthData": true,
  "canViewReminders": true,
  "canEditReminders": true,
  "canApproveRequests": true,
  "createdAt": 1700000000000,
  "updatedAt": 1700000000000,
  "approvedAt": 1700000000000,
  "approvedBy": "caregiver123",
  "virtualAccountPassword": "12345678"
}

// 待审批的绑定请求
{
  "id": "caregiver456_SNR-ABC123",
  "caregiverId": "caregiver456",
  "seniorId": "SNR-ABC123",
  "status": "pending",
  "relationship": "Daughter",
  "nickname": "Mother",
  "message": "我是你的女儿，想要照顾您",
  "canViewHealthData": true,
  "canEditHealthData": false,
  "canViewReminders": true,
  "canEditReminders": false,
  "canApproveRequests": false,
  "createdAt": 1700000000000,
  "updatedAt": 1700000000000
}
```

### Indexes

```javascript
{
  "collectionGroup": "caregiver_relations",
  "fields": [
    { "fieldPath": "caregiverId", "order": "ASCENDING" },
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
},
{
  "collectionGroup": "caregiver_relations",
  "fields": [
    { "fieldPath": "seniorId", "order": "ASCENDING" },
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

## 4. health_records Collection

**用途**: 存储老人的健康记录数据

### Document Structure

```typescript
/health_records/{recordId}
{
  // 记录标识
  id: string                    // 记录ID，自动生成
  seniorId: string              // 老人 profile ID
  type: string                  // 记录类型（大写）
  
  // 血压数据 (type = "BLOOD_PRESSURE")
  systolic?: number             // 收缩压
  diastolic?: number            // 舒张压
  
  // 心率数据 (type = "HEART_RATE")
  heartRate?: number            // 心率值
  
  // 血糖数据 (type = "BLOOD_SUGAR")
  bloodSugar?: number           // 血糖值
  
  // 体重数据 (type = "WEIGHT")
  weight?: number               // 体重值
  
  // 元数据
  recordedAt: number            // 记录时间戳
  recordedBy?: string           // 记录人 UID
  source: string                // 数据来源: "manual" | "device" | "imported"
  notes?: string                // 备注信息
}
```

### Field Details

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | ✅ | 自动生成的记录ID |
| `seniorId` | string | ✅ | 老人 profile ID |
| `type` | string | ✅ | 记录类型（必须大写） |
| `systolic` | number | ⚪ | 收缩压（血压记录） |
| `diastolic` | number | ⚪ | 舒张压（血压记录） |
| `heartRate` | number | ⚪ | 心率值 |
| `bloodSugar` | number | ⚪ | 血糖值 |
| `weight` | number | ⚪ | 体重值 |
| `recordedAt` | number | ✅ | Unix 时间戳（毫秒） |
| `recordedBy` | string | ⚪ | 记录人 UID |
| `source` | string | ✅ | 数据来源 |
| `notes` | string | ⚪ | 备注信息 |

### Health Record Types

**⚠️ 重要：类型必须使用大写！**

```kotlin
BLOOD_PRESSURE    // 血压记录
HEART_RATE        // 心率记录
BLOOD_SUGAR       // 血糖记录
WEIGHT            // 体重记录
```

### Usage Examples

```kotlin
// 血压记录
{
  "id": "record_001",
  "seniorId": "SNR-ABC123",
  "type": "BLOOD_PRESSURE",
  "systolic": 120,
  "diastolic": 80,
  "recordedAt": 1700000000000,
  "recordedBy": "caregiver123",
  "source": "manual",
  "notes": "早晨测量"
}

// 心率记录
{
  "id": "record_002",
  "seniorId": "SNR-ABC123",
  "type": "HEART_RATE",
  "heartRate": 75,
  "recordedAt": 1700000000000,
  "recordedBy": "caregiver123",
  "source": "manual"
}
```

### Health Status Analysis

基于血压数据自动分析健康状态：

| Status | Condition | Display |
|--------|-----------|---------|
| URGENT | 收缩压 ≥160 或 舒张压 ≥100 | High blood pressure, needs urgent attention! |
| ATTENTION | 收缩压 ≥140 或 舒张压 ≥90 | Blood pressure elevated |
| GOOD | 其他 | All metrics normal |

### Indexes

```javascript
{
  "collectionGroup": "health_records",
  "fields": [
    { "fieldPath": "seniorId", "order": "ASCENDING" },
    { "fieldPath": "recordedAt", "order": "DESCENDING" }
  ]
},
{
  "collectionGroup": "health_records",
  "fields": [
    { "fieldPath": "seniorId", "order": "ASCENDING" },
    { "fieldPath": "type", "order": "ASCENDING" },
    { "fieldPath": "recordedAt", "order": "DESCENDING" }
  ]
}
```

---

## 5. chat_history Collection

**用途**: 存储用户与AI助手的聊天记录

### Document Structure

```typescript
/chat_history/{userId}/{messageId}
{
  id: string                    // 消息ID
  text: string                  // 消息内容
  isUser: boolean               // 是否为用户消息
  timestamp: number             // 时间戳
  userId: string                // 用户ID
}
```

### Field Details

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | ✅ | 消息唯一ID |
| `text` | string | ✅ | 消息内容 |
| `isUser` | boolean | ✅ | true=用户消息，false=AI回复 |
| `timestamp` | number | ✅ | Unix 时间戳（毫秒） |
| `userId` | string | ✅ | 发送者 UID |

---

## 6. reminders Collection

**用途**: 存储用药提醒信息

### Document Structure

```typescript
/reminders/{userId}/{reminderId}
{
  id: string                    // 提醒ID
  medicineName: string          // 药品名称
  dosage: string                // 剂量
  frequency: string             // 频率
  time: string                  // 提醒时间
  notes: string                 // 备注
  isActive: boolean             // 是否启用
  createdAt: number             // 创建时间戳
}
```

---

## Data Relationships

### Relationship Diagram

```
users (Firebase Auth UID)
  ├──> senior_profiles (creatorId)
  │      └──> caregiver_relations (seniorId)
  │      └──> health_records (seniorId)
  │
  └──> caregiver_relations (caregiverId)
         └──> senior_profiles (seniorId)
```

### Key Relationships

1. **users → senior_profiles**
   - `users.id` = `senior_profiles.creatorId` (创建关系)
   - `users.id` = `senior_profiles.userId` (绑定关系)

2. **senior_profiles ↔ caregiver_relations**
   - `senior_profiles.id` = `caregiver_relations.seniorId`
   - 多对多关系（一个老人可以有多个护理者）

3. **users ↔ caregiver_relations**
   - `users.id` = `caregiver_relations.caregiverId`

4. **senior_profiles → health_records**
   - `senior_profiles.id` = `health_records.seniorId`
   - 一对多关系

---

## Security Rules Summary

### senior_profiles
- **Create**: 已认证用户，creatorId 必须是自己
- **Read**: 所有已认证用户（应用层控制权限）
- **Update**: 老人本人或创建者
- **Delete**: 仅创建者

### caregiver_relations
- **Create**: 护理者创建 pending 请求，或创建者直接创建 active 关系
- **Read**: 关系参与者（护理者或老人）
- **Update**: 有审批权限的人
- **Delete**: 护理者本人或有审批权限的人

### health_records
- **Create**: 老人本人、创建者、或有编辑权限的护理者
- **Read**: 老人本人、创建者、或有查看权限的护理者
- **Update**: 老人本人、创建者、或有编辑权限的护理者
- **Delete**: 老人本人或创建者

---

## Migration from Old Schema

### Old Schema (seniors collection)
```javascript
seniors/{seniorId}
  ├── caregiverRelationships (nested map)
  ├── healthHistory (nested array)
  └── all data in one document
```

### New Schema (Plan C)
```javascript
senior_profiles/{profileId}        // 基本信息
caregiver_relations/{relationId}   // 关系独立
health_records/{recordId}          // 记录独立
```

### Key Improvements

1. **查询性能**: 独立集合支持复合索引和高效查询
2. **数据扩展**: 无嵌套限制，支持无限增长
3. **权限控制**: 细粒度的集合级别权限
4. **维护性**: 结构清晰，易于理解和维护

---

## Best Practices

### 1. ID生成规则

```kotlin
// senior_profiles ID
"SNR-" + RandomStringUtils.randomAlphanumeric(12).uppercase()
// 示例: SNR-ABC123XYZ456

// caregiver_relations ID
"${caregiverId}_${seniorId}"
// 示例: user123_SNR-ABC123

// health_records ID
自动生成（Firestore auto-generated）
```

### 2. 时间戳格式

统一使用 Unix 毫秒时间戳：

```kotlin
System.currentTimeMillis()  // 1700000000000
```

### 3. 类型字段规范

健康记录类型必须使用**大写**：

```kotlin
// ✅ 正确
type = "BLOOD_PRESSURE"

// ❌ 错误
type = "blood_pressure"
```

### 4. 权限默认值

创建者的关系记录默认权限：

```kotlin
canViewHealthData = true
canEditHealthData = true
canViewReminders = true
canEditReminders = true
canApproveRequests = true
```

其他护理者默认权限：

```kotlin
canViewHealthData = true
canEditHealthData = false
canViewReminders = true
canEditReminders = false
canApproveRequests = false
```

---

## Cloud Functions

### createSeniorAccount

创建老人虚拟账户，包含：
1. 创建 Firebase Auth 用户
2. 创建 users 文档
3. 更新 senior_profiles.userId

### deleteSeniorAccount

原子性删除老人所有数据：
1. 验证创建者权限
2. 检查是否有其他护理者
3. 批量删除 health_records
4. 批量删除 caregiver_relations
5. 删除 senior_profile
6. 删除 Firebase Auth 账户

### fixSeniorUserIds

一次性数据迁移函数，修复 userId 字段

---

## Version History

- **v1.0** (2025-11-26): Initial Plan C architecture
  - 扁平化独立集合设计
  - 完整的权限控制系统
  - 健康记录类型标准化
  - Relationship 下拉框和 nickname 自动映射

---

## Contact & Support

如有问题或需要进一步说明，请参考：
- `ARCHITECTURE.md` - 架构设计文档
- `firestore.rules` - 安全规则详细说明
- `firestore.indexes.json` - 索引配置
