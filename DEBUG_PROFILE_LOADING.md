# Profile 页面 Loading 问题排查指南

## 🐛 问题描述
Profile 页面一直显示 loading，血压和心率不显示

## 🔍 排查步骤

### 1. 查看 Logcat 日志

运行应用后，在 Android Studio Logcat 中过滤 `ProfileViewModel`，查看以下关键日志：

```
# 期望看到的日志流程：
✅ ProfileViewModel: Cached user: id=SNR-XXXXX, name=张三, role=senior
✅ ProfileViewModel: Loading profile for senior: SNR-XXXXX
✅ ProfileViewModel: Senior data loaded: name=张三, age=75, gender=Male, avatarType=ELDERLY_MALE
📅 ProfileViewModel: Days used: 12 (created: 1732435200000, now: 1732435200000)
👤 ProfileViewModel: Avatar emoji: 👴 (type: ELDERLY_MALE)
🔍 ProfileViewModel: Fetching latest health data...
📊 ProfileViewModel: Health history size: 5
✅ ProfileViewModel: Latest health data: BP=128/82, HR=78
✅ ProfileViewModel: Profile loaded successfully
```

### 2. 可能的错误情况

#### 错误 1：没有 senior ID
```
❌ ProfileViewModel: No senior ID found in local cache
```
**原因**：用户未登录或本地缓存被清除
**解决**：重新登录老人账户

#### 错误 2：无法加载 Senior 数据
```
❌ ProfileViewModel: Failed to load senior data: PERMISSION_DENIED
```
**原因**：
- Firestore 权限配置问题
- senior ID 不存在
- 网络连接问题

**检查**：
1. 确认 Firestore Rules 是否允许老人读取自己的数据
2. 在 Firebase Console 检查 `seniors` 集合中是否存在该 senior ID
3. 检查网络连接

#### 错误 3：没有健康数据
```
⚠️ ProfileViewModel: No health data found in history
```
**原因**：该老人还没有上报过健康数据

**结果**：显示 "--/--" 和 "No Data"（这是正常的）

### 3. 检查数据结构

#### LocalDataSource 缓存数据
在登录成功后，应该保存了：
```kotlin
Triple(seniorId, userName, "senior")
// 例如：("SNR-ABCD1234", "张三", "senior")
```

#### Firestore seniors 集合
```
seniors/{seniorId}/
  - id: "SNR-ABCD1234"
  - name: "张三"
  - age: 75
  - gender: "Male"
  - avatarType: "ELDERLY_MALE"
  - createdAt: 1732435200000
  - caregiverIds: ["uid1", "uid2"]
  - ...
```

#### Firestore health_data 集合
```
health_data/{seniorAuthUid}/records/
  - {auto-id}/
    - systolic: 128
    - diastolic: 82
    - heartRate: 78
    - timestamp: 1732435200000
```

## 🔧 修复方案

### 方案 1：重新登录
1. 退出当前账户
2. 重新登录老人账户
3. 检查 Logcat 确认 senior ID 被正确缓存

### 方案 2：添加测试数据
如果没有健康数据，去 Health Report 页面上报一条数据：
1. 进入 "Health Report" (健康上报)
2. 输入血压和心率
3. 点击保存
4. 返回 Profile 查看是否显示

### 方案 3：检查 Firestore 权限
确认 `firestore.rules` 中有以下规则：

```javascript
// 老人可以读取自己的数据
match /seniors/{seniorId} {
  allow read: if seniorId == request.auth.token.seniorId 
              || request.auth.uid in resource.data.caregiverIds;
}

// 老人可以读取自己的健康数据
match /health_data/{uid}/records/{recordId} {
  allow read: if request.auth.uid == uid;
}
```

## 📝 数据流程

```
用户登录
  ↓
LocalDataSource.saveUser(seniorId, userName, "senior")
  ↓
ProfileViewModel.loadProfileData()
  ↓
1. LocalDataSource.getUser() → 获取 seniorId
  ↓
2. SeniorRepository.getSeniorById(seniorId) → 获取 Senior 数据
  ↓
3. 计算使用天数 (当前时间 - createdAt)
  ↓
4. AvatarHelper.getAvatarEmoji(avatarType) → 获取头像 emoji
  ↓
5. HealthRepository.getHealthHistory() → 获取健康数据列表
  ↓
6. healthHistoryList.firstOrNull() → 获取最新一条
  ↓
7. 更新 UI State
  ↓
UI 显示数据
```

## 🧪 测试检查清单

- [ ] 用户已登录（LocalDataSource 有缓存）
- [ ] senior ID 格式正确（SNR-XXXXXXXX）
- [ ] Firestore seniors 集合中存在该 senior
- [ ] avatarType 字段不为空
- [ ] health_data 集合中至少有一条记录
- [ ] 网络连接正常
- [ ] Firestore 权限配置正确

## 💡 常见问题

### Q: 为什么改用 `getHealthHistory()` 而不是 `getLatestHealthData()`？
A: `getHealthHistory()` 返回完整的健康数据列表，我们取第一条（最新的），这样更灵活且与 HealthHistoryScreen 数据源一致。

### Q: 如果一直 loading 怎么办？
A: 
1. 查看 Logcat 日志找到错误点
2. 检查网络连接
3. 确认 Firestore Rules
4. 尝试重新登录

### Q: 显示 "No Data" 正常吗？
A: 正常！如果老人还没上报过健康数据，就会显示 "--/--" 和 "No Data"。去 Health Report 上报一条数据即可。

## 📊 调试命令

在 Android Studio 的 Logcat 中使用以下过滤器：

```
tag:ProfileViewModel
```

或者更详细的：

```
package:com.alvin.pulselink ProfileViewModel
```
