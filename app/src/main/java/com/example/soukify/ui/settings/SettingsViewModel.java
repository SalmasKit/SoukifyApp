package com.example.soukify.ui.settings;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.soukify.R;
import com.example.soukify.data.repositories.SessionRepository;
import com.example.soukify.data.repositories.UserRepository;
import com.example.soukify.data.models.UserModel;
import com.example.soukify.data.remote.CloudinaryImageService;
import android.util.Log;
import android.util.Patterns;
import android.net.Uri;

/**
 * ViewModel for SettingsFragment - Fixed version
 */
public class SettingsViewModel extends AndroidViewModel {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final com.example.soukify.data.repositories.UserSettingRepository userSettingRepository;
    private final CloudinaryImageService cloudinaryService;
    private final MutableLiveData<Integer> themeIndex = new MutableLiveData<>();
    private final MutableLiveData<UserModel> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> operationResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    private boolean isUploadingImage = false;

    public SettingsViewModel(Application application) {
        super(application);
        sessionRepository = SessionRepository.getInstance(application);
        userRepository = new UserRepository(application);
        userSettingRepository = new com.example.soukify.data.repositories.UserSettingRepository(application);
        cloudinaryService = new CloudinaryImageService(application);
        themeIndex.setValue(0);

        setupUserRepositoryObservers();
        loadCurrentUser();
    }
    
    // ... existing code ...

    public void updateDetailedNotificationPreferences(UserModel.NotificationPreferences prefs) {
        String uid = userRepository.getCurrentUserId();
        if (uid == null) {
            uid = getCurrentUserId().getValue();
        }
        
        if (uid != null) {
            userSettingRepository.updateDetailedNotifications(uid, prefs);
        }
    }

    private void setupUserRepositoryObservers() {
        userRepository.getCurrentUser().observeForever(user -> {
            if (user != null) {
                currentUser.postValue(user);
                Log.d("SettingsViewModel", "User data updated: " + user.getEmail());
            }
        });

        userRepository.getIsLoading().observeForever(loading -> {
            isLoading.postValue(loading);
        });

        userRepository.getErrorMessage().observeForever(error -> {
            if (error != null && !error.isEmpty()) {
                operationResult.postValue(error);
                Log.d("SettingsViewModel", "Error: " + error);
            }
        });

        userRepository.getSuccessMessage().observeForever(success -> {
            if (success != null && !success.isEmpty()) {
                operationResult.postValue("SUCCESS: " + success);
                Log.d("SettingsViewModel", "Success: " + success);
            }
        });
    }

    public LiveData<Integer> getThemeIndex() { return themeIndex; }
    public LiveData<String> getCurrentUserId() { return sessionRepository.getCurrentUserId(); }
    public String getUserName() { return sessionRepository.getUserName(); }
    public String getUserEmail() { return sessionRepository.getUserEmail(); }
    public boolean isLoggedIn() {
        String userId = getCurrentUserId().getValue();
        return userId != null && !userId.isEmpty();
    }

    public void setThemeIndex(int index) {
        themeIndex.setValue(index);
        applyTheme(index);
    }

