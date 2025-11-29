package com.example.soukify.data.remote.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.soukify.data.models.UserSettingModel;

import java.util.List;
import java.util.ArrayList;

/**
 * Firebase User Setting Service - Handles user setting operations
 * Follows MVVM pattern by providing clean separation from UI layer
 */
public class FirebaseUserSettingService {
    private final FirebaseFirestore firestore;
    
    private static final String USER_SETTINGS_COLLECTION = "user_settings";
    
    public FirebaseUserSettingService(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }
    
    // CRUD Operations
    
    /**
     * Create or update user settings (upsert operation)
     */
    public Task<Void> saveUserSettings(UserSettingModel userSetting) {
        return firestore.collection(USER_SETTINGS_COLLECTION)
                .document(userSetting.getUserId())
                .set(userSetting);
    }
    
    /**
     * Update user settings
     */
    public Task<Void> updateUserSettings(UserSettingModel userSetting) {
        return firestore.collection(USER_SETTINGS_COLLECTION)
                .document(userSetting.getUserId())
                .set(userSetting);
    }
    
    /**
     * Delete user settings
     */
    public Task<Void> deleteUserSettings(String userId) {
        return firestore.collection(USER_SETTINGS_COLLECTION).document(userId).delete();
    }
    
    // Query Operations
    
    /**
     * Get all user settings
     */
    public Query getAllUserSettings() {
        return firestore.collection(USER_SETTINGS_COLLECTION);
    }
    
    /**
     * Get user settings by user ID
     */
    public Task<UserSettingModel> getUserSettings(String userId) {
        return firestore.collection(USER_SETTINGS_COLLECTION).document(userId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().toObject(UserSettingModel.class);
                    }
                    return null;
                });
    }
    
    /**
     * Get user settings by user ID with defaults
     */
    public Task<UserSettingModel> getUserSettingsWithDefaults(String userId) {
        return getUserSettings(userId).continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                return com.google.android.gms.tasks.Tasks.forResult(task.getResult());
            } else {
                // Return default settings if none exist
                UserSettingModel defaultSettings = createDefaultSettings(userId);
                return com.google.android.gms.tasks.Tasks.forResult(defaultSettings);
            }
        });
    }
    
    /**
     * Get users by theme preference
     */
    public Query getUsersByTheme(String theme) {
        return firestore.collection(USER_SETTINGS_COLLECTION)
                .whereEqualTo("theme", theme);
    }
    
    /**
     * Get users by language preference
     */
    public Query getUsersByLanguage(String language) {
        return firestore.collection(USER_SETTINGS_COLLECTION)
                .whereEqualTo("language", language);
    }
    
    /**
     * Get users by currency preference
     */
    public Query getUsersByCurrency(String currency) {
        return firestore.collection(USER_SETTINGS_COLLECTION)
                .whereEqualTo("currency", currency);
    }
    
    /**
     * Get users with notifications enabled
     */
    public Query getUsersWithNotificationsEnabled() {
        return firestore.collection(USER_SETTINGS_COLLECTION)
                .whereEqualTo("notifications", true);
    }
    
    // Utility Operations
    
    /**
     * Create default user settings
     */
    private UserSettingModel createDefaultSettings(String userId) {
        UserSettingModel settings = new UserSettingModel();
        settings.setUserId(userId);
        settings.setTheme("system"); // system, light, dark
        settings.setLanguage("fr"); // French by default
        settings.setCurrency("MAD"); // Moroccan Dirham
        settings.setNotifications(true);
        return settings;
    }
    
    /**
     * Initialize default settings for a user
     */
    public Task<Void> initializeUserSettings(String userId) {
        UserSettingModel defaultSettings = createDefaultSettings(userId);
        return saveUserSettings(defaultSettings);
    }
    
    /**
     * Update specific setting fields
     */
    public Task<Void> updateTheme(String userId, String theme) {
        return firestore.collection(USER_SETTINGS_COLLECTION)
                .document(userId)
                .update("theme", theme, "updatedAt", String.valueOf(System.currentTimeMillis()));
    }
    
    public Task<Void> updateLanguage(String userId, String language) {
        return firestore.collection(USER_SETTINGS_COLLECTION)
                .document(userId)
                .update("language", language, "updatedAt", String.valueOf(System.currentTimeMillis()));
    }
    
    public Task<Void> updateCurrency(String userId, String currency) {
        return firestore.collection(USER_SETTINGS_COLLECTION)
                .document(userId)
                .update("currency", currency, "updatedAt", String.valueOf(System.currentTimeMillis()));
    }
    
    public Task<Void> updateNotifications(String userId, boolean enabled) {
        return firestore.collection(USER_SETTINGS_COLLECTION)
                .document(userId)
                .update("notifications", enabled, "updatedAt", String.valueOf(System.currentTimeMillis()));
    }
}
