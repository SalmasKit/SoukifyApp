package com.example.soukify.data.repositories;

import android.app.Activity;
import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.soukify.data.models.UserModel;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseUserService;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.soukify.R;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * ===== User Repository - FIXED VERSION =====
 */
public class UserRepository {

    private final FirebaseUserService userService;
    private final Application application;
    private final FirebaseAuth.AuthStateListener authStateListener;

    private final MutableLiveData<UserModel> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    
    private volatile boolean isUpdating = false;

    public UserRepository(Application application) {
        this.application = application;
        FirebaseManager manager = FirebaseManager.getInstance(application);
        this.userService = new FirebaseUserService(
                manager.getAuth(),
                manager.getFirestore()
        );
        
        // Initialize auth state listener for automatic email verification detection
        this.authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null && user.isEmailVerified()) {
                Log.d("UserRepository", "Auth state changed: Email verified detected for " + user.getEmail());
                checkAndSyncVerifiedEmail(user);
            }
        };
        
        // Add auth state listener
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    /* ===================== GETTERS ===================== */

    public LiveData<UserModel> getCurrentUser() { return currentUser; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public String getCurrentUserEmail() { return userService.getCurrentUserEmail(); }
    public String getCurrentUserId() { return userService.getCurrentUserId(); }
    public boolean isUserLoggedIn() { return userService.isUserLoggedIn(); }

    /* ===================== AUTH EMAIL ===================== */

    public void signIn(String email, String password) {
        startLoading();

        userService.signIn(email, password)
                .addOnSuccessListener(auth -> {
                    FirebaseUser fUser = auth.getUser();
                    if (fUser != null) loadUserProfile(fUser.getUid());
                })
                .addOnFailureListener(e -> fail(application.getString(R.string.login_failed, e.getMessage())));
    }

    public void signUp(String fullName, String email, String password, String phone) {
        startLoading();

        userService.signUp(email, password)
                .addOnSuccessListener(auth -> {
                    FirebaseUser firebaseUser = auth.getUser();
                    if (firebaseUser == null) {
                        fail(application.getString(R.string.signup_failed, "User creation failed"));
                        return;
                    }

                    String uid = firebaseUser.getUid();
                    String hash = hashPassword(password);

                    UserModel user = new UserModel(fullName, email, phone, hash);
                    user.setUserId(uid);

                    userService.createUser(user)
                            .addOnSuccessListener(aVoid -> {
                                currentUser.postValue(user);
                                stopLoading();
                            })
                            .addOnFailureListener(e ->
                                    fail(application.getString(R.string.profile_creation_failed, e.getMessage())));
                })
                .addOnFailureListener(e -> fail(application.getString(R.string.signup_failed, e.getMessage())));
    }

    public void signOut() {
        // Remove auth state listener to prevent memory leaks
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        
        userService.signOut();
        currentUser.postValue(null);
        successMessage.postValue(application.getString(R.string.signed_out_success));
    }

    public Task<Void> reauthenticate(String email, String password) {
        return userService.reauthenticate(email, password);
    }

    /* ===================== PASSWORD ===================== */

    public void resetPassword(String email) {
        startLoading();

        userService.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    successMessage.postValue(application.getString(R.string.reset_email_sent));
                    stopLoading();
                })
                .addOnFailureListener(e ->
                        fail(application.getString(R.string.reset_error_msg, e.getMessage())));
    }

    public void updatePassword(String currentPassword, String newPassword) {
        startLoading();

        String email = userService.getCurrentUserEmail();
        if (email == null) {
            Log.e("UserRepository", "No authenticated user found");
            fail(application.getString(R.string.no_user_auth));
            return;
        }

        Log.d("UserRepository", "Starting password update for email: " + email);

        userService.reauthenticate(email, currentPassword)
                .addOnSuccessListener(aVoid -> {
                    Log.d("UserRepository", "Reauthentication successful, updating password");
                    userService.updatePassword(newPassword)
                            .addOnSuccessListener(v -> {
                                Log.d("UserRepository", "Password update successful");
                                successMessage.postValue(application.getString(R.string.password_update_success));
                                stopLoading();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("UserRepository", "Password update failed: " + e.getMessage());
                                fail(application.getString(R.string.password_update_failed, e.getMessage()));
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("UserRepository", "Reauthentication failed: " + e.getMessage());
                    fail(application.getString(R.string.current_password_incorrect, e.getMessage()));
                });
    }

    /* ===================== PROFILE UPDATE ===================== */

    /**
     * ✅ FIXED: Main update method
     * - Updates name, phone, image IMMEDIATELY in Firestore
     * - Email change sends verification email (verifyBeforeUpdateEmail)
     * - Email in Firestore stays OLD until user verifies
     */
    public void updateProfile(UserModel user, String newEmail, String password) {
        if (isUpdating) {
            Log.d("UserRepository", "Update already in progress, skipping");
            return;
        }
        
        isUpdating = true;
        startLoading();
        
        String currentEmail = userService.getCurrentUserEmail();
        FirebaseUser firebaseUser = userService.getCurrentUser();

        Log.d("UserRepository", "Updating profile - Name: " + user.getFullName() + 
              ", Email: " + newEmail + ", Phone: " + user.getPhoneNumber());

        // Check if there's a pending email verification
        if (firebaseUser != null && firebaseUser.getEmail() != null && 
            !firebaseUser.getEmail().equals(currentEmail)) {
            fail(application.getString(R.string.verification_pending));
            isUpdating = false;
            return;
        }

        // Update name, phone, and profile image immediately
        updateProfileFields(user, currentEmail);
        
        // Handle email change separately
        if (newEmail != null && !newEmail.equals(currentEmail)) {
            updateEmailOnly(newEmail, password);
        } else {
            Log.d("UserRepository", "No email change, profile update complete");
            successMessage.postValue(application.getString(R.string.profile_update_success));
            stopLoading();
            isUpdating = false;
        }
    }
    
    /**
     * ✅ FIXED: Update profile fields immediately
     */
    private void updateProfileFields(UserModel user, String email) {
        Log.d("UserRepository", "Updating profile fields - Name: " + user.getFullName() + ", Phone: " + user.getPhoneNumber());
        
        UserModel updatedUser = new UserModel(
            user.getFullName(),
            email, // Current email (not new one yet)
            user.getPhoneNumber(),
            user.getPasswordHash()
        );
        updatedUser.setUserId(user.getUserId());
        updatedUser.setProfileImage(user.getProfileImage());
        
        userService.updateUser(updatedUser)
            .addOnSuccessListener(aVoid -> {
                Log.d("UserRepository", "Profile fields updated successfully in Firestore");
                currentUser.postValue(updatedUser);
            })
            .addOnFailureListener(e -> {
                Log.e("UserRepository", "Failed to update profile fields", e);
                fail(application.getString(R.string.profile_update_failed, e.getMessage()));
                isUpdating = false;
            });
    }
    
    /**
     * ✅ FIXED: Complete email update method with proper verification flow
     */
    private void updateEmailOnly(String newEmail, String password) {
        Log.d("UserRepository", "Updating email to: " + newEmail);
        
        String currentEmail = userService.getCurrentUserEmail();
        FirebaseUser firebaseUser = userService.getCurrentUser();
        
        if (firebaseUser == null) {
            fail(application.getString(R.string.no_user_auth));
            isUpdating = false;
            return;
        }

        // Re-authenticate if password provided
        Task<Void> authTask = (password != null && !password.isEmpty()) 
            ? userService.reauthenticate(currentEmail, password)
            : com.google.android.gms.tasks.Tasks.forResult(null);

        authTask.addOnSuccessListener(aVoid -> {
            Log.d("UserRepository", "Re-authentication successful, sending verification email");
            
            // Use verifyBeforeUpdateEmail to send verification
            firebaseUser.verifyBeforeUpdateEmail(newEmail)
                    .addOnSuccessListener(done -> {
                        Log.d("UserRepository", "Email verification request sent successfully");
                        successMessage.postValue(application.getString(R.string.verification_email_sent, newEmail));
                        stopLoading();
                        isUpdating = false;
                    })
                    .addOnFailureListener(e -> {
                        Log.e("UserRepository", "Email update failed", e);
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && errorMsg.contains("already in use")) {
                            fail(application.getString(R.string.email_already_registered));
                        } else if (errorMsg != null && errorMsg.contains("invalid")) {
                            fail(application.getString(R.string.email_invalid_format));
                        } else {
                            fail(application.getString(R.string.profile_update_failed, errorMsg));
                        }
                        isUpdating = false;
                    });
        }).addOnFailureListener(e -> {
            Log.e("UserRepository", "Re-authentication failed", e);
            fail(application.getString(R.string.reauth_failed, e.getMessage()));
            isUpdating = false;
        });
    }

    /* ===================== EMAIL VERIFICATION SYNC ===================== */

    /**
     * ✅ FIXED: Complete email change after verification
     */
    public void completeEmailChange() {
        Log.d("UserRepository", "completeEmailChange() called - Checking for verified email");
        
        if (isUpdating) {
            Log.d("UserRepository", "Update in progress, deferring email sync");
            return;
        }
        
        startLoading();

        UserModel localUser = currentUser.getValue();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        Log.d("UserRepository", "localUser: " + (localUser != null ? localUser.getEmail() : "null"));
        Log.d("UserRepository", "firebaseUser: " + (firebaseUser != null ? firebaseUser.getEmail() : "null"));

        if (localUser == null || firebaseUser == null) {
            Log.e("UserRepository", "No user authenticated");
            fail(application.getString(R.string.no_user_auth));
            return;
        }

        // Force refresh to get latest email verification status
        firebaseUser.reload().addOnSuccessListener(v -> {
            String firebaseEmail = firebaseUser.getEmail();
            Log.d("UserRepository", "Firebase email: " + firebaseEmail + ", Local email: " + localUser.getEmail());
            
            if (firebaseEmail == null || firebaseEmail.equals(localUser.getEmail())) {
                Log.d("UserRepository", "No email change detected or emails are the same");
                stopLoading();
                return;
            }

            // Update Firestore with verified email
            UserModel updated = new UserModel(
                    localUser.getFullName(),
                    firebaseEmail,
                    localUser.getPhoneNumber(),
                    localUser.getPasswordHash()
            );
            updated.setUserId(localUser.getUserId());
            updated.setProfileImage(localUser.getProfileImage());

            Log.d("UserRepository", "Updating Firestore document with email: " + firebaseEmail);
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(localUser.getUserId())
                    .set(updated)
                    .addOnSuccessListener(done -> {
                        Log.d("UserRepository", "Firestore document updated successfully");
                        currentUser.postValue(updated);
                        successMessage.postValue(application.getString(R.string.email_updated_success, firebaseEmail));
                        stopLoading();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("UserRepository", "Firestore update failed", e);
                        fail(application.getString(R.string.profile_update_failed, e.getMessage()));
                    });
        }).addOnFailureListener(e -> {
            Log.e("UserRepository", "Failed to reload user", e);
            fail(application.getString(R.string.error_prefix, e.getMessage()));
        });
    }

    /* ===================== PHONE AUTH ===================== */

    public void sendPhoneVerificationCode(String phone, Activity activity,
                                          PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks) {
        startLoading();
        userService.sendPhoneVerificationCode(phone, activity, callbacks);
    }

    public void signInWithPhoneCredential(PhoneAuthCredential credential) {
        startLoading();

        userService.signInWithPhoneAuthCredential(credential)
                .addOnSuccessListener(auth -> {
                    FirebaseUser user = auth.getUser();
                    if (user != null) loadUserProfile(user.getUid());
                })
                .addOnFailureListener(e ->
                        fail(application.getString(R.string.login_failed, e.getMessage())));
    }

    /* ===================== GOOGLE AUTH ===================== */

    public void signInWithGoogle(String idToken) {
        startLoading();

        userService.signInWithGoogle(idToken)
                .addOnSuccessListener(auth -> {
                    FirebaseUser firebase = auth.getUser();
                    if (firebase == null) {
                        fail(application.getString(R.string.login_failed, "Google user null"));
                        return;
                    }

                    String uid = firebase.getUid();

                    userService.getUser(uid)
                            .addOnSuccessListener(user -> {
                                if (user != null) {
                                    currentUser.postValue(user);
                                    stopLoading();
                                    return;
                                }

                                UserModel newUser = new UserModel(
                                        firebase.getDisplayName(),
                                        firebase.getEmail(),
                                        firebase.getPhotoUrl() != null ? 
                                            firebase.getPhotoUrl().toString() : ""
                                );
                                newUser.setUserId(uid);

                                userService.createUser(newUser)
                                        .addOnSuccessListener(v -> {
                                            currentUser.postValue(newUser);
                                            stopLoading();
                                        })
                                        .addOnFailureListener(e ->
                                                fail(application.getString(R.string.profile_creation_failed, e.getMessage())));
                            });
                })
                .addOnFailureListener(e ->
                        fail(application.getString(R.string.login_failed, e.getMessage())));
    }

    /* ===================== LOAD PROFILE ===================== */

    public void loadUserProfile() {
        String uid = userService.getCurrentUserId();
        if (uid != null) loadUserProfile(uid);
    }

    /**
     * Manual sync trigger
     */
    public void syncVerifiedEmail() {
        Log.d("UserRepository", "Manual email sync requested");
        completeEmailChange();
    }

    /**
     * ✅ FIXED: Load profile with auto email sync check
     */
    private void loadUserProfile(String uid) {
        if (isUpdating) {
            Log.d("UserRepository", "Update in progress, skipping profile load");
            return;
        }
        
        Log.d("UserRepository", "Loading user profile for uid: " + uid);
        
        userService.getUser(uid)
                .addOnSuccessListener(user -> {
                    if (user == null) {
                        fail(application.getString(R.string.user_not_found));
                        return;
                    }
                    
                    Log.d("UserRepository", "Loaded from Firestore - Name: " + 
                          user.getFullName() + ", Email: " + user.getEmail());
                    
                    currentUser.postValue(user);
                    
                    // Check if email verification needs to be synced
                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser != null && firebaseUser.isEmailVerified() && 
                        user != null && !user.getEmail().equals(firebaseUser.getEmail())) {
                        Log.d("UserRepository", "Detected verified email mismatch, syncing Firestore");
                        completeEmailChange();
                    } else {
                        stopLoading();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserRepository", "Failed to load user profile", e);
                    fail(application.getString(R.string.profile_update_failed, e.getMessage()));
                });
    }

    /* ===================== EMAIL UPDATE API ===================== */

    /**
     * Public method for email change requests
     */
    public void updateEmail(String newEmail) {
        Log.d("UserRepository", "Public updateEmail called with: " + newEmail);
        
        FirebaseUser firebaseUser = userService.getCurrentUser();
        if (firebaseUser == null) {
            fail(application.getString(R.string.no_user_auth));
            return;
        }

        String currentEmail = firebaseUser.getEmail();
        if (currentEmail == null || currentEmail.equals(newEmail)) {
            fail(application.getString(R.string.email_same_as_current));
            return;
        }

        updateEmailOnly(newEmail, null);
    }

    /**
     * Public method to sync email status
     */
    public void syncEmailStatus() {
        Log.d("UserRepository", "Public syncEmailStatus called");
        
        FirebaseUser firebaseUser = userService.getCurrentUser();
        if (firebaseUser == null) {
            fail(application.getString(R.string.no_user_auth));
            return;
        }

        startLoading();
        
        firebaseUser.reload()
                .addOnSuccessListener(aVoid -> {
                    Log.d("UserRepository", "Firebase user reloaded successfully");
                    checkAndSyncVerifiedEmail(firebaseUser);
                })
                .addOnFailureListener(e -> {
                    Log.e("UserRepository", "Failed to reload Firebase user", e);
                    fail(application.getString(R.string.error_prefix, e.getMessage()));
                });
    }

    /**
     * ✅ FIXED: Check and sync verified email changes
     */
    private void checkAndSyncVerifiedEmail(FirebaseUser firebaseUser) {
        Log.d("UserRepository", "checkAndSyncVerifiedEmail called");
        
        if (firebaseUser == null || !firebaseUser.isEmailVerified()) {
            Log.d("UserRepository", "Email not verified or user null");
            stopLoading();
            return;
        }

        UserModel localUser = currentUser.getValue();
        if (localUser == null) {
            Log.d("UserRepository", "Local user null, loading profile first");
            loadUserProfile(firebaseUser.getUid());
            return;
        }

        String firebaseEmail = firebaseUser.getEmail();
        String localEmail = localUser.getEmail();
        
        Log.d("UserRepository", "Firebase email: " + firebaseEmail + ", Local email: " + localEmail);

        if (localUser.getUserId() == null || !localUser.getUserId().equals(firebaseUser.getUid())) {
            Log.w("UserRepository", "CRITICAL: Attempted to sync email for mismatched users! " +
                    "Local: " + localUser.getUserId() + ", Firebase: " + firebaseUser.getUid());
            stopLoading();
            return;
        }

        if (firebaseEmail == null || firebaseEmail.equals(localEmail)) {
            Log.d("UserRepository", "No email change detected");
            stopLoading();
            return;
        }

        // Update Firestore with the new verified email
        localUser.setEmail(firebaseEmail);
        userService.updateUser(localUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d("UserRepository", "Firestore updated with new email: " + firebaseEmail);
                    currentUser.postValue(localUser);
                    successMessage.postValue(application.getString(R.string.email_updated_success, firebaseEmail));
                    stopLoading();
                })
                .addOnFailureListener(e -> {
                    Log.e("UserRepository", "Failed to update Firestore with new email", e);
                    fail(application.getString(R.string.profile_update_failed, e.getMessage()));
                });
    }

    /* ===================== NOTIFICATION PREFERENCES ===================== */

    public void updateNotificationPreferences(UserModel.NotificationPreferences prefs) {
        String uid = getCurrentUserId();
        if (uid == null) {
            Log.e("UserRepository", "Cannot update preferences: User not logged in");
            return;
        }

        Log.d("UserRepository", "Updating notification preferences for user: " + uid);

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("notificationPreferences", prefs)
                .addOnSuccessListener(aVoid -> {
                    Log.d("UserRepository", "Notification preferences updated successfully");
                    
                    // Update local LiveData
                    UserModel current = currentUser.getValue();
                    if (current != null) {
                        current.setNotificationPreferences(prefs);
                        currentUser.postValue(current);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserRepository", "Failed to update notification preferences", e);
                    // If the document doesn't exist or field doesn't exist, we might need to set 'merge' but update is cleaner for existing users
                    // If "notificationPreferences" field creates issues, we might need SetOptions.merge()
                });
    }

    /* ===================== FCM TOKEN ===================== */

    public void updateFcmToken(String token) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        Log.d("UserRepository", "Updating FCM token for user: " + uid);
        
        // Use SetOptions.merge() via map or just update if document exists
        // Since user must exist to be logged in, update is fine.
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> Log.d("UserRepository", "FCM token updated"))
                .addOnFailureListener(e -> Log.e("UserRepository", "Failed to update FCM token", e));
    }

    /* ===================== UTILS ===================== */

    private void startLoading() {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        successMessage.postValue(null);
    }

    private void stopLoading() {
        isLoading.postValue(false);
    }

    private void fail(String msg) {
        errorMessage.postValue(msg);
        isLoading.postValue(false);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
    
    /**
     * Cleanup method - call this when repository is no longer needed
     */
    public void cleanup() {
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
    }
}