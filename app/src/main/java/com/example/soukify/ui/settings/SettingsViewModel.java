package com.example.soukify.ui.settings;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.Observer;

import com.example.soukify.data.repositories.SessionRepository;
import com.example.soukify.data.repositories.UserRepository;
import com.example.soukify.data.models.UserModel;
import com.example.soukify.utils.ImageUtils;
import java.util.List;

import android.util.Log;
import android.util.Patterns;
import android.net.Uri;

/**
 * ViewModel for SettingsFragment that manages theme settings and user information
 */
public class SettingsViewModel extends AndroidViewModel {
    
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<Integer> themeIndex = new MutableLiveData<>();
    private final MutableLiveData<UserModel> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> operationResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    
    public SettingsViewModel(Application application) {
        super(application);
        sessionRepository = SessionRepository.getInstance(application);
        userRepository = new UserRepository(application);
        themeIndex.setValue(0);
        
        setupUserRepositoryObservers();
        loadCurrentUser();
    }
    
    private void setupUserRepositoryObservers() {
        userRepository.getCurrentUser().observeForever(user -> {
            if (user != null) {
                currentUser.postValue(user);
                android.util.Log.d("SettingsViewModel", "User data updated: " + user.getEmail());
                android.util.Log.d("SettingsViewModel", "Profile image: " + user.getProfileImage());
            }
        });
        
        userRepository.getIsLoading().observeForever(loading -> {
            isLoading.postValue(loading);
        });
        
        userRepository.getErrorMessage().observeForever(error -> {
            if (error != null && !error.isEmpty()) {
                operationResult.postValue(error);
                android.util.Log.d("SettingsViewModel", "Operation result: " + error);
            }
        });
    }
    
    public LiveData<Integer> getThemeIndex() {
        return themeIndex;
    }
    
    public LiveData<String> getCurrentUserId() {
        return sessionRepository.getCurrentUserId();
    }
    
    public String getUserName() {
        return sessionRepository.getUserName();
    }
    
    public String getUserEmail() {
        return sessionRepository.getUserEmail();
    }
    
    public boolean isLoggedIn() {
        String userId = getCurrentUserId().getValue();
        return userId != null && !userId.isEmpty();
    }
    
    public void setThemeIndex(int index) {
        themeIndex.setValue(index);
        applyTheme(index);
    }
    
