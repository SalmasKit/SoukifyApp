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
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
