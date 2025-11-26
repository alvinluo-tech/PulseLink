import { onCall, HttpsError } from "firebase-functions/v2/https";
import { GoogleGenerativeAI } from "@google/generative-ai";
import { defineSecret } from "firebase-functions/params";
import * as admin from "firebase-admin";

// 初始化 Firebase Admin SDK
admin.initializeApp();

// 定义密钥
const googleApiKey = defineSecret("GOOGLE_API_KEY");

// 导出音频转录函数
export { transcribeAudio } from "./transcribe";

// 语音转文字 Cloud Function
export const voiceToText = onCall(
    {
        secrets: [googleApiKey],
        timeoutSeconds: 60,
        memory: "512MiB"
    },
    async (request) => {
        // 1. 鉴权
        if (!request.auth) {
            throw new HttpsError("unauthenticated", "请先登录");
        }

        // 2. 获取音频数据 (Base64)
        const base64Audio = request.data.audio;
        if (!base64Audio) {
            throw new HttpsError("invalid-argument", "无音频数据");
        }

        try {
            const genAI = new GoogleGenerativeAI(googleApiKey.value());
            
            // 使用 Gemini 2.0 Flash 模型
            const model = genAI.getGenerativeModel({ 
                model: "gemini-2.0-flash-exp" 
            });

            // "听写员" 提示词
            const prompt = `
                Please transcribe this audio verbatim.
                Requirements:
                1. Only output the recognized text content.
                2. Do not answer questions in the audio.
                3. Do not add any explanatory text except punctuation.
                4. If the audio contains Chinese, use Simplified Chinese.
                5. If the audio contains English, use English.
            `;

            // 3. 发送请求
            const result = await model.generateContent([
                { text: prompt },
                {
                    inlineData: {
                        mimeType: "audio/mp4", // Android 录制的 m4a 对应 audio/mp4
                        data: base64Audio
                    }
                }
            ]);

            const transcription = result.response.text();
            
            console.log("识别结果:", transcription);

            return { 
                success: true, 
                text: transcription.trim() 
            };

        } catch (error: any) {
            console.error("Gemini STT Error:", error);
            throw new HttpsError("internal", "语音识别服务暂时不可用");
        }
    }
);

// 定义云函数 (v2 写法)
export const chatWithAI = onCall(
    {
        secrets: [googleApiKey],
        timeoutSeconds: 60,
        memory: "256MiB"
    },
    async (request) => {
        // --- 鉴权检查 ---
        if (!request.auth) {
            throw new HttpsError("unauthenticated", "请先登录");
        }

        // --- 获取数据 ---
        const userMessage = request.data.text;
        const healthData = request.data.healthData || "无最新数据";

        try {
            // 初始化 Gemini 客户端
            const genAI = new GoogleGenerativeAI(googleApiKey.value());

            // 获取模型 
            const model = genAI.getGenerativeModel({ 
                model: "gemini-2.0-flash",
                // 系统人设在这里配置
                systemInstruction: `
# Role
You are "Alex", a caring health assistant for seniors over 65. Your personality is warm, patient, and respectful, like a loving grandchild.

# Context
The user (a senior) will tell you their blood pressure readings or ask about their health.
Here is the recent health record:
${healthData}

# Constraints & Tone
1. **Minimalism**: Keep responses to 3-4 sentences max.
2. **Natural Care**: Be warm and respectful. Use "Mr./Ms." or just "you" warmly. **Avoid dramatic exclamations**.
3. **Simple Terms**: Use "Top number / Bottom number", avoid "Systolic/Diastolic".
4. **Direct Answers**: Numbers First; Focus on Now.
5. **Medication Inquiry (Interactive)**: 
   - **Step 1 (Clarify Name)**: If the user asks "How do I take this medicine?" **without naming it**, ask gently: "Which medicine are you referring to? Tell me the name and I'll check the reminder for you."
   - **Step 2 (Check Record)**: Once the medicine name is known:
     - **Case A (Has Plan)**: If in \`medication_plan\`, **Start with**: "**According to the reminder set by your family**", then state usage. **End with**: "However, please always verify with your doctor's instructions."
     - **Case B (No Plan)**: If NOT in plan, reply ONLY: "Your family hasn't set a reminder for this medicine, so I don't have the record. Please strictly follow your doctor's instructions or check the label."
6. **Fallback (Gibberish Only)**: 
   - Reply: "I'm sorry, I didn't quite catch that. Could you say it again?"

# Safety
At the very end, add a tiny note: *AI advice for reference only. Please consult a doctor if unwell.*
`

            });

            // 发送消息
            const result = await model.generateContent(userMessage);
            const response = result.response;
            const aiReply = response.text();

            // 返回给 Android
            return {
                success: true,
                reply: aiReply
            };

        } catch (error: any) {
            console.error("Gemini Error:", error);
            throw new HttpsError("internal", "AI 服务暂时不可用");
        }
    }
);

