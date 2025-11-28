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

// 导出语音消息自动处理函数
export { onNewChatMessage } from "./chatWithAudio";

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

            // --- 更新 senior_profiles 文档的 userId 字段 ---
            await admin.firestore().collection("senior_profiles").doc(seniorId).update({
                userId: userRecord.uid
            });

            console.log(`Updated senior_profiles/${seniorId} with userId: ${userRecord.uid}`);

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
 * 删除老人账户的 Cloud Function (原子性删除所有数据)
 * 
 * 调用参数:
 * - seniorId: 老人的虚拟ID (例如: SNR-ABCD1234)
 * - requesterId: 请求者的 caregiver ID
 * 
 * 返回:
 * - success: boolean
 * - message: 删除结果消息
 * - deletedProfile: boolean
 * - deletedHealthRecords: number
 * - deletedRelations: number
 * - deletedAuth: boolean
 */
export const deleteSeniorAccount = onCall(
    {
        timeoutSeconds: 60,
        memory: "512MiB"
    },
    async (request) => {
        // --- 鉴权检查：只有已登录的 caregiver 可以删除 ---
        if (!request.auth) {
            throw new HttpsError("unauthenticated", "请先登录");
        }

        // --- 获取参数 ---
        const { seniorId, requesterId } = request.data;

        // --- 参数验证 ---
        if (!seniorId || typeof seniorId !== "string") {
            throw new HttpsError("invalid-argument", "seniorId 是必需的");
        }
        
        if (!requesterId || typeof requesterId !== "string") {
            throw new HttpsError("invalid-argument", "requesterId 是必需的");
        }

        const db = admin.firestore();
        const batch = db.batch();
        
        let deletedHealthRecords = 0;
        let deletedRelations = 0;
        let deletedProfile = false;
        let deletedAuth = false;

        try {
            console.log(`Deleting senior account: ${seniorId} by requester: ${requesterId}`);

            // --- 1. 验证权限（必须是创建者）---
            const profileDoc = await db.collection("senior_profiles").doc(seniorId).get();
            
            if (!profileDoc.exists) {
                throw new HttpsError("not-found", "老人资料不存在");
            }
            
            const profileData = profileDoc.data();
            if (profileData?.creatorId !== requesterId) {
                throw new HttpsError("permission-denied", "只有创建者才能删除账号");
            }

            // --- 2. 检查是否有其他护理者 ---
            const relationsSnapshot = await db.collection("caregiver_relations")
                .where("seniorId", "==", seniorId)
                .where("status", "==", "active")
                .get();
            
            const activeRelations = relationsSnapshot.docs.filter(doc => {
                const data = doc.data();
                return data.caregiverId !== requesterId;
            });
            
            if (activeRelations.length > 0) {
                // 构建其他护理者信息（英文）
                const otherCaregivers = activeRelations.map(doc => {
                    const data = doc.data();
                    // 使用英文关系名称
                    return data.relationship || "Caregiver";
                });
                
                const count = activeRelations.length;
                const caregiverText = count === 1 ? "caregiver" : "caregivers";
                
                throw new HttpsError(
                    "failed-precondition",
                    `Cannot delete: ${count} other ${caregiverText} (${otherCaregivers.join(", ")}) are still linked to this senior account`
                );
            }

            // --- 3. 批量删除所有 health_records ---
            const healthRecordsSnapshot = await db.collection("health_records")
                .where("seniorId", "==", seniorId)
                .get();
            
            console.log(`Found ${healthRecordsSnapshot.size} health records to delete`);
            healthRecordsSnapshot.docs.forEach(doc => {
                batch.delete(doc.ref);
                deletedHealthRecords++;
            });

            // --- 4. 批量删除所有 caregiver_relations ---
            // 重新查询所有关系（包括非活跃的）
            const allRelationsSnapshot = await db.collection("caregiver_relations")
                .where("seniorId", "==", seniorId)
                .get();
            
            console.log(`Found ${allRelationsSnapshot.size} relations to delete`);
            allRelationsSnapshot.docs.forEach(doc => {
                batch.delete(doc.ref);
                deletedRelations++;
            });

            // --- 5. 删除 senior_profile ---
            batch.delete(profileDoc.ref);
            deletedProfile = true;

            // --- 6. 提交批量删除 (原子性操作) ---
            await batch.commit();
            console.log(`Batch deleted: ${deletedHealthRecords} health records, ${deletedRelations} relations, 1 profile`);

            // --- 7. 删除 Firebase Auth 账户 ---
            const email = `senior_${seniorId}@pulselink.app`;
            
            try {
                const userRecord = await admin.auth().getUserByEmail(email);
                const uid = userRecord.uid;
                
                // 删除 users 文档
                await db.collection("users").doc(uid).delete();
                
                // 删除 Auth 账户
                await admin.auth().deleteUser(uid);
                
                deletedAuth = true;
                console.log(`Deleted Firebase Auth user: ${uid}`);
            } catch (error: any) {
                if (error.code === "auth/user-not-found") {
                    console.log(`Firebase Auth user not found for ${seniorId}, skipping Auth deletion`);
                } else {
                    console.error(`Failed to delete Auth user:`, error);
                    // Auth 删除失败不影响整体成功（Firestore 数据已删除）
                }
            }

            // --- 返回结果 ---
            return {
                success: true,
                message: "Senior account deleted successfully",
                deletedProfile: deletedProfile,
                deletedHealthRecords: deletedHealthRecords,
                deletedRelations: deletedRelations,
                deletedAuth: deletedAuth
            };

        } catch (error: any) {
            console.error("Delete senior account error:", error);
            
            // 如果是业务错误（权限、不存在），直接抛出
            if (error instanceof HttpsError) {
                throw error;
            }
            
            // 其他错误
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