    private void applyTheme(int index) {
        String theme;
        switch (index) {
            case 1:
                theme = "light";
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                theme = "dark";
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 0:
            default:
                theme = "system";
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
        
        // Update backend - only if userId is valid
        String uid = userRepository.getCurrentUserId();
        if (uid == null) {
            uid = getCurrentUserId().getValue();
        }
        
        // Only update Firebase if we have a valid userId
        if (uid != null && !uid.isEmpty() && !uid.equals("user_settings")) {
            userSettingRepository.updateTheme(uid, theme);
        } else {
            Log.w("SettingsViewModel", "Cannot update theme in Firebase: userId is null or invalid");
        }
    }

    public void logout() {
        userRepository.signOut();
        sessionRepository.logout();
        currentUser.setValue(null);
    }

    public LiveData<UserModel> getCurrentUser() { return currentUser; }
    public LiveData<String> getOperationResult() { return operationResult; }
    public void clearOperationResult() { operationResult.setValue(null); }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void refreshCurrentUser() {
        Log.d("SettingsViewModel", "Refreshing user profile");
        loadCurrentUser();
    }

    public void syncVerifiedEmail() {
        Log.d("SettingsViewModel", "Manual email sync requested");
        userRepository.syncVerifiedEmail();
    }

    private void loadCurrentUser() {
        String uid = userRepository.getCurrentUserId();
        Log.d("SettingsViewModel", "Loading user profile for ID: " + uid);
        userRepository.loadUserProfile();
    }

    /**
     * ✅ FIXED: Unified method for updating user profile
     * Handles name, phone, email changes properly
     */
    public void updateUserProfile(String name, String email, String phone, String password) {
        Log.d("SettingsViewModel", "Updating user profile - Name: " + name + ", Email: " + email + ", Phone: " + phone);

        operationResult.setValue(null);

        UserModel currentUserModel = currentUser.getValue();
        if (currentUserModel == null) {
            operationResult.setValue(getApplication().getString(R.string.error_no_user_logged_in));
            return;
        }

        // Validate inputs
        if (name == null || name.trim().isEmpty()) {
            operationResult.setValue(getApplication().getString(R.string.name_required_error));
            return;
        }

        if (email != null && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            operationResult.setValue(getApplication().getString(R.string.email_invalid_error));
            return;
        }

        if (phone == null || phone.trim().isEmpty()) {
            operationResult.setValue(getApplication().getString(R.string.phone_required_error));
            return;
        }

        // Create updated user model
        UserModel updatedUser = new UserModel(
                name,
                email != null ? email : currentUserModel.getEmail(),
                phone,
                currentUserModel.getPasswordHash()
        );
        updatedUser.setUserId(currentUserModel.getUserId());
        updatedUser.setProfileImage(currentUserModel.getProfileImage());

        // Update profile (repository handles email verification if needed)
        userRepository.updateProfile(updatedUser, email, password);
    }

    public void updateUserPassword(String currentPassword, String newPassword) {
        Log.d("SettingsViewModel", "Updating user password");
        Log.d("SettingsViewModel", "Current password length: " + (currentPassword != null ? currentPassword.length() : 0));
        Log.d("SettingsViewModel", "New password length: " + (newPassword != null ? newPassword.length() : 0));

        operationResult.setValue(null);

        // Enhanced validation
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            Log.w("SettingsViewModel", "Current password validation failed: null or empty");
            operationResult.postValue(getApplication().getString(R.string.current_password_required));
            return;
        }

        if (currentPassword.length() < 6) {
            Log.w("SettingsViewModel", "Current password validation failed: too short");
            operationResult.postValue(getApplication().getString(R.string.current_password_short));
            return;
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            Log.w("SettingsViewModel", "New password validation failed: null or empty");
            operationResult.postValue(getApplication().getString(R.string.new_password_required));
            return;
        }

        if (newPassword.length() < 6) {
            Log.w("SettingsViewModel", "New password validation failed: too short");
            operationResult.postValue(getApplication().getString(R.string.new_password_length));
            return;
        }

        if (newPassword.equals(currentPassword)) {
            Log.w("SettingsViewModel", "New password validation failed: same as current");
            operationResult.postValue(getApplication().getString(R.string.new_password_different));
            return;
        }

        Log.d("SettingsViewModel", "Password validation passed, calling repository update");
        userRepository.updatePassword(currentPassword, newPassword);
    }