/**
 * 创建老人账户的 Cloud Function
 * 
 * 调用参数:
 * - seniorId: 老人的虚拟ID (例如: SNR-ABCD1234)
 * - name: 老人姓名
 * - password: 可选，如果不提供则自动生成 8 位随机密码
 * 
 * 返回:
 * - success: boolean
 * - email: 生成的邮箱 (senior_${seniorId}@pulselink.app)
 * - password: 密码（用于生成二维码）
 * - uid: Firebase Auth UID
 */
export const createSeniorAccount = onCall(
    {
        timeoutSeconds: 30,
        memory: "256MiB"
    },
    async (request) => {
        // --- 鉴权检查：只有已登录的 caregiver 可以创建 ---
        if (!request.auth) {
            throw new HttpsError("unauthenticated", "请先登录");
        }

        // --- 获取参数 ---
        const { seniorId, name, password } = request.data;

        // --- 参数验证 ---
        if (!seniorId || typeof seniorId !== "string") {
            throw new HttpsError("invalid-argument", "seniorId 是必需的");
        }

        if (!name || typeof name !== "string") {
            throw new HttpsError("invalid-argument", "name 是必需的");
        }

        // 验证 seniorId 格式 (SNR-XXXXXXXXXXXX)
        if (!/^SNR-[A-Z0-9]{12}$/.test(seniorId)) {
            throw new HttpsError(
                "invalid-argument", 
                "seniorId 格式不正确，应为 SNR-XXXXXXXXXXXX"
            );
        }

        try {
            // --- 生成邮箱和密码 ---
            const email = `senior_${seniorId}@pulselink.app`;
            const finalPassword = password || generateRandomPassword();

            console.log(`Creating senior account: ${email}`);

            // --- 在 Firebase Auth 中创建用户 ---
            const userRecord = await admin.auth().createUser({
                email: email,
                password: finalPassword,
                displayName: `${name}|SENIOR`, // 格式: "姓名|角色"
                emailVerified: true // 老人账户默认验证邮箱
            });

            console.log(`Senior account created: ${userRecord.uid}`);

            // --- 在 Firestore 中创建用户文档 ---
            await admin.firestore().collection("users").doc(userRecord.uid).set({
                uid: userRecord.uid,
                email: email,
                username: name,
                role: "SENIOR",
                seniorId: seniorId, // 存储虚拟 ID 以便查询
                createdAt: Date.now(), // 使用时间戳
                createdBy: request.auth.uid, // 记录创建者（caregiver）
                emailVerified: true
            });

            console.log(`Firestore user document created for: ${userRecord.uid}`);

            // --- 返回结果 ---
            return {
                success: true,
                email: email,
                password: finalPassword,
                uid: userRecord.uid,
                seniorId: seniorId
            };

        } catch (error: any) {
            console.error("Create senior account error:", error);
            
            // 如果邮箱已存在
            if (error.code === "auth/email-already-exists") {
                throw new HttpsError(
                    "already-exists", 
                    "该老人账户已存在"
                );
            }

            // 其他错误
            throw new HttpsError(
                "internal", 
                `创建账户失败: ${error.message}`
            );
        }
    }
);

/**
 * 删除老人账户的 Cloud Function
 * 
 * 调用参数:
 * - seniorId: 老人的虚拟ID (例如: SNR-ABCD1234)
 * 
 * 返回:
 * - success: boolean
 * - message: 删除结果消息
 */
