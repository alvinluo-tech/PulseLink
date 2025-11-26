# Voice Input - MediaRecorder + Gemini API Implementation

## Architecture Overview

```
User Press Mic → MediaRecorder (m4a) → Base64 Encode → 
Firebase Cloud Function → Gemini 2.0 Flash → Transcribed Text → Input Field
```

## Components

### 1. AudioRecorderManager (`data/speech/AudioRecorderManager.kt`)
- **Purpose**: Manage MediaRecorder lifecycle
- **File Format**: `.m4a` (audio/mp4 MIME type)
- **Key Methods**:
  - `startRecording()`: Create temp file, start MediaRecorder
  - `stopRecording()`: Stop recording, return File
  - `cancelRecording()`: Stop and delete file
  - `getBase64Audio()`: Convert m4a to Base64 string

### 2. VoiceToTextUseCase (`domain/usecase/VoiceToTextUseCase.kt`)
- **Purpose**: Call Firebase Cloud Function for transcription
- **Function**: `voiceToText`
- **Input**: `{ audio: "Base64 string" }`
- **Output**: `{ success: true, text: "transcribed text" }`

### 3. AssistantViewModel (`presentation/senior/voice/AssistantViewModel.kt`)
- **Workflow**:
  1. **onMicPressed()**: 
     - Call `audioRecorderManager.startRecording()`
     - Update UI (isRecording = true)
  
  2. **onMicReleased()**:
     - Stop recording
     - Read file and convert to Base64
     - Call `voiceToTextUseCase(base64Audio)`
     - On success: Update `inputText` with transcribed text
     - On failure: Show error message
     - Delete temp audio file

### 4. Cloud Function (`functions/src/index.ts`)
- **Function Name**: `voiceToText`
- **Model**: `gemini-2.0-flash-exp`
- **Prompt**: Transcription-only (no conversational response)
- **Audio Format**: audio/mp4 (Base64)
- **Timeout**: 60 seconds
- **Memory**: 512MiB

## Deployment Status

✅ **Cloud Function Deployed**: `voiceToText` (us-central1)
✅ **Android Code Complete**: AudioRecorderManager + VoiceToTextUseCase integrated
✅ **ViewModel Updated**: onMicPressed/Released use new system

## Testing Checklist

### 1. Permissions
- [ ] RECORD_AUDIO permission granted in app settings
- [ ] User authenticated (required by Cloud Function)

### 2. Recording
- [ ] Press mic button → Toast "Mic pressed!"
- [ ] Check Logcat: `AudioRecorder: Started recording`
- [ ] Verify file created: `/data/user/0/com.example.pulselink/cache/voice_*.m4a`

### 3. Transcription
- [ ] Release mic button → Toast "Mic released!"
- [ ] Check Logcat: `AssistantViewModel: Audio Base64 length: [size]`
- [ ] Check Logcat: `AssistantViewModel: Calling voiceToText...`
- [ ] Check Firebase Console → Functions logs for Gemini API call
- [ ] Verify transcribed text appears in input field

### 4. Error Handling
- [ ] Network error → Toast shows error message
- [ ] Empty audio → Gemini returns empty or error
- [ ] Permission denied → Recording fails with error

## Logcat Filters

```bash
# Android logs
adb logcat -s AudioRecorder AssistantViewModel VoiceToTextUseCase

# Firebase Cloud Function logs
firebase functions:log --only voiceToText
```

## Debugging Tips

### If recording doesn't start:
1. Check RECORD_AUDIO permission: Settings → Apps → PulseLink → Permissions
2. Check Logcat for `AudioRecorder: Error starting recording`
3. Verify cache directory is writable

### If transcription fails:
1. Check network connectivity
2. Verify user is authenticated: `request.auth != null`
3. Check Firebase Console → Functions for error logs
4. Verify GOOGLE_API_KEY secret is set: `firebase functions:secrets:access GOOGLE_API_KEY`

### If text doesn't appear:
1. Check Logcat: `AssistantViewModel: Transcription result: [text]`
2. Verify `_uiState.update { it.copy(inputText = text) }` is called
3. Check if UI recomposition triggers on inputText change

## Gemini Prompt

The Cloud Function uses a strict "transcription-only" prompt:

```
Please transcribe this audio verbatim.
Requirements:
1. Only output the recognized text content.
2. Do not answer questions in the audio.
3. Do not add any explanatory text except punctuation.
4. If the audio contains Chinese, use Simplified Chinese.
5. If the audio contains English, use English.
```

This prevents Gemini from responding conversationally (e.g., "Sure! You said...").

## Comparison with SpeechRecognizer

| Feature | SpeechRecognizer | MediaRecorder + Gemini |
|---------|------------------|------------------------|
| **Dependency** | Google Speech Services | Network + Gemini API |
| **Device Support** | Limited (requires Google services) | Universal (any Android device) |
| **Network** | Required | Required |
| **Cost** | Free | Gemini API usage fees |
| **Accuracy** | Good (Google ASR) | Excellent (Gemini multimodal) |
| **Languages** | Many | Many (Gemini supports 100+) |
| **Offline** | No | No |

## Cost Estimation

Gemini 2.0 Flash pricing (as of deployment):
- **Audio Input**: ~$0.000315 per second
- **Text Output**: ~$0.000001 per character

Example: 10-second recording → ~$0.00315 per transcription

## Next Steps

1. **Test on Real Device**: Deploy APK and test end-to-end flow
2. **Add Loading State**: Show spinner while transcribing
3. **Add Audio Waveform**: Visual feedback during recording
4. **Optimize**: Compress audio before upload (reduce bandwidth)
5. **Error Messages**: Improve user-facing error messages (Chinese)

## Migration Notes

**Removed**:
- `SpeechRecognizerManager.kt` (deprecated)
- `observeSpeechRecognition()` in ViewModel
- SpeechRecognizer dependencies

**Added**:
- `AudioRecorderManager.kt`
- `VoiceToTextUseCase.kt`
- `voiceToText` Cloud Function
- Base64 encoding logic

**Modified**:
- `AssistantViewModel.kt`: Switched from SpeechRecognizer to MediaRecorder
- `VoiceAssistantScreen.kt`: Added RECORD_AUDIO permission checks
