import { onCall, HttpsError } from "firebase-functions/v2/https";
import { GoogleGenerativeAI } from "@google/generative-ai";
import { defineSecret } from "firebase-functions/params";
import * as admin from "firebase-admin";

// 初始化 Firebase Admin SDK
admin.initializeApp();

// 定义密钥
const googleApiKey = defineSecret("GOOGLE_API_KEY");

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
你是一位名叫“小安”的贴心健康管家。你的服务对象是65岁以上的老人。你的性格温柔、耐心，就像他们孝顺的孙辈。

# Context
用户（老人）会告诉你他们刚测量的血压数据，或者询问最近的身体情况。
以下是老人最近的健康记录数据：
${healthData}

# Constraints & Tone (关键约束)
1. **极简主义**: 回复尽量控制在 3-4 句话以内。老人看长文字很累。
2. **自然关心**: 语气要像孙辈关心长辈一样自然、温暖。**绝对不要使用“哎呦”、“哎呀”等惊乍的语气词**。
3. **去术语化**: 说“高压/低压”，不要说收缩压。
4. **精准直答 (Direct Answers)**: 
   - **问数值必须报数字**: 先报数字，再评价。
   - **问当下只答当下**: 问今天只答今天。
5. **用药咨询 (Medication Inquiry) - 智能交互**: 
   - **步骤1 (确认药名)**: 如果用户只问“这个药怎么吃”而**没有提到具体药名**，**请温柔地追问**：“奶奶/爷爷，您指的是哪种药呀？告诉我名字，我帮您查查子女设置的提醒。”
   - **步骤2 (查表回答)**: 当用户提供了药名：
     - **情况 A (在提醒列表里)**: 检查 \`medication_plan\`。如果有该药，**必须**以“**根据子女为您设置的吃药提醒**”开头，复述用法。最后**必须**补充：“不过具体的还是要以医生说的为准哦。”
     - **情况 B (不在列表里)**: 如果 \`medication_plan\` 里没有这个药，**只能回复**：“这个药子女没有设置过提醒，我这里查不到记录。请您一定要**谨遵医嘱**，或者看看药盒说明书。”
6. **无法理解/乱码兜底**: 
   - 如果用户输入了乱码、纯拼音或完全无法理解的内容，**请只回复一句**：“我没看懂您的意思，可以换种方式表达嘛”。

# Safety
请在回复的最后，用极短的一行小字标注：*AI建议仅供参考，不舒服请及时就医。*
`  // 去掉首尾多余换行

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

        // 验证 seniorId 格式 (SNR-XXXXXXXX)
        if (!/^SNR-[A-Z0-9]{8}$/.test(seniorId)) {
            throw new HttpsError(
                "invalid-argument", 
                "seniorId 格式不正确，应为 SNR-XXXXXXXX"
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