export const deleteSeniorAccount = onCall(
    {
        timeoutSeconds: 30,
        memory: "256MiB"
    },
    async (request) => {
        // --- 鉴权检查：只有已登录的 caregiver 可以删除 ---
        if (!request.auth) {
            throw new HttpsError("unauthenticated", "请先登录");
        }

        // --- 获取参数 ---
        const { seniorId } = request.data;

        // --- 参数验证 ---
        if (!seniorId || typeof seniorId !== "string") {
            throw new HttpsError("invalid-argument", "seniorId 是必需的");
        }

        try {
            console.log(`Deleting senior account: ${seniorId}`);

            // --- 1. 根据 seniorId 查找对应的 Firebase Auth UID ---
            const email = `senior_${seniorId}@pulselink.app`;
            
            let userRecord;
            try {
                userRecord = await admin.auth().getUserByEmail(email);
            } catch (error: any) {
                if (error.code === "auth/user-not-found") {
                    console.log(`Firebase Auth user not found for ${seniorId}, continuing with Firestore deletion`);
                    // 即使 Auth 用户不存在，也继续删除 Firestore 数据
                    return {
                        success: true,
                        message: "Senior account deleted (Auth user not found)",
                        deletedAuth: false,
                        deletedFirestore: false
                    };
                }
                throw error;
            }

            const uid = userRecord.uid;
            console.log(`Found UID: ${uid} for seniorId: ${seniorId}`);

            // --- 2. 删除 Firestore 中的 user 文档 ---
            await admin.firestore().collection("users").doc(uid).delete();
            console.log(`Deleted Firestore user document: ${uid}`);

            // --- 3. 删除 Firebase Auth 账户 ---
            await admin.auth().deleteUser(uid);
            console.log(`Deleted Firebase Auth user: ${uid}`);

            // --- 返回结果 ---
            return {
                success: true,
                message: "Senior account deleted successfully",
                deletedAuth: true,
                deletedFirestore: true,
                uid: uid
            };

        } catch (error: any) {
            console.error("Delete senior account error:", error);
            
            throw new HttpsError(
                "internal", 
                `删除账户失败: ${error.message}`
            );
        }
    }
);

/**
 * 生成 8 位随机密码
 */
