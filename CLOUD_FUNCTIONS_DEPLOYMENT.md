# Firebase Cloud Functions Deployment Guide

## 📋 Prerequisites
1. **Node.js 18+** installed ([Download here](https://nodejs.org/))
2. **Firebase CLI** installed:
   ```bash
   npm install -g firebase-tools
   ```

## 🚀 Deployment Steps

### 1. Login to Firebase
```bash
firebase login
```

### 2. Initialize Firebase Project (if not done)
```bash
cd e:\AndroidStudioProjects\Soukify
firebase init
```
- Select: **Functions**
- Choose: **Use an existing project** → Select your Firebase project
- Language: **JavaScript**
- ESLint: **No**
- Install dependencies: **Yes**

### 3. Install Dependencies
```bash
cd functions
npm install
```

### 4. Deploy Cloud Functions
```bash
firebase deploy --only functions
```

## 📱 What These Functions Do

### 1. **sendMessageNotification**
- **Trigger**: New document in `conversations/{conversationId}/messages/{messageId}`
- **Action**: Sends push notification to message recipient
- **Checks**: User preferences for messages, push enabled, quiet hours

### 2. **sendNewProductNotification**
- **Trigger**: New document in `products/{productId}`
- **Action**: Notifies all users who liked the shop
- **Checks**: User preferences for new products, push enabled, quiet hours

### 3. **sendPromotionNotification**
- **Trigger**: Shop document updated with `hasPromotion: true`
- **Action**: Notifies all users who liked the shop
- **Checks**: User preferences for promotions, push enabled, quiet hours

## 🔍 Monitoring

### View Logs
```bash
firebase functions:log
```

### Test Locally (Optional)
```bash
firebase emulators:start --only functions
```

## 💰 Cost (Free Tier)
- **2M invocations/month** free
- **400K GB-seconds** compute time free
- **200K CPU-seconds** free

Your usage should stay well within free limits.

## 🛠️ Troubleshooting

### Error: "Firebase project not found"
```bash
firebase use --add
# Select your project from the list
```

### Error: "Insufficient permissions"
Make sure you're logged in as the project owner:
```bash
firebase login --reauth
```

### Functions not triggering
1. Check Firebase Console → Functions → Logs
2. Verify Firestore collections match expected names
3. Ensure users have `fcmToken` field populated

## 📊 Verify Deployment

After deploying, check:
1. **Firebase Console** → Functions → Dashboard
2. You should see 3 functions listed:
   - `sendMessageNotification`
   - `sendNewProductNotification`
   - `sendPromotionNotification`

## 🧪 Testing

### Test Message Notification
Send a chat message in your app → Recipient should get notification

### Test New Product Notification
1. Like a shop
2. Add a new product to that shop → You should get notification

### Test Promotion Notification
1. Like a shop
2. Update shop with `hasPromotion: true` → You should get notification
