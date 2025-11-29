package com.example.soukify.data.models;

/**
 * User Setting Model - Firebase model for user settings
 * POJO class for Firebase Firestore
 */
public class UserSettingModel {
    private String userId;
    private String theme;
    private String language;
    private String currency;
    private boolean notifications;
    
    // Default constructor for Firebase
    public UserSettingModel() {}
    
    public UserSettingModel(String userId) {
        this.userId = userId;
        this.theme = "system";
        this.language = "fr";
        this.currency = "MAD";
        this.notifications = true;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getTheme() {
        return theme;
    }
    
    public void setTheme(String theme) {
        this.theme = theme;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public boolean isNotifications() {
        return notifications;
    }
    
    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
    }
}
