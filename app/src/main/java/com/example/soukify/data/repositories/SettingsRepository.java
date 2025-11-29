package com.example.soukify.data.repositories;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.models.UserModel;

/**
 * Settings Repository - SharedPreferences/Firebase implementation
 * Follows MVVM pattern by abstracting data operations from ViewModels
 */
public class SettingsRepository {
    private final SharedPreferences sharedPreferences;
    private final FirebaseManager firebaseManager;
    private final MutableLiveData<String> language = new MutableLiveData<>();
    private final MutableLiveData<String> currency = new MutableLiveData<>();
    private final MutableLiveData<Boolean> notificationsEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> darkMode = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    private static final String PREFS_NAME = "soukify_settings";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_CURRENCY = "currency";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_DARK_MODE = "dark_mode";
    
    // Default values
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String DEFAULT_CURRENCY = "MAD";
    private static final boolean DEFAULT_NOTIFICATIONS = true;
    private static final boolean DEFAULT_DARK_MODE = false;
    
    public SettingsRepository(Application application) {
        this.sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firebaseManager = FirebaseManager.getInstance(application);
        loadSettings();
    }
    
    public LiveData<String> getLanguage() {
        return language;
    }
    
    public LiveData<String> getCurrency() {
        return currency;
    }
    
    public LiveData<Boolean> getNotificationsEnabled() {
        return notificationsEnabled;
    }
    
    public LiveData<Boolean> getDarkMode() {
        return darkMode;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    private void loadSettings() {
        language.setValue(sharedPreferences.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE));
        currency.setValue(sharedPreferences.getString(KEY_CURRENCY, DEFAULT_CURRENCY));
        notificationsEnabled.setValue(sharedPreferences.getBoolean(KEY_NOTIFICATIONS, DEFAULT_NOTIFICATIONS));
        darkMode.setValue(sharedPreferences.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE));
    }
    
    public void setLanguage(String lang) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LANGUAGE, lang);
        editor.apply();
        language.setValue(lang);
    }
    
    public void setCurrency(String curr) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CURRENCY, curr);
        editor.apply();
        currency.setValue(curr);
    }
    
    public void setNotificationsEnabled(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_NOTIFICATIONS, enabled);
        editor.apply();
        notificationsEnabled.setValue(enabled);
    }
    
    public void setDarkMode(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_DARK_MODE, enabled);
        editor.apply();
        darkMode.setValue(enabled);
    }
    
    public void resetToDefaults() {
        setLanguage(DEFAULT_LANGUAGE);
        setCurrency(DEFAULT_CURRENCY);
        setNotificationsEnabled(DEFAULT_NOTIFICATIONS);
        setDarkMode(DEFAULT_DARK_MODE);
    }
    
    // User profile settings (stored in Firebase)
    public void updateUserProfile(UserModel user) {
        if (firebaseManager.isUserLoggedIn()) {
            // This would use UserRepository in a real implementation
            // For now, just emit an error message
            errorMessage.setValue("User profile update not implemented in SettingsRepository");
        }
    }
    
    public void deleteUserAccount() {
        if (firebaseManager.isUserLoggedIn()) {
            // This would use UserRepository in a real implementation
            // For now, just emit an error message
            errorMessage.setValue("Account deletion not implemented in SettingsRepository");
        }
    }
    
    public void clearAllSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        loadSettings();
    }
}
