package com.example.soukify.data.repositories;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseUserSettingService;
import com.example.soukify.data.models.UserSettingModel;

/**
 * User Setting Repository - Firebase implementation
 * Follows MVVM pattern by abstracting data operations from ViewModels
 */
public class UserSettingRepository {
    private final FirebaseUserSettingService userSettingService;
    private final MutableLiveData<UserSettingModel> currentUserSettings = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    
    public UserSettingRepository(Application application) {
        FirebaseManager firebaseManager = FirebaseManager.getInstance(application);
        this.userSettingService = new FirebaseUserSettingService(firebaseManager.getFirestore());
    }
    
    // LiveData getters
    public LiveData<UserSettingModel> getCurrentUserSettings() {
        return currentUserSettings;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    // CRUD Operations
    
    public void saveUserSettings(UserSettingModel userSettings) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        userSettingService.saveUserSettings(userSettings)
                .addOnSuccessListener(aVoid -> {
                    currentUserSettings.postValue(userSettings);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to save user settings: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void updateUserSettings(UserSettingModel userSettings) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        userSettingService.updateUserSettings(userSettings)
                .addOnSuccessListener(aVoid -> {
                    currentUserSettings.postValue(userSettings);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to update user settings: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void deleteUserSettings(String userId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        userSettingService.deleteUserSettings(userId)
                .addOnSuccessListener(aVoid -> {
                    currentUserSettings.postValue(null);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to delete user settings: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    // Query Operations
    
    public void loadUserSettings(String userId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        userSettingService.getUserSettingsWithDefaults(userId)
                .addOnSuccessListener(userSettings -> {
                    currentUserSettings.postValue(userSettings);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load user settings: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    // Utility Operations
    
    public void initializeUserSettings(String userId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        userSettingService.initializeUserSettings(userId)
                .addOnSuccessListener(aVoid -> {
                    loadUserSettings(userId); // Load the newly created settings
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to initialize user settings: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void updateTheme(String userId, String theme) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        userSettingService.updateTheme(userId, theme)
                .addOnSuccessListener(aVoid -> {
                    loadUserSettings(userId); // Refresh the settings
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to update theme: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void updateLanguage(String userId, String language) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        userSettingService.updateLanguage(userId, language)
                .addOnSuccessListener(aVoid -> {
                    loadUserSettings(userId); // Refresh the settings
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to update language: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void updateCurrency(String userId, String currency) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        userSettingService.updateCurrency(userId, currency)
                .addOnSuccessListener(aVoid -> {
                    loadUserSettings(userId); // Refresh the settings
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to update currency: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void updateNotifications(String userId, boolean enabled) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        userSettingService.updateNotifications(userId, enabled)
                .addOnSuccessListener(aVoid -> {
                    loadUserSettings(userId); // Refresh the settings
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to update notifications: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    // Synchronous methods for backward compatibility
    
    public UserSettingModel getUserSettingsSync(String userId) {
        UserSettingModel result = currentUserSettings.getValue();
        return result != null ? result : createDefaultSettings(userId);
    }
    
    private UserSettingModel createDefaultSettings(String userId) {
        UserSettingModel settings = new UserSettingModel(userId);
        settings.setTheme("system");
        settings.setLanguage("fr");
        settings.setCurrency("MAD");
        settings.setNotifications(true);
        return settings;
    }
    
    // Get current theme preference
    public String getCurrentTheme() {
        UserSettingModel settings = currentUserSettings.getValue();
        return settings != null ? settings.getTheme() : "system";
    }
    
    // Get current language preference
    public String getCurrentLanguage() {
        UserSettingModel settings = currentUserSettings.getValue();
        return settings != null ? settings.getLanguage() : "fr";
    }
    
    // Get current currency preference
    public String getCurrentCurrency() {
        UserSettingModel settings = currentUserSettings.getValue();
        return settings != null ? settings.getCurrency() : "MAD";
    }
    
    // Get current notifications preference
    public boolean areNotificationsEnabled() {
        UserSettingModel settings = currentUserSettings.getValue();
        return settings != null ? settings.isNotifications() : true;
    }
}
