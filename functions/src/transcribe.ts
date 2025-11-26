import { onCall, HttpsError } from "firebase-functions/v2/https";
import { GoogleGenerativeAI } from "@google/generative-ai";
import { defineSecret } from "firebase-functions/params";

// 定义密钥
const googleApiKey = defineSecret("GOOGLE_API_KEY");

/**
 * 音频转文字 Cloud Function
 * 使用 Gemini API 的语音转文字功能
 */
export const transcribeAudio = onCall(
    {
        secrets: [googleApiKey],
        timeoutSeconds: 60,
        memory: "512MiB"
    },
    async (request) => {
        // --- 鉴权检查 ---
        if (!request.auth) {
            throw new HttpsError("unauthenticated", "请先登录");
        }

        // --- 获取参数 ---
        const audioBase64 = request.data.audio;
        
        if (!audioBase64 || typeof audioBase64 !== "string") {
            throw new HttpsError("invalid-argument", "音频数据是必需的");
        }

        try {
            console.log(`Transcribing audio, size: ${audioBase64.length} chars`);
            
            // 初始化 Gemini 客户端
            const genAI = new GoogleGenerativeAI(googleApiKey.value());
            
            // 使用 Gemini 1.5 Flash，它支持音频输入
            const model = genAI.getGenerativeModel({ 
                model: "gemini-1.5-flash"
            });

            // 将音频数据转换为 Gemini 可接受的格式
            const result = await model.generateContent([
                {
                    inlineData: {
                        data: audioBase64,
                        mimeType: "audio/mp4"
                    }
                },
                "Please transcribe this audio to text. Only return the transcribed text, nothing else."
            ]);

            const response = result.response;
            const transcript = response.text().trim();
            
            console.log(`Transcription result: ${transcript}`);

            return {
                success: true,
                transcript: transcript
            };

        } catch (error: any) {
            console.error("Transcription Error:", error);
            throw new HttpsError("internal", `转录失败: ${error.message}`);
        }
    }
);
