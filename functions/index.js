const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

// ============================================
// 1. NEW CHAT MESSAGE NOTIFICATION
// ============================================
exports.sendMessageNotification = functions.firestore
    .document('Conversation/{conversationId}/messages/{messageId}')
    .onCreate(async (snap, context) => {
        const message = snap.data();
        const conversationId = context.params.conversationId;

        console.log('📨 New message in conversation:', conversationId);

        try {
            // Get conversation details
            const conversationDoc = await db.collection('Conversation').doc(conversationId).get();
            if (!conversationDoc.exists) {
                console.log('Conversation not found');
                return null;
            }

            const conversation = conversationDoc.data();
            const senderId = message.senderId;
            const recipientId = conversation.buyerId === senderId ? conversation.sellerId : conversation.buyerId;

            console.log('Sender:', senderId, 'Recipient:', recipientId);

            // Get recipient's FCM token and preferences
            const recipientDoc = await db.collection('users').doc(recipientId).get();
            if (!recipientDoc.exists) {
                console.log('Recipient not found');
                return null;
            }

            const recipient = recipientDoc.data();
            const fcmToken = recipient.fcmToken;

            if (!fcmToken) {
                console.log('No FCM token for recipient');
                return null;
            }

            // Check notification preferences
            const settingsDoc = await db.collection('user_settings').doc(recipientId).get();
            const settings = settingsDoc.exists ? settingsDoc.data() : {};
            const notifPrefs = settings.notificationPreferences || {};

            // Check if user wants message notifications
            if (notifPrefs.messages === false || notifPrefs.push === false) {
                console.log('User has disabled message notifications');
                return null;
            }

            // Check quiet hours
            if (isInQuietHours(notifPrefs)) {
                console.log('User is in quiet hours');
                return null;
            }

            // Get sender name
            const senderDoc = await db.collection('users').doc(senderId).get();
            const senderName = senderDoc.exists ? senderDoc.data().fullName : 'Someone';

            // Send notification
            const payload = {
                notification: {
                    title: `New message from ${senderName}`,
                    body: message.text.length > 100 ? message.text.substring(0, 97) + '...' : message.text,
                },
                data: {
                    type: 'message',
                    conversationId: conversationId,
                    senderId: senderId,
                },
                token: fcmToken,
            };

            await messaging.send(payload);
            console.log('✅ Message notification sent');
            return null;

        } catch (error) {
            console.error('Error sending message notification:', error);
            return null;
        }
    });

// ============================================
// 2. NEW PRODUCT FROM LIKED SHOP NOTIFICATION
// ============================================
exports.sendNewProductNotification = functions.firestore
    .document('products/{productId}')
    .onCreate(async (snap, context) => {
        const product = snap.data();
        const shopId = product.shopId;

        console.log('🆕 New product created in shop:', shopId);

        try {
            // Find all users who liked this shop
            const likesSnapshot = await db.collection('shop_likes')
                .where('shopId', '==', shopId)
                .where('liked', '==', true)
                .get();

            if (likesSnapshot.empty) {
                console.log('No users have liked this shop');
                return null;
            }

            console.log(`Found ${likesSnapshot.size} users who liked this shop`);

            // Get shop details
            const shopDoc = await db.collection('shops').doc(shopId).get();
            const shopName = shopDoc.exists ? shopDoc.data().name : 'A shop you follow';

            // Send notification to each user
            const promises = likesSnapshot.docs.map(async (likeDoc) => {
                const userId = likeDoc.data().userId;

                // Get user's FCM token and preferences
                const userDoc = await db.collection('users').doc(userId).get();
                if (!userDoc.exists) return;

                const user = userDoc.data();
                const fcmToken = user.fcmToken;
                if (!fcmToken) return;

                // Check notification preferences
                const settingsDoc = await db.collection('user_settings').doc(userId).get();
                const settings = settingsDoc.exists ? settingsDoc.data() : {};
                const notifPrefs = settings.notificationPreferences || {};

                if (notifPrefs.newProducts === false || notifPrefs.push === false) {
                    console.log(`User ${userId} has disabled new product notifications`);
                    return;
                }

                if (isInQuietHours(notifPrefs)) {
                    console.log(`User ${userId} is in quiet hours`);
                    return;
                }

                // Send notification
                const payload = {
                    notification: {
                        title: `New product at ${shopName}`,
                        body: product.title || 'Check out the latest addition!',
                    },
                    data: {
                        type: 'nouveau produit',
                        shopId: shopId,
                        productId: context.params.productId,
                    },
                    token: fcmToken,
                };

                try {
                    await messaging.send(payload);
                    console.log(`✅ New product notification sent to ${userId}`);
                } catch (error) {
                    console.error(`Failed to send to ${userId}:`, error);
                }
            });

            await Promise.all(promises);
            return null;

        } catch (error) {
            console.error('Error sending new product notifications:', error);
            return null;
        }
    });

