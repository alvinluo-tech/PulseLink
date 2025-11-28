import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { GoogleGenerativeAI } from "@google/generative-ai";
import { defineSecret } from "firebase-functions/params";
import * as admin from "firebase-admin";

// 定义密钥
const googleApiKey = defineSecret("GOOGLE_API_KEY");

// 初始化 Gemini (在函数外部初始化以复用)
let genAI: GoogleGenerativeAI;

/**
 * 监听 Firestore 新消息写入 - 自动处理语音消息
 * 
 * 触发路径: chat_history/{userId}/messages/{messageId}
 * 
 * 当用户发送语音消息时:
 * 1. 检测到 type="audio" 的消息
 * 2. 使用 Gemini 直接理解音频内容 (支持 GCS URI)
 * 3. 获取用户健康数据作为上下文
 * 4. 生成 AI 回复并写入数据库
 */
export const onNewChatMessage = onDocumentCreated(
    {
        document: "chat_history/{userId}/messages/{messageId}",
        secrets: [googleApiKey],
        timeoutSeconds: 60,
        memory: "512MiB"
    },
    async (event) => {
        const newMessage = event.data?.data();
        const userId = event.params.userId;
        const messageId = event.params.messageId;

        console.log(`New message received: ${messageId} from user: ${userId}`);

        // 只处理用户发的语音消息
        if (!newMessage || newMessage.fromAssistant || newMessage.type !== "audio") {
            console.log("Skipping: Not a user audio message");
            return;
        }

        try {
            console.log("Processing audio message...");
            
            // 初始化 Gemini (首次调用时)
            if (!genAI) {
                genAI = new GoogleGenerativeAI(googleApiKey.value());
            }

            // 1. 解析 GCS URI 并下载音频文件
            const gcsUri = newMessage.audioGcsUri;
            console.log(`Audio GCS URI: ${gcsUri}`);
            
            const matches = gcsUri.match(/^gs:\/\/([^\/]+)\/(.+)$/);
            if (!matches) {
                throw new Error("Invalid GCS URI format");
            }

            const bucketName = matches[1]; // pulselink-xxx.appspot.com
            const filePath = matches[2];   // voice_messages/uid/file.m4a
            
            console.log(`Downloading from bucket: ${bucketName}, path: ${filePath}`);

            // 2. 下载文件到内存
            const bucket = admin.storage().bucket(bucketName);
            const [fileBuffer] = await bucket.file(filePath).download();
            
            console.log(`File downloaded, size: ${fileBuffer.length} bytes`);

            // 3. 转为 Base64
            const base64Audio = fileBuffer.toString('base64');
            
            // 4. 准备音频数据 (使用 inlineData 而不是 fileUri)
            const audioPart = {
                inlineData: {
                    mimeType: "audio/mp4", // m4a 对应 audio/mp4
                    data: base64Audio
                }
            };

            console.log(`Audio prepared as Base64, length: ${base64Audio.length}`);

            // 2. 获取用户健康数据作为上下文
            let healthContext = "No recent health data available.";
            
            try {
                // 尝试获取老人档案
                const profileDoc = await admin.firestore()
                    .collection("senior_profiles")
                    .doc(userId)
                    .get();
                
                if (profileDoc.exists) {
                    const profileData = profileDoc.data();
                    const age = profileData?.age || "Unknown";
                    const gender = profileData?.gender || "Unknown";
                    healthContext = `Senior Profile: Age ${age}, Gender: ${gender}. `;
                }

                // 获取最近的健康记录
                const healthRecords = await admin.firestore()
                    .collection("health_records")
                    .where("seniorId", "==", userId)
                    .orderBy("recordedAt", "desc")
                    .limit(3)
                    .get();

                if (!healthRecords.empty) {
                    const records = healthRecords.docs.map(doc => {
                        const data = doc.data();
                        return `BP: ${data.systolic}/${data.diastolic} mmHg, HR: ${data.heartRate} bpm`;
                    });
                    healthContext += `Recent readings: ${records.join(", ")}`;
                }

                console.log(`Health context: ${healthContext}`);
            } catch (error) {
                console.warn("Failed to fetch health data:", error);
            }

            // 3. 使用 systemInstruction 配置 AI 角色和行为
            const model = genAI.getGenerativeModel({ 
                model: "gemini-2.5-flash",
                systemInstruction: `You are Alex, a warm and caring health assistant for seniors.

Health Context: ${healthContext}

Your behavior:
- When the senior chats casually: respond warmly and naturally
- When they mention health issues: acknowledge their concern and give gentle advice
- When they report health data: confirm receipt and give brief feedback
- Keep responses to 3-4 sentences maximum
- Always end with: "*AI advice for reference only. Please consult a doctor if unwell.*"

Important: Respond directly to the audio content. Do NOT say things like "I'm ready to listen" or "I understand the instructions".`
            });

            // 4. 调用 Gemini 分析音频 (文字在前，音频在后)
            console.log("Calling Gemini to analyze audio...");
            const result = await model.generateContent([
                { text: "Here is the senior's voice message:" },
                audioPart
            ]);

            const responseText = result.response.text();
            console.log(`Gemini response: ${responseText}`);

            // 6. 将 AI 回复写入数据库
            await admin.firestore()
                .collection("chat_history")
                .doc(userId)
                .collection("messages")
                .add({
                    text: responseText,
                    fromAssistant: true,
                    type: "text",
                    timestamp: admin.firestore.FieldValue.serverTimestamp()
                });

            console.log("AI reply saved to Firestore");

        } catch (error: any) {
            console.error("AI processing failed:", error);
            
            // 发送错误消息给用户
            try {
                await admin.firestore()
                    .collection("chat_history")
                    .doc(userId)
                    .collection("messages")
                    .add({
                        text: "❌ Sorry, I couldn't process your voice message. Please try again or type your message.",
                        fromAssistant: true,
                        type: "text",
                        timestamp: admin.firestore.FieldValue.serverTimestamp()
                    });
            } catch (writeError) {
                console.error("Failed to write error message:", writeError);
            }
        }
    }
);
