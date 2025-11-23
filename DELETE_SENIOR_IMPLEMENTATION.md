# 删除老人账户功能实现

## 概述
实现了删除老人账户时同时删除 Firebase Authentication 账户和 Firestore 数据的功能。

## 修改的文件

### 1. Cloud Functions (`functions/src/index.ts`)
添加了新的 Cloud Function `deleteSeniorAccount`：

```typescript
export const deleteSeniorAccount = onCall(...)
```

**功能**：
- 接收参数：`seniorId`（老人的虚拟ID，如 SNR-ABCD1234）
- 根据 seniorId 生成邮箱 `senior_${seniorId}@pulselink.app`
- 查找对应的 Firebase Auth UID
- 删除 Firestore `users` 集合中的用户文档
- 删除 Firebase Authentication 账户
- 返回删除结果

**安全性**：
- 需要用户已登录（只有 caregiver 可以调用）
- 即使 Auth 用户不存在也不会报错，会继续执行

### 2. SeniorRepositoryImpl (`app/src/main/java/.../data/repository/SeniorRepositoryImpl.kt`)
修改了 `deleteSenior` 方法：

**之前**：
```kotlin
override suspend fun deleteSenior(seniorId: String): Result<Unit> {
    return try {
        seniorsCollection.document(seniorId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**现在**：
```kotlin
override suspend fun deleteSenior(seniorId: String): Result<Unit> {
    return try {
        // Step 1: 调用 Cloud Function 删除 Firebase Auth 和 users 文档
        val data = hashMapOf("seniorId" to seniorId)
        functions.getHttpsCallable("deleteSeniorAccount").call(data).await()
        
        // Step 2: 删除 Firestore seniors 集合中的文档
        seniorsCollection.document(seniorId).delete().await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**新增依赖**：
- 添加了 `FirebaseFunctions` 参数注入

### 3. AppModule (`app/src/main/java/.../di/AppModule.kt`)
修改了 `provideSeniorRepository` 方法，添加 `FirebaseFunctions` 依赖：

```kotlin
@Provides
@Singleton
fun provideSeniorRepository(
    firestore: FirebaseFirestore,
    functions: FirebaseFunctions  // 新增
): SeniorRepository {
    return SeniorRepositoryImpl(firestore, functions)  // 传入 functions
}
```

## 删除流程

当 caregiver 删除老人账户时：

```
1. ManageSeniorsViewModel.deleteSenior(seniorId)
   ↓
2. SeniorRepository.deleteSenior(seniorId)
   ↓
3. 调用 Cloud Function: deleteSeniorAccount
   ├─ 删除 Firebase Auth 账户 (senior_SNR-XXXX@pulselink.app)
   └─ 删除 Firestore users/{uid} 文档
   ↓
4. 删除 Firestore seniors/{seniorId} 文档
   ↓
5. 完成
```

## 部署 Cloud Functions

**重要**：修改了 Cloud Functions 后需要重新部署！

### 方法 1：部署所有函数
```bash
cd functions
npm run deploy
```

### 方法 2：只部署删除函数
```bash
firebase deploy --only functions:deleteSeniorAccount
```

### 验证部署
部署后，在 Firebase Console 中检查：
1. 打开 Firebase Console
2. 进入 Functions 页面
3. 确认 `deleteSeniorAccount` 函数已部署

## 测试建议

1. **创建测试老人账户**
2. **验证 Firebase Auth 中存在该账户**
   - Firebase Console > Authentication > Users
   - 查找 `senior_SNR-XXXX@pulselink.app`
3. **删除老人账户**
4. **验证删除成功**：
   - Firebase Auth 中账户已删除
   - Firestore `users` 集合中文档已删除
   - Firestore `seniors` 集合中文档已删除

## 错误处理

- 如果 Cloud Function 调用失败，整个删除操作会回滚
- 如果 Auth 用户不存在，Cloud Function 会记录日志但继续执行
- 所有错误都会通过 `Result.failure` 返回给 UI 层

## 注意事项

1. **Cloud Functions 必须部署**：否则删除操作会失败
2. **需要网络连接**：删除操作需要调用 Cloud Function
3. **权限检查**：Cloud Function 会验证调用者已登录
4. **不可恢复**：删除操作是永久性的，无法撤销

## 编译状态
✅ Android 项目编译成功
✅ TypeScript Cloud Functions 编译成功
⚠️ 需要部署 Cloud Functions 才能使用删除功能