function generateRandomPassword(): string {
    const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    let password = "";
    for (let i = 0; i < 8; i++) {
        password += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return password;
}

// ============================================================================
// 数据迁移函数 (Plan C 架构重构)
// ============================================================================

/**
 * 迁移单个老人数据到新架构
 * 
 * 旧架构 (seniors collection):
 * - 所有数据嵌入在一个文档中
 * - caregiverRelationships 是一个 Map
 * - healthHistory 是嵌套数组
 * 
 * 新架构:
 * - senior_profiles: 简化的老人资料
 * - caregiver_relations: 独立的关系管理
 * - health_records: 独立的健康记录
 * - senior_passwords: 独立的密码存储
 * 
 * 调用参数:
 * - seniorId: 要迁移的老人 ID (可选，不提供则迁移所有)
 * - dryRun: 是否只预览不执行 (默认 true)
 * 
 * 返回:
 * - success: boolean
 * - migratedCount: 迁移数量
 * - details: 详细迁移信息
 */
export const migrateToNewArchitecture = onCall(
    {
        timeoutSeconds: 540, // 9 分钟，大量数据迁移需要较长时间
        memory: "1GiB"
    },
    async (request) => {
        // --- 鉴权检查：需要管理员权限 ---
        if (!request.auth) {
            throw new HttpsError("unauthenticated", "请先登录");
        }

        // 检查是否为管理员 (可以根据需要调整权限检查逻辑)
        // 这里简单检查是否为特定的管理员 UID，生产环境应该使用 Custom Claims
        const adminUids = ["YOUR_ADMIN_UID"]; // TODO: 替换为实际管理员 UID
        // 暂时允许所有已登录用户执行迁移（调试用）
        // if (!adminUids.includes(request.auth.uid)) {
        //     throw new HttpsError("permission-denied", "需要管理员权限");
        // }

        const { seniorId, dryRun = true } = request.data;
        const db = admin.firestore();
        const results: MigrationResult[] = [];

        try {
            console.log(`Starting migration (dryRun: ${dryRun})`);

            // 获取要迁移的老人文档
            let seniorsQuery;
            if (seniorId) {
                // 迁移单个老人
                seniorsQuery = db.collection("seniors").where("id", "==", seniorId);
            } else {
                // 迁移所有老人
                seniorsQuery = db.collection("seniors");
            }

            const seniorsSnapshot = await seniorsQuery.get();
            console.log(`Found ${seniorsSnapshot.size} seniors to migrate`);

            for (const seniorDoc of seniorsSnapshot.docs) {
                const oldData = seniorDoc.data();
                const result = await migrateSingleSenior(db, oldData, dryRun);
                results.push(result);
            }

            const successCount = results.filter(r => r.success).length;
            const failedCount = results.filter(r => !r.success).length;

            console.log(`Migration complete: ${successCount} success, ${failedCount} failed`);

            return {
                success: true,
                dryRun: dryRun,
                totalCount: seniorsSnapshot.size,
                successCount: successCount,
                failedCount: failedCount,
                results: results
            };

        } catch (error: any) {
            console.error("Migration error:", error);
            throw new HttpsError("internal", `迁移失败: ${error.message}`);
        }
    }
);

interface MigrationResult {
    seniorId: string;
    success: boolean;
    profileCreated: boolean;
    relationsCreated: number;
    healthRecordsCreated: number;
    passwordMigrated: boolean;
    error?: string;
}

/**
 * 迁移单个老人的数据
 */
async function migrateSingleSenior(
    db: admin.firestore.Firestore,
    oldData: admin.firestore.DocumentData,
    dryRun: boolean
): Promise<MigrationResult> {
    const seniorId = oldData.id;
    const result: MigrationResult = {
        seniorId: seniorId,
        success: false,
        profileCreated: false,
        relationsCreated: 0,
        healthRecordsCreated: 0,
        passwordMigrated: false
    };

    try {
        console.log(`Migrating senior: ${seniorId}`);

        // 检查是否已经迁移过
        const existingProfile = await db.collection("senior_profiles").doc(seniorId).get();
        if (existingProfile.exists) {
            console.log(`Senior ${seniorId} already migrated, skipping`);
            result.success = true;
            result.error = "Already migrated";
            return result;
        }

        // 1. 创建 senior_profile
        const profileData = {
            id: seniorId,
            userId: oldData.userId || null, // 可能为空（未绑定 Auth）
            name: oldData.name || "未命名",
            age: oldData.age || 0,
            gender: oldData.gender || "未知",
            avatarType: oldData.avatarType || "GRANDFATHER",
            creatorId: oldData.createdBy || oldData.primaryCaregiverId || "",
            createdAt: oldData.createdAt || Date.now(),
            registrationType: oldData.userId ? "SELF_REGISTERED" : "CAREGIVER_CREATED"
        };

        if (!dryRun) {
            await db.collection("senior_profiles").doc(seniorId).set(profileData);
        }
        result.profileCreated = true;
        console.log(`  - Profile created: ${seniorId}`);

        // 2. 迁移密码到 senior_passwords
        if (oldData.passwordHash) {
            const passwordData = {
                seniorProfileId: seniorId,
                passwordHash: oldData.passwordHash,
                salt: oldData.salt || "",
                createdAt: oldData.createdAt || Date.now(),
                updatedAt: Date.now()
            };

            if (!dryRun) {
                await db.collection("senior_passwords").doc(seniorId).set(passwordData);
            }
            result.passwordMigrated = true;
            console.log(`  - Password migrated`);
        }

        // 3. 迁移 caregiverRelationships 到 caregiver_relations
        const relationships = oldData.caregiverRelationships || {};
        for (const [caregiverId, relationData] of Object.entries(relationships)) {
            const relation = relationData as any;
            const relationId = `${caregiverId}_${seniorId}`;

            // 解析旧的权限数据（可能是嵌套对象或简单布尔值）
            const permissions = relation.permissions || {};
            const canViewHealthData = permissions.canViewHealthData ?? permissions.viewHealthData ?? true;
            const canEditHealthData = permissions.canEditHealthData ?? permissions.editHealthData ?? false;
            const canViewReminders = permissions.canViewReminders ?? permissions.viewReminders ?? true;
            const canEditReminders = permissions.canEditReminders ?? permissions.editReminders ?? false;
            const canApproveRequests = permissions.canApproveRequests ?? permissions.approveRequests ?? false;

            const relationDocData = {
                id: relationId,
                caregiverId: caregiverId,
                seniorProfileId: seniorId,
                status: relation.status || "approved",
                role: relation.role || "CAREGIVER",
                // 扁平化权限
                canViewHealthData: canViewHealthData,
                canEditHealthData: canEditHealthData,
                canViewReminders: canViewReminders,
                canEditReminders: canEditReminders,
                canApproveRequests: canApproveRequests,
                createdAt: relation.createdAt || Date.now(),
                updatedAt: relation.updatedAt || Date.now(),
                approvedAt: relation.approvedAt || null,
                approvedBy: relation.approvedBy || null
            };

            if (!dryRun) {
                await db.collection("caregiver_relations").doc(relationId).set(relationDocData);
            }
            result.relationsCreated++;
        }
        console.log(`  - ${result.relationsCreated} relations created`);

        // 4. 迁移 healthHistory 到 health_records
        const healthHistory = oldData.healthHistory || [];
        for (const healthEntry of healthHistory) {
            const recordId = `${seniorId}_${healthEntry.recordedAt || Date.now()}`;
            
            // 确定健康记录类型
            let recordType = "blood_pressure"; // 默认
            if (healthEntry.type) {
                recordType = healthEntry.type;
            } else if (healthEntry.heartRate && !healthEntry.systolic) {
                recordType = "heart_rate";
            } else if (healthEntry.bloodSugar) {
                recordType = "blood_sugar";
            } else if (healthEntry.weight) {
                recordType = "weight";
            }

            const recordData = {
                id: recordId,
                seniorProfileId: seniorId,
                type: recordType,
                // 血压数据
                systolic: healthEntry.systolic || null,
                diastolic: healthEntry.diastolic || null,
                // 心率数据
                heartRate: healthEntry.heartRate || null,
                // 血糖数据
                bloodSugar: healthEntry.bloodSugar || null,
                // 体重数据
                weight: healthEntry.weight || null,
                // 元数据
                recordedAt: healthEntry.recordedAt || Date.now(),
                recordedBy: healthEntry.recordedBy || null,
                source: healthEntry.source || "manual",
                notes: healthEntry.notes || null
            };

            if (!dryRun) {
                await db.collection("health_records").doc(recordId).set(recordData);
            }
            result.healthRecordsCreated++;
        }
        console.log(`  - ${result.healthRecordsCreated} health records created`);

        result.success = true;
        return result;

    } catch (error: any) {
        console.error(`Error migrating senior ${seniorId}:`, error);
        result.error = error.message;
        return result;
    }
}

/**
 * 回滚迁移（删除新架构数据）
 * 
 * ⚠️ 危险操作！仅用于测试环境
 * 
 * 调用参数:
 * - seniorId: 要回滚的老人 ID (可选，不提供则回滚所有)
 * - confirm: 必须为 "I_UNDERSTAND_THIS_WILL_DELETE_DATA"
 */
export const rollbackMigration = onCall(
    {
        timeoutSeconds: 300,
        memory: "512MiB"
    },
    async (request) => {
        if (!request.auth) {
            throw new HttpsError("unauthenticated", "请先登录");
        }

        const { seniorId, confirm } = request.data;

        // 安全确认
        if (confirm !== "I_UNDERSTAND_THIS_WILL_DELETE_DATA") {
            throw new HttpsError(
                "invalid-argument", 
                "请确认操作：设置 confirm 为 'I_UNDERSTAND_THIS_WILL_DELETE_DATA'"
            );
        }

        const db = admin.firestore();
        let deletedProfiles = 0;
        let deletedRelations = 0;
        let deletedHealthRecords = 0;
        let deletedPasswords = 0;

        try {
            console.log(`Starting rollback for ${seniorId || "all seniors"}`);

            if (seniorId) {
                // 回滚单个老人
                // 删除 profile
                await db.collection("senior_profiles").doc(seniorId).delete();
                deletedProfiles++;

                // 删除 password
                await db.collection("senior_passwords").doc(seniorId).delete();
                deletedPasswords++;

                // 删除 relations
                const relationsQuery = await db.collection("caregiver_relations")
                    .where("seniorProfileId", "==", seniorId)
                    .get();
                for (const doc of relationsQuery.docs) {
                    await doc.ref.delete();
                    deletedRelations++;
                }

                // 删除 health_records
                const healthQuery = await db.collection("health_records")
                    .where("seniorProfileId", "==", seniorId)
                    .get();
                for (const doc of healthQuery.docs) {
                    await doc.ref.delete();
                    deletedHealthRecords++;
                }
            } else {
                // 回滚所有 - 批量删除
                const batch = db.batch();
                let batchCount = 0;
                const maxBatchSize = 500;

                // 删除所有 profiles
                const profiles = await db.collection("senior_profiles").get();
                for (const doc of profiles.docs) {
                    batch.delete(doc.ref);
                    batchCount++;
                    deletedProfiles++;
                    if (batchCount >= maxBatchSize) {
                        await batch.commit();
                        batchCount = 0;
                    }
                }

                // 删除所有 passwords
                const passwords = await db.collection("senior_passwords").get();
                for (const doc of passwords.docs) {
                    batch.delete(doc.ref);
                    batchCount++;
                    deletedPasswords++;
                    if (batchCount >= maxBatchSize) {
                        await batch.commit();
                        batchCount = 0;
                    }
                }

                // 删除所有 relations
                const relations = await db.collection("caregiver_relations").get();
                for (const doc of relations.docs) {
                    batch.delete(doc.ref);
                    batchCount++;
                    deletedRelations++;
                    if (batchCount >= maxBatchSize) {
                        await batch.commit();
                        batchCount = 0;
                    }
                }

                // 删除所有 health_records
                const healthRecords = await db.collection("health_records").get();
                for (const doc of healthRecords.docs) {
                    batch.delete(doc.ref);
                    batchCount++;
                    deletedHealthRecords++;
                    if (batchCount >= maxBatchSize) {
                        await batch.commit();
                        batchCount = 0;
                    }
                }

                if (batchCount > 0) {
                    await batch.commit();
                }
            }

            console.log(`Rollback complete: ${deletedProfiles} profiles, ${deletedRelations} relations, ${deletedHealthRecords} health records, ${deletedPasswords} passwords deleted`);

            return {
                success: true,
                deletedProfiles,
                deletedRelations,
                deletedHealthRecords,
                deletedPasswords
            };

        } catch (error: any) {
            console.error("Rollback error:", error);
            throw new HttpsError("internal", `回滚失败: ${error.message}`);
        }
    }
);

/**
 * 验证迁移结果
 * 
 * 对比新旧数据，确保迁移正确
 */
export const validateMigration = onCall(
    {
        timeoutSeconds: 120,
        memory: "512MiB"
    },
    async (request) => {
        if (!request.auth) {
            throw new HttpsError("unauthenticated", "请先登录");
        }

        const { seniorId } = request.data;
        const db = admin.firestore();

        try {
            const validationResults: ValidationResult[] = [];

            // 获取要验证的老人
            let seniorsQuery;
            if (seniorId) {
                seniorsQuery = db.collection("seniors").where("id", "==", seniorId);
            } else {
                seniorsQuery = db.collection("seniors").limit(100); // 限制数量避免超时
            }

            const seniorsSnapshot = await seniorsQuery.get();

            for (const seniorDoc of seniorsSnapshot.docs) {
                const oldData = seniorDoc.data();
                const sid = oldData.id;

                // 获取新数据
                const newProfile = await db.collection("senior_profiles").doc(sid).get();
                const newRelations = await db.collection("caregiver_relations")
                    .where("seniorProfileId", "==", sid)
                    .get();
                const newHealthRecords = await db.collection("health_records")
                    .where("seniorProfileId", "==", sid)
                    .get();

                const result: ValidationResult = {
                    seniorId: sid,
                    profileExists: newProfile.exists,
                    profileMatches: false,
                    relationsCount: {
                        old: Object.keys(oldData.caregiverRelationships || {}).length,
                        new: newRelations.size
                    },
                    healthRecordsCount: {
                        old: (oldData.healthHistory || []).length,
                        new: newHealthRecords.size
                    },
                    issues: []
                };

                // 验证 profile 数据
                if (newProfile.exists) {
                    const newData = newProfile.data()!;
                    if (newData.name !== oldData.name) {
                        result.issues.push(`Name mismatch: ${oldData.name} vs ${newData.name}`);
                    }
                    if (newData.age !== oldData.age) {
                        result.issues.push(`Age mismatch: ${oldData.age} vs ${newData.age}`);
                    }
                    result.profileMatches = result.issues.length === 0;
                } else {
                    result.issues.push("Profile not found in new collection");
                }

                // 验证关系数量
                if (result.relationsCount.old !== result.relationsCount.new) {
                    result.issues.push(
                        `Relations count mismatch: ${result.relationsCount.old} vs ${result.relationsCount.new}`
                    );
                }

                // 验证健康记录数量
                if (result.healthRecordsCount.old !== result.healthRecordsCount.new) {
                    result.issues.push(
                        `Health records count mismatch: ${result.healthRecordsCount.old} vs ${result.healthRecordsCount.new}`
                    );
                }

                validationResults.push(result);
            }

            const passedCount = validationResults.filter(r => r.issues.length === 0).length;
            const failedCount = validationResults.filter(r => r.issues.length > 0).length;

            return {
                success: true,
                totalValidated: validationResults.length,
                passed: passedCount,
                failed: failedCount,
                results: validationResults
            };

        } catch (error: any) {
            console.error("Validation error:", error);
            throw new HttpsError("internal", `验证失败: ${error.message}`);
        }
    }
);

interface ValidationResult {
    seniorId: string;
    profileExists: boolean;
    profileMatches: boolean;
    relationsCount: { old: number; new: number };
    healthRecordsCount: { old: number; new: number };
    issues: string[];
}