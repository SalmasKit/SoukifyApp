package com.example.soukify.services;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import com.example.soukify.data.repositories.UserRepository;
import com.example.soukify.utils.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        // Update token in UserRepository
        // Since Service Context is different, we create a new Repo instance or access simpler method
        // But Repositories need Application.
        // We can just use UserRepository directly.
        try {
            UserRepository userRepository = new UserRepository(getApplication());
            if (userRepository.isUserLoggedIn()) {
                userRepository.updateFcmToken(token);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating token", e);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // Handle FCM messages here.
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleNow(remoteMessage.getData());
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Message Notification Body: " + body);
            
            // If the app is in foreground, standard Firebase notification might not show
            // So we show it manually using Helper
            new NotificationHelper(this).showNotification(title, body, "general");
        }
    }

    private void handleNow(Map<String, String> data) {
        String title = data.get("title");
        String body = data.get("body");
        String type = data.get("type");

        if (title != null && body != null) {
            new NotificationHelper(this).showNotification(title, body, type);
        }
    }
}
