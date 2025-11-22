# Quick Start: Deploy AI Chat Feature

## 🚀 Step-by-Step Deployment

### 1. Get Google AI API Key

1. Visit https://makersuite.google.com/app/apikey
2. Click "Create API Key"
3. Copy your API key

### 2. Configure Firebase Secret

```bash
cd functions
firebase functions:secrets:set GOOGLE_API_KEY
# Paste your API key when prompted
```

> **Note**: Using Firebase Functions v2 with `defineSecret` for better security and performance.

### 3. Deploy Cloud Function

```bash
# Install dependencies (first time only)
npm install

# Deploy
firebase deploy --only functions
```

Expected output:
```
✔  functions[chatWithAI(us-central1)] Successful create operation.
Function URL: https://us-central1-YOUR_PROJECT.cloudfunctions.net/chatWithAI
```

### 4. Test in Android App

1. Build and run the app
2. Login as a senior user
3. Navigate to Voice Assistant (center button)
4. Type a message like:
   - "How is my blood pressure?"
   - "I feel dizzy"
   - "Give me health tips"

### 5. Monitor

```bash
# Watch real-time logs
firebase functions:log --only chatWithAI

# Or check Firebase Console
# https://console.firebase.google.com/project/YOUR_PROJECT/functions
```

## 🧪 Testing Locally (Optional)

```bash
# Start Firebase emulators
firebase emulators:start

# In Android code, add (for testing only):
// FirebaseFunctions.getInstance().useEmulator("10.0.2.2", 5001)
```

## ✅ Verification Checklist

- [ ] API Key configured in Firebase Secrets
- [ ] Cloud Function deployed successfully
- [ ] Android app can send messages
- [ ] AI responds with context-aware answers
- [ ] Health data is included in AI context
- [ ] Error handling works (try without internet)

## 🐛 Troubleshooting

**Problem**: "unauthenticated" error
- **Solution**: User must be logged in via Firebase Auth

**Problem**: "AI service unavailable"
- **Solution**: 
  1. Check API key: `firebase functions:secrets:access GOOGLE_API_KEY`
  2. View logs: `firebase functions:log`
  3. Verify API key is valid at Google AI Studio

**Problem**: Slow response
- **Solution**: First call might be slow (cold start). Subsequent calls faster.

**Problem**: Function not found
- **Solution**: Run `firebase deploy --only functions` again

## 💡 Usage Examples

Test these prompts:

```
User: "我的血压怎么样？"
Expected: AI analyzes current BP data and gives advice

User: "我头疼"
Expected: AI provides health suggestions

User: "今天天气如何？"
Expected: AI politely redirects to health topics
```

## 📊 Cost Estimate

**Free Tier** (Gemini 1.5 Flash):
- 15 requests/minute
- Sufficient for testing and small user base

**Paid** (if exceeded):
- ~$0.01 per 100 messages (typical)
- Monitor usage in Firebase Console

## 🔐 Security Notes

- ✅ All requests require Firebase Auth
- ✅ API Key stored securely in Firebase Secrets
- ✅ Input validated on cloud function
- ✅ No sensitive data logged

## Next Steps

After successful deployment:
1. [ ] Test with real users
2. [ ] Collect feedback on AI responses
3. [ ] Adjust system instructions if needed
4. [ ] Add voice input (future feature)
5. [ ] Implement conversation memory (future feature)
