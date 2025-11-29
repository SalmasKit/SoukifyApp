package com.example.soukify.data.repositories;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseUserService;
import com.example.soukify.data.models.UserModel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * User Repository - Firebase implementation
 * Follows MVVM pattern by abstracting data operations from ViewModels
 */
public class UserRepository {
    private final FirebaseUserService userService;
    private final MutableLiveData<UserModel> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final Application application;
    
    public UserRepository(Application application) {
        this.application = application;
        FirebaseManager firebaseManager = FirebaseManager.getInstance(application);
        this.userService = new FirebaseUserService(firebaseManager.getAuth(), firebaseManager.getFirestore());
    }
    
    public LiveData<UserModel> getCurrentUser() {
        return currentUser;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public void signIn(String email, String password) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        // Standard Firebase sign in with unique email
        userService.signIn(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() != null) {
                        loadUserProfile(authResult.getUser().getUid());
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Sign in failed: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void signUp(String fullName, String email, String password, String phoneNumber) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        // Standard Firebase sign up with unique email enforcement
        userService.signUp(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() != null) {
                        String passwordHash = hashPassword(password);
                        UserModel user = new UserModel(fullName, email, phoneNumber, passwordHash);
                        user.setUserId(authResult.getUser().getUid());
                        
                        userService.createUser(user)
                                .addOnSuccessListener(aVoid -> {
                                    currentUser.postValue(user);
                                    isLoading.postValue(false);
                                })
                                .addOnFailureListener(e -> {
                                    errorMessage.postValue("Failed to create user profile: " + e.getMessage());
                                    isLoading.postValue(false);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Sign up failed: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    // Helper method to hash passwords using SHA-256
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return null; // Fallback to null if hashing fails
        }
    }
    
    public void signOut() {
        userService.signOut();
        currentUser.setValue(null);
        errorMessage.setValue(null);
    }
    
    public void resetPassword(String email) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        userService.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    errorMessage.postValue("Password reset email sent");
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to send password reset email: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void updateProfile(UserModel user) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        userService.updateUser(user)
                .addOnSuccessListener(aVoid -> {
                    currentUser.postValue(user);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to update profile: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    /**
     * Updates both the user profile in Firestore and the authentication email
     * @param user The updated user model
     * @param newEmail The new email for authentication (if different from current)
     * @param password Current password for email re-authentication (required for email change)
     */
    public void updateProfileWithEmail(UserModel user, String newEmail, String password) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        String currentEmail = userService.getCurrentUserEmail();
        android.util.Log.d("UserRepository", "Current email: " + currentEmail + ", New email: " + newEmail);
        
        // Check if email needs to be changed
        if (newEmail != null && !newEmail.equals(currentEmail)) {
            android.util.Log.d("UserRepository", "Email change detected, starting re-authentication");
            // Re-authenticate user first (required for email change)
            userService.reauthenticate(password)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("UserRepository", "Re-authentication successful, updating email");
                        // Update authentication email
                        userService.updateEmail(newEmail)
                                .addOnSuccessListener(emailUpdateResult -> {
                                    android.util.Log.d("UserRepository", "Firebase Auth email updated successfully");
                                    // Update Firestore profile
                                    userService.updateUser(user)
                                            .addOnSuccessListener(profileUpdateResult -> {
                                                android.util.Log.d("UserRepository", "Firestore profile updated successfully");
                                                // Update SessionRepository with new email and name
                                                SessionRepository.getInstance(application).updateUserEmail(newEmail, user.getFullName());
                                                
                                                currentUser.postValue(user);
                                                isLoading.postValue(false);
                                                android.util.Log.d("UserRepository", "Profile and email updated successfully");
                                            })
                                            .addOnFailureListener(e -> {
                                                android.util.Log.e("UserRepository", "Profile update failed after email change", e);
                                                errorMessage.postValue("Profile update failed after email change: " + e.getMessage());
                                                isLoading.postValue(false);
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("UserRepository", "Failed to update authentication email", e);
                                    errorMessage.postValue("Failed to update authentication email: " + e.getMessage());
                                    isLoading.postValue(false);
                                });
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("UserRepository", "Re-authentication failed", e);
                        errorMessage.postValue("Re-authentication failed. Please check your password: " + e.getMessage());
                        isLoading.postValue(false);
                    });
        } else {
            android.util.Log.d("UserRepository", "No email change needed, updating profile only");
            // No email change, just update profile
            updateProfile(user);
        }
    }
    
    public void loadUserProfile() {
        String userId = userService.getCurrentUserId();
        if (userId != null) {
            loadUserProfile(userId);
        }
    }
    
    private void loadUserProfile(String userId) {
        userService.getUser(userId)
                .addOnSuccessListener(user -> {
                    currentUser.postValue(user);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load user profile: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public boolean isUserLoggedIn() {
        return userService.isUserLoggedIn();
    }
    
    public String getCurrentUserId() {
        return userService.getCurrentUserId();
    }
}