    private void applyTheme(int index) {
        switch (index) {
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 0:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
    
    public void logout() {
        userRepository.signOut();
        sessionRepository.logout();
    }
    
    public LiveData<UserModel> getCurrentUser() {
        return currentUser;
    }
    
    public LiveData<String> getOperationResult() {
        return operationResult;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public void refreshCurrentUser() {
        android.util.Log.d("SettingsViewModel", "Refreshing user profile");
        loadCurrentUser();
    }
    
    private void loadCurrentUser() {
        android.util.Log.d("SettingsViewModel", "Loading user profile");
        userRepository.loadUserProfile();
    }
    
    public void updateUserProfile(String name, String email, String phone) {
        android.util.Log.d("SettingsViewModel", "Updating user profile - Name: " + name + ", Email: " + email + ", Phone: " + phone);
        
        operationResult.setValue(null);
        
        UserModel user = currentUser.getValue();
        if (user == null) {
            operationResult.postValue("No user data available");
            return;
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            operationResult.postValue("Invalid email format");
            return;
        }
        
        UserModel updatedUser = new UserModel(name, email, phone, user.getPasswordHash());
        updatedUser.setUserId(user.getUserId());
        updatedUser.setProfileImage(user.getProfileImage());
        
        userRepository.updateProfile(updatedUser);
    }
    
    /**
     * Updates user profile with email change (requires password for re-authentication)
     * This will send a verification email to the new address
     * Email will only be updated in Firestore after user clicks verification link
     */
    public void updateUserProfileWithEmail(String name, String email, String phone, String password) {
        android.util.Log.d("SettingsViewModel", "Updating user profile with email change");
        android.util.Log.d("SettingsViewModel", "New email: " + email + " (verification required)");
        
        operationResult.setValue(null);
        
        UserModel user = currentUser.getValue();
        if (user == null) {
            operationResult.postValue("No user data available");
            return;
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            operationResult.postValue("Invalid email format");
            return;
        }
        
        if (password == null || password.trim().isEmpty()) {
            operationResult.postValue("Password is required to change email");
            return;
        }
        
        if (password.length() < 6) {
            operationResult.postValue("Password must be at least 6 characters");
            return;
        }
        
        UserModel updatedUser = new UserModel(name, email, phone, user.getPasswordHash());
        updatedUser.setUserId(user.getUserId());
        updatedUser.setProfileImage(user.getProfileImage());
        
        android.util.Log.d("SettingsViewModel", "Profile image being sent: " + user.getProfileImage());
        
        userRepository.updateProfileWithEmail(updatedUser, email, password);
    }
    
    /**
     * Complete email change after user has clicked verification link
     * This syncs the verified email from Firebase Auth to Firestore
     * Should be called automatically when user returns to app after verification
     */
    public void completeEmailChange() {
        android.util.Log.d("SettingsViewModel", "Completing email change process");
        operationResult.setValue(null);
        userRepository.completeEmailChange();
    }
    
    public void updateUserPassword(String currentPassword, String newPassword) {
        android.util.Log.d("SettingsViewModel", "Updating user password");
        
        operationResult.setValue(null);
        
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            operationResult.postValue("Current password is required");
            return;
        }
        
        if (newPassword == null || newPassword.length() < 6) {
            operationResult.postValue("New password must be at least 6 characters");
            return;
        }
        
        userRepository.updatePassword(currentPassword, newPassword);
    }
    
    public void updateProfileImage(String imageUri) {
        android.util.Log.d("SettingsViewModel", "Updating profile image");
        
        operationResult.setValue(null);
        
        UserModel user = currentUser.getValue();
        if (user == null) {
            operationResult.postValue("No user data available");
            return;
        }
        
        Log.d("SettingsViewModel", "Updating profile image for user " + user.getUserId() + " with URI: " + imageUri);
        
        if (imageUri != null && !imageUri.isEmpty()) {
            // Upload to Firebase Storage
            ImageUtils.uploadImageToFirebaseStorage(getApplication(), Uri.parse(imageUri), "profile", user.getUserId())
                .addOnSuccessListener(uri -> {
                    Log.d("SettingsViewModel", "Profile image uploaded to Firebase Storage: " + uri.toString());
                    
                    // Delete old image from Firebase Storage if it exists
                    if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                        ImageUtils.deleteImageFromFirebaseStorage("profile", user.getUserId());
                    }
                    
                    UserModel updatedUser = new UserModel(
                        user.getFullName(),
                        user.getEmail(),
                        user.getPhoneNumber(),
                        user.getPasswordHash()
                    );
                    updatedUser.setUserId(user.getUserId());
                    updatedUser.setProfileImage(uri.toString());
                    
                    userRepository.updateProfile(updatedUser);
                })
                .addOnFailureListener(e -> {
                    Log.e("SettingsViewModel", "Failed to upload profile image to Firebase Storage", e);
                    operationResult.postValue("Failed to upload image: " + e.getMessage());
                });
        } else {
            // No image provided, clear existing image
            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                ImageUtils.deleteImageFromFirebaseStorage("profile", user.getUserId());
            }
            
            UserModel updatedUser = new UserModel(
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getPasswordHash()
            );
            updatedUser.setUserId(user.getUserId());
            updatedUser.setProfileImage(null);
            
            userRepository.updateProfile(updatedUser);
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        android.util.Log.d("SettingsViewModel", "ViewModel cleared");
    }
}
