# 多护理者关系映射实现说明

## 📊 新数据结构

### Senior 模型更新

```kotlin
data class Senior(
    val id: String = "",
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val avatarType: String = "",
    val healthHistory: HealthHistory = HealthHistory(),
    
    // ✅ 新增：护理者关系映射
    val caregiverRelationships: Map<String, CaregiverRelationship> = emptyMap(),
    
    val caregiverIds: List<String> = emptyList(),
    val creatorId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val password: String = ""
)

data class CaregiverRelationship(
    val relationship: String = "",  // "Son", "Daughter", etc.
    val nickname: String = "",      // "Dad", "Mom", etc.
    val linkedAt: Long = System.currentTimeMillis(),
    val status: String = "active"   // "pending", "active", "rejected"
)
```

## 🎯 使用场景示例

### 场景 1：儿子创建老人账户

```kotlin
val caregiverId1 = "user123"
val senior = Senior(
    id = "SNR-ABC12345",
    name = "Zhang Wei",
    age = 68,
    gender = "Male",
    creatorId = caregiverId1,
    caregiverIds = listOf(caregiverId1),
    caregiverRelationships = mapOf(
        caregiverId1 to CaregiverRelationship(
            relationship = "Son",
            nickname = "Dad",
            linkedAt = System.currentTimeMillis(),
            status = "active"
        )
    )
)
```

**结果**：
- 儿子在主页看到："Dad - Zhang Wei"
- 老人端看到："Created by your son"

---

### 场景 2：女儿链接同一个老人

```kotlin
val caregiverId2 = "user456"

// 更新 Senior
val updatedSenior = senior.copy(
    caregiverIds = senior.caregiverIds + caregiverId2,
    caregiverRelationships = senior.caregiverRelationships + mapOf(
        caregiverId2 to CaregiverRelationship(
            relationship = "Daughter",
            nickname = "Mom",
            linkedAt = System.currentTimeMillis(),
            status = "active"
        )
    )
)
```

**结果**：
- 儿子看到："Dad - Zhang Wei"
- 女儿看到："Mom - Zhang Wei"
- 同一个老人，不同称呼！✅

---

### 场景 3：孙子链接，使用默认称呼

```kotlin
val caregiverId3 = "user789"

val updatedSenior = senior.copy(
    caregiverIds = senior.caregiverIds + caregiverId3,
    caregiverRelationships = senior.caregiverRelationships + mapOf(
        caregiverId3 to CaregiverRelationship(
            relationship = "Grandson",
            nickname = "",  // 留空，使用默认称呼
            linkedAt = System.currentTimeMillis(),
            status = "active"
        )
    )
)
```

**结果**：
- 孙子看到："Grandfather - Zhang Wei"（自动推断）

---

## 🔧 扩展函数使用

### 获取当前用户的显示名称

```kotlin
// 在 UI 中
val displayName = senior.getDisplayNameFor(currentUserId)
// 返回：nickname（如果有）或默认称呼
```

### 获取关系信息

```kotlin
val relationship = senior.getRelationshipFor(currentUserId)
// 返回：CaregiverRelationship? 对象

val relationshipString = senior.getRelationshipStringFor(currentUserId)
// 返回："Son", "Daughter" 等

val nickname = senior.getNicknameFor(currentUserId)
// 返回：自定义昵称
```

### 检查关系状态

```kotlin
if (senior.hasActiveRelationship(currentUserId)) {
    // 用户已激活关系
}

if (senior.hasPendingRelationship(currentUserId)) {
    // 用户关系待审核
}
```

---

## 📝 Firestore 数据结构示例

```json
{
  "id": "SNR-ABC12345",
  "name": "Zhang Wei",
  "age": 68,
  "gender": "Male",
  "creatorId": "user123",
  "caregiverIds": ["user123", "user456", "user789"],
  "caregiverRelationships": {
    "user123": {
      "relationship": "Son",
      "nickname": "Dad",
      "linkedAt": 1700000000000,
      "status": "active"
    },
    "user456": {
      "relationship": "Daughter",
      "nickname": "Mom",
      "linkedAt": 1700001000000,
      "status": "active"
    },
    "user789": {
      "relationship": "Grandson",
      "nickname": "",
      "linkedAt": 1700002000000,
      "status": "active"
    }
  }
}
```

---

## ✅ 实现的优势

1. **多用户支持**：每个护理者可以有自己的关系和称呼
2. **灵活性**：支持自定义昵称或使用默认称呼
3. **可扩展性**：future 可以添加权限、审核状态等
4. **数据一致性**：所有关系信息集中在一个文档中
5. **向后兼容**：现有代码最小化改动

---

## 🚀 未来扩展可能性

1. **审核机制**：status = "pending" 等待创建者审核
2. **权限管理**：不同关系不同权限
3. **通知系统**：新的绑定请求通知
4. **关系历史**：记录关系变更历史
5. **多语言支持**：称呼的多语言版本
