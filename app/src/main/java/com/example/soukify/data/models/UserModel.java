package com.example.soukify.data.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * User Model - Firebase POJO for user data
 * Used for Firestore serialization and follows MVVM pattern
 */
public class UserModel {
    private String userId;
    private String fullName;
    private String email;
    private String passwordHash;
    private String phoneNumber;
    private String profileImage;
    private String createdAt;
    
    // Date formatter for consistent date format
    private static final String DATE_FORMAT = "dd/MM/yyyy HH:mm";
    
    private String fcmToken;
    
    // Default constructor required for Firestore
    public UserModel() {}
    
    public UserModel(String fullName, String email, String phoneNumber, String passwordHash) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber != null ? phoneNumber : "";
        this.passwordHash = passwordHash;
        this.createdAt = formatCurrentDate();
    }
    
    public UserModel(String fullName, String email, String phoneNumber) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber != null ? phoneNumber : "";
        this.passwordHash = null;
        this.createdAt = formatCurrentDate();
    }
    
    // Helper method to format current date consistently
    private String formatCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return sdf.format(new Date());
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getProfileImage() {
        return profileImage;
    }
    
    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    private NotificationPreferences notificationPreferences;

    public NotificationPreferences getNotificationPreferences() {
        return notificationPreferences;
    }

    public void setNotificationPreferences(NotificationPreferences notificationPreferences) {
        this.notificationPreferences = notificationPreferences;
    }

    public static class NotificationPreferences {
        // General
        public boolean push = true;
        public boolean email = true;
        public boolean sound = true;
        public boolean vibrate = true;
        
        // Categories
        public boolean messages = true;
        public boolean newProducts = true;
        public boolean shopPromotions = true;
        public boolean promotions = true;
        public boolean appUpdates = true;
        
        // Quiet hours
        public int quietStartHour = 22;
        public int quietStartMinute = 0;
        public int quietEndHour = 7;
        public int quietEndMinute = 0;
        
        public NotificationPreferences() {}
    }
}
