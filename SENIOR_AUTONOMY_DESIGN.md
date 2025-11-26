# 老人账户自主权设计方案

## 🎯 设计目标
老人账户拥有**绝对自主权**，无论创建方式如何，老人都能完全控制自己的账户和数据。

## 📊 数据结构

### seniors/{seniorId}
```typescript
{
  id: string,                    // SNR-XXXXXXXXXXXX
  name: string,
  age: number,
  gender: string,
  avatarType: string,
  
  // === 创建信息 ===
  creatorId: string,             // 创建者 UID（可能是 caregiver 或老人自己）
  registrationType: 'SELF_REGISTERED' | 'CAREGIVER_CREATED',
  createdAt: number,
  
  // === 链接请求审批设置 ===
  linkRequestApprovers: string[],  // 有权审批链接请求的人员 UID 列表
                                   // CAREGIVER_CREATED: 默认 [creatorId]
                                   // SELF_REGISTERED: 默认 [seniorAuthUid]
                                   // 老人可以随时添加/移除审批人
  
  // === Caregiver 关系和权限 ===
  caregiverIds: string[],                        // 已绑定的 caregiver UIDs
  caregiverRelationships: {
    [caregiverId: string]: {
      relationship: string,        // 关系（女儿、儿子等）
      nickname: string,            // 昵称
      linkedAt: number,           // 绑定时间
      approvedBy: string,         // ⭐ 谁审批的这个绑定（UID）
      status: 'active' | 'suspended',  // 老人可以暂停某个 caregiver
      permissions: {
        canViewHealthData: boolean,      // 查看健康数据
        canViewReminders: boolean,       // 查看用药提醒
        canEditReminders: boolean,       // 编辑用药提醒
        canApproveLinkRequests: boolean  // ⭐ 审批其他人的链接请求
      }
    }
  },
  
  // === 其他 ===
  healthHistory: {...},
  password: string
}
```

### linkRequests/{requestId}
```typescript
{
  id: string,
  seniorId: string,              // 目标老人的 seniorId
  requesterId: string,           // 发起请求的 caregiver UID
  creatorId: string,             // 老人账户的创建者 UID（用于权限判断）
  
  // === 请求信息 ===
  relationship: string,
  nickname: string,
  message: string,
  status: 'pending' | 'approved' | 'rejected',
  createdAt: number,
  updatedAt: number,
  
  // === 审批记录 ===
  approvedBy?: string,           // ⭐ 实际审批人 UID
  approvedAt?: number,
  rejectedBy?: string,           // ⭐ 拒绝人 UID
  rejectedAt?: number
}
```

## 🔐 Firestore Rules 逻辑

### seniors 集合

**读取权限：**
- 老人自己：完整访问
- 已绑定的 caregiver：根据 `permissions` 决定能看到什么
- 其他已认证用户：基本信息（用于搜索和发送链接请求）

**更新权限：**
- 老人自己：完全控制（包括修改 autonomySettings、caregiverRelationships）
- 已绑定的 caregiver：仅限有权限的字段（如 reminders）

### linkRequests 集合

**读取权限：**
- `approverId` 指定的人（老人自己 或 被委托的 caregiver）
- 请求发起人（caregiver）

**更新权限：**
- `approverId` 指定的人可以审批（改 status）
- 请求发起人不能更新

## 🔄 业务流程

### 流程 1: Caregiver 创建老人账户

```
1. Caregiver 填写老人信息
   ↓
2. 调用 Cloud Function 创建 Auth + users 文档
   ↓
3. Caregiver 创建 seniors 文档：
   - registrationType: 'CAREGIVER_CREATED'
   - creatorId: caregiver.uid
   - autonomySettings.linkRequestApprover: 'DELEGATED'  ⬅️ 默认委托给创建者
   - autonomySettings.delegatedApproverId: caregiver.uid
   - caregiverIds: [caregiver.uid]
   - caregiverRelationships[caregiver.uid].permissions.canApproveLinks: true
   ↓
4. 老人登录后，可以在设置中：
   - 修改 linkRequestApprover 为 'SELF'（收回审批权）
   - 修改 permissions（限制 caregiver 权限）
   - 暂停或移除 caregiver
```

### 流程 2: 老人自主注册

```
1. 老人填写注册信息
   ↓
2. 创建 Auth + users + seniors 文档：
   - registrationType: 'SELF_REGISTERED'
   - creatorId: senior.uid
   - autonomySettings.linkRequestApprover: 'SELF'  ⬅️ 默认自己审批
   - caregiverIds: []
   ↓
3. 老人可以选择：
   - 保持 'SELF'（自己审批所有链接请求）
   - 委托给某个 caregiver（修改为 'DELEGATED'）
```

### 流程 3: Caregiver 发送链接请求

```
1. Caregiver 搜索老人（通过 SNR-ID）
   ↓
2. Caregiver 发送链接请求
   ↓
3. 系统自动设置 linkRequest.approverId：
   - 读取 senior.autonomySettings.linkRequestApprover
   - 如果是 'SELF' → approverId = senior 的 auth.uid
   - 如果是 'DELEGATED' → approverId = delegatedApproverId
   ↓
4. 审批人收到通知，可以批准/拒绝
```

## 🎨 UI 设计

### 老人端设置页面

**自主权设置 (Autonomy Settings)**
```
┌─────────────────────────────────────┐
│ 链接请求审批                          │
│ ○ 由我自己审批                        │
│ ● 委托给护理者审批                    │
│   └─ 当前委托人: 张三 (女儿)          │
│   [更改委托人]                        │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ 已绑定的护理者                        │
│                                      │
│ 👤 张三 (女儿)                        │
│    ✓ 查看健康数据                    │
│    ✓ 查看提醒                        │
│    ✓ 编辑提醒                        │
│    ✓ 审批链接请求                    │
│    [编辑权限] [暂停] [移除]          │
│                                      │
│ 👤 李四 (儿子)                        │
│    ✓ 查看健康数据                    │
│    ✗ 查看提醒                        │
│    ✗ 编辑提醒                        │
│    ✗ 审批链接请求                    │
│    [编辑权限] [暂停] [移除]          │
└─────────────────────────────────────┘
```

### Link Guard 页面

**根据 autonomySettings 动态显示：**

- **模式 1: 老人自己审批** → Link Guard 在老人端 Profile
- **模式 2: 委托给 Caregiver** → Link Guard 在 Caregiver 端 Profile
  - 老人可以随时在设置中收回权限

## 🔒 安全考虑

1. **防止权限滥用**
   - Caregiver 不能修改自己的 permissions
   - 只有老人可以修改 autonomySettings
   - 老人可以随时移除任何 caregiver（包括创建者）

2. **审计日志**
   - 记录所有权限变更
   - 记录所有审批操作
   - 记录 caregiver 的数据访问

3. **紧急联系人**
   - 设置紧急联系人（即使被暂停也能查看关键数据）
   - 避免老人误操作后无人能访问

## 📝 实现优先级

**Phase 1 (当前):**
- ✅ 基础的两种创建模式
- ✅ 简单的审批权限（固定为创建者或老人）
- ✅ Link Guard 基础功能

**Phase 2 (下一步):**
- 🔲 autonomySettings 数据结构
- 🔲 委托审批人功能
- 🔲 细粒度权限控制（permissions）

**Phase 3 (未来):**
- 🔲 数据共享范围控制
- 🔲 审计日志
- 🔲 紧急联系人机制