    /**
     * ✅ FIXED: Simplified image upload
     * Upload image and update user profile in Firestore
     */
    public void updateProfileImage(String imageUri) {
        Log.d("SettingsViewModel", "Updating profile image");

        if (isUploadingImage) {
            Log.d("SettingsViewModel", "Image upload already in progress");
            return;
        }

        UserModel user = currentUser.getValue();
        if (user == null) {
            operationResult.postValue(getApplication().getString(R.string.error_no_user_data));
            return;
        }

        if (imageUri == null || imageUri.isEmpty()) {
            operationResult.postValue(getApplication().getString(R.string.error_no_image_selected));
            return;
        }

        isUploadingImage = true;
        isLoading.postValue(true);

        Log.d("SettingsViewModel", "Uploading profile image for user " + user.getUserId());

        String publicId = CloudinaryImageService.generateUniquePublicId("profile", user.getUserId());

        cloudinaryService.uploadMedia(Uri.parse(imageUri), publicId, new CloudinaryImageService.MediaUploadCallback() {
            @Override
            public void onSuccess(String mediaUrl) {
                Log.d("SettingsViewModel", "✅ Profile image uploaded successfully: " + mediaUrl);

                // ✅ FIX: Verify user ID before update and SELF-HEAL if corrupted
                String currentUid = userRepository.getCurrentUserId();
                if (currentUid == null) {
                    Log.e("SettingsViewModel", "CRITICAL: No authenticated user found during upload.");
                    operationResult.postValue(getApplication().getString(R.string.error_not_logged_in));
                    isUploadingImage = false;
                    isLoading.postValue(false);
                    return;
                }

                if (!currentUid.equals(user.getUserId())) {
                    Log.w("SettingsViewModel", "ID Mismatch detected. Repairing... Auth: " + currentUid + ", Model: " + user.getUserId());
                    // We continue, using currentUid as the source of truth
                }

                // ✅ FIX: Update in Firestore IMMEDIATELY using the AUTH ID
                 // Update user model with new image URL
                UserModel updatedUser = new UserModel(
                        user.getFullName(),
                        user.getEmail(),
                        user.getPhoneNumber(),
                        user.getPasswordHash()
                );
                updatedUser.setUserId(currentUid); // FORCE CORRECT ID
                updatedUser.setProfileImage(mediaUrl);

                userRepository.updateProfile(updatedUser, null, null);

                // Delete old image in background (non-blocking) - don't wait for this
                if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                    String oldPublicId = extractPublicIdFromUrl(user.getProfileImage());
                    if (oldPublicId != null && !oldPublicId.equals(publicId)) {
                        newThreadToDeleteImage(oldPublicId);
                    }
                }

                isUploadingImage = false;
                isLoading.postValue(false);
                Log.d("SettingsViewModel", "✅ Image update complete, LiveData will be updated by repository");
            }

            @Override
            public void onError(String error) {
                Log.e("SettingsViewModel", "❌ Failed to upload profile image: " + error);
                operationResult.postValue(getApplication().getString(R.string.error_upload_failed, error));
                isUploadingImage = false;
                isLoading.postValue(false);
            }

            private void newThreadToDeleteImage(String oldPublicId) {
                new Thread(() -> {
                    cloudinaryService.deleteMedia(oldPublicId, new CloudinaryImageService.MediaDeleteCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d("SettingsViewModel", "Old profile image deleted");
                        }

                        @Override
                        public void onError(String error) {
                            Log.w("SettingsViewModel", "Failed to delete old image: " + error);
                        }
                    });
                }).start();
            }

            @Override
            public void onProgress(int progress) {
                Log.d("SettingsViewModel", "📤 Upload progress: " + progress + "%");
                // You could add a progress LiveData here if you want to show a progress bar
            }
        });
    }

    /**
     * Complete email change after user has verified
     */
    public void completeEmailChange() {
        Log.d("SettingsViewModel", "Completing email change process");
        operationResult.setValue(null);
        userRepository.completeEmailChange();
    }

    public void updateNotificationPreferences(UserModel.NotificationPreferences prefs) {
        userRepository.updateNotificationPreferences(prefs);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (userRepository != null) {
            userRepository.cleanup();
        }
        Log.d("SettingsViewModel", "ViewModel cleared and repository cleaned up");
    }

    private String extractPublicIdFromUrl(String cloudinaryUrl) {
        if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
            return null;
        }

        try {
            // Extract public ID from Cloudinary URL
            // URL format: https://res.cloudinary.com/cloud_name/image/upload/v1234567890/folder/public_id.extension
            String[] parts = cloudinaryUrl.split("/");
            if (parts.length >= 2) {
                String lastPart = parts[parts.length - 1];

                // Remove file extension
                int dotIndex = lastPart.lastIndexOf('.');
                if (dotIndex > 0) {
                    String publicId = lastPart.substring(0, dotIndex);

                    // Include folder if exists (second to last part might be folder)
                    if (parts.length >= 3 && !parts[parts.length - 2].matches("v\\d+")) {
                        publicId = parts[parts.length - 2] + "/" + publicId;
                    }

                    return publicId;
                }
                return lastPart;
            }
        } catch (Exception e) {
            Log.w("SettingsViewModel", "Failed to extract public ID from URL: " + cloudinaryUrl, e);
        }

        return null;
    }
}