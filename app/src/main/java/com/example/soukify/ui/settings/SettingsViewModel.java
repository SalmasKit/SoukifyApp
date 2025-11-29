package com.example.soukify.ui.settings;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.appcompat.app.AppCompatDelegate;

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
        // Initialize with default values
        themeIndex.setValue(0);
        loadCurrentUser();
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
    
    // Account Profile Management Methods
    
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
        loadCurrentUser();
    }
    
    private void loadCurrentUser() {
        isLoading.setValue(true);
        
        // Use Firebase UserRepository to load current user
        userRepository.loadUserProfile();
        
        // Observe the result
        userRepository.getCurrentUser().observeForever(user -> {
            if (user != null) {
                currentUser.postValue(user);
                operationResult.postValue("User data loaded");
            } else {
                currentUser.postValue(null);
                operationResult.postValue("No user data found");
            }
            isLoading.postValue(false);
        });
        
        userRepository.getErrorMessage().observeForever(error -> {
            if (error != null) {
                operationResult.postValue(error);
                isLoading.postValue(false);
            }
        });
    }
    
    public void updateUserProfile(String name, String email, String phone) {
        isLoading.setValue(true);
        operationResult.setValue(null);
        
        UserModel user = currentUser.getValue();
        if (user == null) {
            operationResult.postValue("No user data available");
            isLoading.postValue(false);
            return;
        }
        
        // Validate email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            operationResult.postValue("Invalid email format");
            isLoading.postValue(false);
            return;
        }
        
        // Update user data
        user.setFullName(name);
        user.setEmail(email);
        user.setPhoneNumber(phone);
        
        // Use Firebase UserRepository to update profile
        userRepository.updateProfile(user);
        
        // Observe the result
        userRepository.getErrorMessage().observeForever(error -> {
            if (error != null) {
                operationResult.postValue(error);
                isLoading.postValue(false);
            }
        });
        
        userRepository.getIsLoading().observeForever(loading -> {
            if (!loading) {
                // Refresh user data after update
                loadCurrentUser();
            }
        });
    }
    
    /**
     * Updates user profile with email change (requires password for re-authentication)
     */
    public void updateUserProfileWithEmail(String name, String email, String phone, String password) {
        isLoading.setValue(true);
        operationResult.setValue(null);
        
        UserModel user = currentUser.getValue();
        if (user == null) {
            operationResult.postValue("No user data available");
            isLoading.postValue(false);
            return;
        }
        
        // Validate email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            operationResult.postValue("Invalid email format");
            isLoading.postValue(false);
            return;
        }
        
        // Update user data
        user.setFullName(name);
        user.setEmail(email);
        user.setPhoneNumber(phone);
        
        // Use Firebase UserRepository to update profile with email change
        userRepository.updateProfileWithEmail(user, email, password);
        android.util.Log.d("SettingsViewModel", "Called updateProfileWithEmail");
        
        // Observe the result
        userRepository.getErrorMessage().observeForever(error -> {
            if (error != null) {
                android.util.Log.e("SettingsViewModel", "Error from UserRepository: " + error);
                operationResult.postValue(error);
                isLoading.postValue(false);
            }
        });
        
        userRepository.getIsLoading().observeForever(loading -> {
            if (!loading) {
                android.util.Log.d("SettingsViewModel", "Loading completed, refreshing user data");
                // Refresh user data after update
                loadCurrentUser();
            }
        });
        
        // Also observe success
        userRepository.getCurrentUser().observeForever(updatedUser -> {
            if (updatedUser != null && updatedUser.getEmail().equals(email)) {
                android.util.Log.d("SettingsViewModel", "Email update successful: " + updatedUser.getEmail());
                operationResult.postValue("Profile updated successfully!");
                isLoading.postValue(false);
            }
        });
    }
    
    public void updateUserPassword(String currentPassword, String newPassword) {
        isLoading.setValue(true);
        operationResult.setValue(null);
        
        // Validate new password
        if (newPassword.length() < 6) {
            operationResult.postValue("Password must be at least 6 characters");
            isLoading.postValue(false);
            return;
        }
        
        // Firebase handles password updates differently - use reset password
        UserModel user = currentUser.getValue();
        if (user != null && user.getEmail() != null) {
            userRepository.resetPassword(user.getEmail());
            
            userRepository.getErrorMessage().observeForever(error -> {
                if (error != null) {
                    operationResult.postValue("Password reset failed: " + error);
                    isLoading.postValue(false);
                }
            });
            
            operationResult.postValue("Password reset email sent");
            isLoading.postValue(false);
        } else {
            operationResult.postValue("No user email available");
            isLoading.postValue(false);
        }
    }
    
    public void updateProfileImage(String imageUri) {
        isLoading.setValue(true);
        operationResult.setValue(null);
        
        UserModel user = currentUser.getValue();
        if (user == null) {
            operationResult.postValue("No user data available");
            isLoading.postValue(false);
            return;
        }
        
        Log.d("SettingsViewModel", "Updating profile image for user " + user.getUserId() + " with URI: " + imageUri);
        
        // Copy image to internal storage for persistence
        String copiedImageUri = imageUri;
        if (imageUri != null && !imageUri.isEmpty()) {
            String fileName = ImageUtils.createUniqueFileName("profile", user.getUserId().hashCode(), 0);
            copiedImageUri = ImageUtils.copyImageToInternalStorage(getApplication(), Uri.parse(imageUri), "profile", fileName);
            
            if (copiedImageUri == null) {
                operationResult.postValue("Failed to copy image to internal storage");
                isLoading.postValue(false);
                return;
            }
            
            // Delete old image if it exists
            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                ImageUtils.deleteImageFromInternalStorage(getApplication(), user.getProfileImage());
            }
            
            Log.d("SettingsViewModel", "Image copied to internal storage: " + copiedImageUri);
        }
        
        // Update user profile image
        user.setProfileImage(copiedImageUri);
        
        // Use Firebase UserRepository to update profile
        userRepository.updateProfile(user);
        
        // Observe the result
        userRepository.getErrorMessage().observeForever(error -> {
            if (error != null) {
                operationResult.postValue("Error updating profile image: " + error);
                isLoading.postValue(false);
            }
        });
        
        userRepository.getIsLoading().observeForever(loading -> {
            if (!loading) {
                operationResult.postValue("Profile image updated");
                isLoading.postValue(false);
            }
        });
    }
}
