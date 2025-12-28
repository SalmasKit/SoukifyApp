package com.example.soukify.services;

import android.util.Log;
import com.example.soukify.data.models.Conversation;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client-side notification sender using OneSignal API
 * Replaces legacy FCM implementation to ensure reliable delivery
 */
public class NotificationSenderService {

    private static final String TAG = "NotificationSender";
    
    // OneSignal Configuration
    private static final String ONESIGNAL_APP_ID = "3e5e2256-41bb-473c-ae7b-a2e35cbfad9a";
    private static final String ONESIGNAL_API_KEY = "os_v2_app_hzpcevsbxndtzlt3ulrvzp5ntkfgqjnaatvej4ua7rsvokacos2oihlklta2aw3tk3tzvha2oh4h6xqmkvqj7cio3b3p2vpi6pjyqdi";
    private static final String ONESIGNAL_API_URL = "https://onesignal.com/api/v1/notifications";
    
    private final FirebaseFirestore db;
    private final ExecutorService executor;

    public NotificationSenderService() {
        this.db = FirebaseFirestore.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Send new message notification
     */
    public void sendMessageNotification(String recipientId, String senderName, String messageText, String conversationId) {
        // Validation basic
        if (recipientId == null || recipientId.isEmpty()) return;

        executor.execute(() -> {
            // Check preferences logic is handled in ChatRepository usually, 
            // but we can double check or just send if called directly.
            // For now, we assume the caller has decided to send.
            
            sendOneSignalNotification(
                recipientId,
                "New message from " + senderName,
                messageText.length() > 100 ? messageText.substring(0, 97) + "..." : messageText,
                "message",
                conversationId,
                null,
                null
            );
        });
    }

    /**
     * Send new product notification to all users who liked the shop
     */
    /**
     * Send new product notification to all users who liked the shop
     */
    public void sendNewProductNotification(String shopId, String shopName, String productTitle, String productId) {
        executor.execute(() -> {
            Log.d(TAG, "Preparing to send new product notification for shop: " + shopName);
            
            db.collection("shops").document(shopId).get()
                .addOnSuccessListener(shopDoc -> {
                    if (!shopDoc.exists()) {
                        Log.e(TAG, "Shop not found: " + shopId);
                        return;
                    }

                    List<String> followers = (List<String>) shopDoc.get("likedByUserIds");
                    
                    // DEBUG: If no followers, send to current user (Owner) for verification
                    if (followers == null || followers.isEmpty()) {
                        Log.d(TAG, "No followers found in 'likedByUserIds' for shop: " + shopId + ". Sending to owner for testing.");
                        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                        if (currentUserId != null) {
                            checkPreferencesAndSend(currentUserId, "newProducts",
                                "New product at " + shopName,
                                "(Test to Owner) " + (productTitle != null ? productTitle : "Check out the latest addition!"),
                                "nouveau produit",
                                null,
                                shopId,
                                productId
                            );
                        }
                        return;
                    }

                    Log.d(TAG, "Found " + followers.size() + " followers. Sending notifications...");

                    for (String userId : followers) {
                        checkPreferencesAndSend(userId, "newProducts", 
                            "New product at " + shopName,
                            productTitle != null ? productTitle : "Check out the latest addition!",
                            "nouveau produit",
                            null,
                            shopId,
                            productId
                        );
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get shop document", e));
        });
    }

    /**
     * Send promotion notification to all users who liked the shop
     */
    public void sendPromotionNotification(String shopId, String shopName, String promotionMessage) {
        executor.execute(() -> {
            Log.d(TAG, "Preparing to send promotion notification for shop: " + shopName);

            db.collection("shops").document(shopId).get()
                .addOnSuccessListener(shopDoc -> {
                    if (!shopDoc.exists()) {
                        Log.e(TAG, "Shop not found: " + shopId);
                        return;
                    }

                    List<String> followers = (List<String>) shopDoc.get("likedByUserIds");

                    // DEBUG: If no followers, send to current user (Owner) for verification
                    if (followers == null || followers.isEmpty()) {
                        Log.d(TAG, "No followers found in 'likedByUserIds' for shop: " + shopId + ". Sending to owner for testing.");
                        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                        if (currentUserId != null) {
                            checkPreferencesAndSend(currentUserId, "shopPromotions",
                                "🎉 " + shopName + " has a promotion!",
                                "(Test to Owner) " + (promotionMessage != null ? promotionMessage : "Special offers available now!"),
                                "promotion",
                                null,
                                shopId,
                                null
                            );
                        }
                        return;
                    }

                    Log.d(TAG, "Found " + followers.size() + " followers. Sending notifications...");

                    for (String userId : followers) {
                        checkPreferencesAndSend(userId, "shopPromotions",
                            "🎉 " + shopName + " has a promotion!",
                            promotionMessage != null ? promotionMessage : "Special offers available now!",
                            "promotion",
                            null,
                            shopId,
                            null
                        );
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get shop document", e));
        });
    }

    /**
     * Check user preferences before sending
     */
    private void checkPreferencesAndSend(String userId, String prefKey, 
                                         String title, String body, String type, 
                                         String conversationId, String shopId, String productId) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener(settingsDoc -> {
                if (settingsDoc.exists()) {
                    Object notifPrefsObj = settingsDoc.get("notificationPreferences");
                    if (notifPrefsObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> notifPrefs = (Map<String, Object>) notifPrefsObj;
                        
                        // Check if push is enabled
                        Boolean pushEnabled = (Boolean) notifPrefs.get("push");
                        if (pushEnabled != null && !pushEnabled) {
                            Log.d(TAG, "Push notifications disabled for user: " + userId);
                            return;
                        }

                        // Check specific preference
                        Boolean prefEnabled = (Boolean) notifPrefs.get(prefKey);
                        if (prefEnabled != null && !prefEnabled) {
                            Log.d(TAG, prefKey + " notifications disabled for user: " + userId);
                            return;
                        }

                        // Check quiet hours
                        if (isInQuietHours(notifPrefs)) {
                            Log.d(TAG, "User " + userId + " is in quiet hours");
                            return;
                        }
                    }
                }

                // Send notification
                sendOneSignalNotification(userId, title, body, type, conversationId, shopId, productId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to check preferences, sending anyway", e);
                sendOneSignalNotification(userId, title, body, type, conversationId, shopId, productId);
            });
    }

    /**
     * Send OneSignal notification via REST API
     */
    private void sendOneSignalNotification(String userId, String title, String body, String type, 
                                           String conversationId, String shopId, String productId) {
        executor.execute(() -> {
            try {
                URL url = new URL(ONESIGNAL_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Basic " + ONESIGNAL_API_KEY);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("app_id", ONESIGNAL_APP_ID);
                
                // Target specific user
                JSONArray includeExternalUserIds = new JSONArray();
                includeExternalUserIds.put(userId);
                json.put("include_external_user_ids", includeExternalUserIds);

                // Content
                JSONObject contents = new JSONObject();
                contents.put("en", body);
                json.put("contents", contents);

                JSONObject headings = new JSONObject();
                headings.put("en", title);
                json.put("headings", headings);

                // Data Payload
                JSONObject data = new JSONObject();
                data.put("type", type);
                if (conversationId != null) data.put("conversationId", conversationId);
                if (shopId != null) data.put("shopId", shopId);
                if (productId != null) data.put("productId", productId);
                json.put("data", data);

                // Android Specifics (High Priority & Visibility)
                json.put("priority", 10);
                json.put("android_visibility", 1); // Public
                json.put("android_channel_id", getChannelId(type)); // Optional: Map to local channels if OneSignal allows mapping via Dashboard

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 201) {
                    Log.d(TAG, "✅ Notification sent successfully to " + userId + " (Type: " + type + ")");
                } else {
                    Log.e(TAG, "❌ Failed to send notification: " + responseCode);
                    // Read error stream if possible
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending notification", e);
            }
        });
    }

    private String getChannelId(String type) {
        // Determines which channel ID to suggest (must be created in OneSignal Dashboard or mapped locally)
        // For now returning null to let OneSignal use default or misc
        return null; 
    }

    /**
     * Check if current time is in quiet hours
     */
    private boolean isInQuietHours(Map<String, Object> notifPrefs) {
        try {
            Long startHour = getLong(notifPrefs.get("quietStartHour"));
            if (startHour == null) return false;

            Long startMinute = getLong(notifPrefs.get("quietStartMinute"));
            Long endHour = getLong(notifPrefs.get("quietEndHour"));
            Long endMinute = getLong(notifPrefs.get("quietEndMinute"));

            java.util.Calendar now = java.util.Calendar.getInstance();
            int currentHour = now.get(java.util.Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(java.util.Calendar.MINUTE);

            int currentTime = currentHour * 60 + currentMinute;
            int startTime = startHour.intValue() * 60 + (startMinute != null ? startMinute.intValue() : 0);
            int endTime = endHour != null ? endHour.intValue() * 60 + (endMinute != null ? endMinute.intValue() : 0) : 0;

            if (startTime <= endTime) {
                return currentTime >= startTime && currentTime < endTime;
            } else {
                return currentTime >= startTime || currentTime < endTime;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private Long getLong(Object value) {
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        return null;
    }
}
