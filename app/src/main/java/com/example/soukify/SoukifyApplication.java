package com.example.soukify;

import android.app.Application;
import android.util.Log;

import com.example.soukify.data.repositories.AuthPreferenceManager;
import com.example.soukify.utils.LocaleHelper;
import com.google.firebase.FirebaseApp;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;
import android.content.Context;
import org.json.JSONObject;

public class SoukifyApplication extends Application {
    private static final String TAG = "SoukifyApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            // Initialize Firebase with error handling
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
            Log.d(TAG, "Firebase initialized successfully");
            
            // Initialize AuthPreferenceManager for persistent like/favorite state
            AuthPreferenceManager.getInstance(this);
            Log.d(TAG, "AuthPreferenceManager initialized successfully");
            
            // Active les logs détaillés OneSignal pour le débogage (Utile pour voir ce que fait l'Oppo)
            OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
            
            // Initialize OneSignal for push notifications (used for Chat)
            OneSignal.initWithContext(this, "3e5e2256-41bb-473c-ae7b-a2e35cbfad9a");
            
            // Demande de permission avec un callback vide pour OneSignal 5.x
            OneSignal.getNotifications().requestPermission(true, com.onesignal.Continue.none());
            
            // Handle notification clicks
            OneSignal.getNotifications().addClickListener(new com.onesignal.notifications.INotificationClickListener() {
                @Override
                public void onClick(com.onesignal.notifications.INotificationClickEvent event) {
                    JSONObject data = event.getNotification().getAdditionalData();
                    if (data != null) {
                        Log.d(TAG, "Notification clicked with data: " + data.toString());
                        
                        String conversationId = data.optString("conversationId");
                        String productId = data.optString("productId");
                        String shopId = data.optString("shopId");
                        String type = data.optString("type");
                        
                        if (conversationId != null && !conversationId.isEmpty()) {
                            android.content.Intent intent = new android.content.Intent(SoukifyApplication.this, com.example.soukify.ui.chat.ChatActivity.class);
                            intent.putExtra("conversationId", conversationId);
                            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } else if (productId != null && !productId.isEmpty()) {
                            android.content.Intent intent = new android.content.Intent(SoukifyApplication.this, com.example.soukify.MainActivity.class);
                            intent.putExtra("type", "nouveau produit"); // Must match MainActivity logic
                            intent.putExtra("productId", productId);
                            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        } else if (shopId != null && !shopId.isEmpty()) {
                            android.content.Intent intent = new android.content.Intent(SoukifyApplication.this, com.example.soukify.MainActivity.class);
                            intent.putExtra("type", "promotion"); 
                            intent.putExtra("shopId", shopId);
                            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                    }
                }
            });

            Log.d(TAG, "✅ OneSignal initialisé avec succès");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing app components: " + e.getMessage(), e);
        }
    }
}