// ============================================
// 3. SHOP PROMOTION NOTIFICATION
// ============================================
exports.sendPromotionNotification = functions.firestore
    .document('shops/{shopId}')
    .onUpdate(async (change, context) => {
        const before = change.before.data();
        const after = change.after.data();
        const shopId = context.params.shopId;

        // Check if promotion was just enabled (from false/null to true)
        const promotionEnabled = !before.hasPromotion && after.hasPromotion;

        if (!promotionEnabled) {
            console.log('No promotion change detected');
            return null;
        }

        console.log('🎉 Promotion enabled for shop:', shopId);

        try {
            // Find all users who liked this shop
            const likesSnapshot = await db.collection('shop_likes')
                .where('shopId', '==', shopId)
                .where('liked', '==', true)
                .get();

            if (likesSnapshot.empty) {
                console.log('No users have liked this shop');
                return null;
            }

            console.log(`Found ${likesSnapshot.size} users who liked this shop`);

            const shopName = after.name || 'A shop you follow';
            const promotionMessage = after.promotionMessage || 'Special offers available now!';

            // Send notification to each user
            const promises = likesSnapshot.docs.map(async (likeDoc) => {
                const userId = likeDoc.data().userId;

                // Get user's FCM token and preferences
                const userDoc = await db.collection('users').doc(userId).get();
                if (!userDoc.exists) return;

                const user = userDoc.data();
                const fcmToken = user.fcmToken;
                if (!fcmToken) return;

                // Check notification preferences
                const settingsDoc = await db.collection('user_settings').doc(userId).get();
                const settings = settingsDoc.exists ? settingsDoc.data() : {};
                const notifPrefs = settings.notificationPreferences || {};

                if (notifPrefs.shopPromotions === false || notifPrefs.push === false) {
                    console.log(`User ${userId} has disabled promotion notifications`);
                    return;
                }

                if (isInQuietHours(notifPrefs)) {
                    console.log(`User ${userId} is in quiet hours`);
                    return;
                }

                // Send notification
                const payload = {
                    notification: {
                        title: `🎉 ${shopName} has a promotion!`,
                        body: promotionMessage,
                    },
                    data: {
                        type: 'promotion',
                        shopId: shopId,
                    },
                    token: fcmToken,
                };

                try {
                    await messaging.send(payload);
                    console.log(`✅ Promotion notification sent to ${userId}`);
                } catch (error) {
                    console.error(`Failed to send to ${userId}:`, error);
                }
            });

            await Promise.all(promises);
            return null;

        } catch (error) {
            console.error('Error sending promotion notifications:', error);
            return null;
        }
    });

// ============================================
// HELPER: Check if current time is in quiet hours
// ============================================
function isInQuietHours(notifPrefs) {
    if (!notifPrefs.quietStartHour && notifPrefs.quietStartHour !== 0) return false;

    const now = new Date();
    const currentHour = now.getHours();
    const currentMinute = now.getMinutes();

    const startHour = notifPrefs.quietStartHour || 0;
    const startMinute = notifPrefs.quietStartMinute || 0;
    const endHour = notifPrefs.quietEndHour || 0;
    const endMinute = notifPrefs.quietEndMinute || 0;

    const currentTime = currentHour * 60 + currentMinute;
    const startTime = startHour * 60 + startMinute;
    const endTime = endHour * 60 + endMinute;

    if (startTime <= endTime) {
        // Normal range (e.g., 22:00 to 08:00 within same day)
        return currentTime >= startTime && currentTime < endTime;
    } else {
        // Overnight range (e.g., 22:00 to 08:00 crossing midnight)
        return currentTime >= startTime || currentTime < endTime;
    }
}
