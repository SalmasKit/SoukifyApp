package com.example.soukify.data.remote.firebase;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.soukify.data.models.UserModel;

/**
 * Firebase User Service
 * Handles authentication and Firestore user data operations
 */
public class FirebaseUserService {
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    
    private static final String USERS_COLLECTION = "users";
    
    public FirebaseUserService(FirebaseAuth auth, FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }
    
    // ==================== AUTHENTICATION ====================
    
    public Task<AuthResult> signIn(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }
    
    public Task<AuthResult> signUp(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }
    
    public Task<AuthResult> signInAnonymously() {
        return auth.signInAnonymously();
    }
    
    public void signOut() {
        auth.signOut();
    }
    
    public Task<Void> sendPasswordResetEmail(String email) {
        return auth.sendPasswordResetEmail(email);
    }
    
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
    
    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    public String getCurrentUserEmail() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }
    
    public boolean isUserLoggedIn() {
        FirebaseUser user = getCurrentUser();
        return user != null && (user.isEmailVerified() || user.isAnonymous());
    }
    
    /**
     * Re-authenticates the user with their current password
     * Required before sensitive operations like email/password changes
     */
    public Task<Void> reauthenticate(String email, String password) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }
        
        android.util.Log.d("FirebaseUserService", "Re-authenticating user: " + email);
        
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        
        return user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("FirebaseUserService", "✅ Re-authentication successful");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseUserService", "❌ Re-authentication failed", e);
                });
    }
    
    /**
     * Updates the user's password
     * Requires recent re-authentication
     */
    public Task<Void> updatePassword(String newPassword) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }
        
        android.util.Log.d("FirebaseUserService", "Updating password");
        
        return user.updatePassword(newPassword)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("FirebaseUserService", "✅ Password updated");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseUserService", "❌ Password update failed", e);
                });
    }
    
    /**
     * ✅ SECURE EMAIL CHANGE with verification
     * Sends verification email to new address
     * Email is ONLY changed after user clicks verification link
     * This is the proper way to change email securely
     * 
     * @param newEmail The new email address to verify
     * @return Task that completes when verification email is sent
     */
    public Task<Void> sendEmailVerificationForChange(String newEmail) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }
        
        if (newEmail == null || newEmail.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("New email cannot be empty"));
        }
        
        android.util.Log.d("FirebaseUserService", "=== SENDING EMAIL VERIFICATION ===");
        android.util.Log.d("FirebaseUserService", "Current email: " + user.getEmail());
        android.util.Log.d("FirebaseUserService", "New email: " + newEmail);
        
        // verifyBeforeUpdateEmail sends verification link to new email
        // Email change takes effect ONLY after user clicks the link
        return user.verifyBeforeUpdateEmail(newEmail)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("FirebaseUserService", "✅ Verification email sent to: " + newEmail);
                    android.util.Log.d("FirebaseUserService", "User must click link to complete email change");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseUserService", "❌ Failed to send verification email", e);
                });
    }
    
    /**
     * Reloads current user to get latest data from Firebase Auth
     * Call this after user clicks verification link to get the verified email
     * 
     * @return Task that returns the updated email after reload
     */
    public Task<String> reloadAndGetCurrentEmail() {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }
        
        android.util.Log.d("FirebaseUserService", "Reloading user to check for verified email");
        
        return user.reload()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        String currentEmail = user.getEmail();
                        android.util.Log.d("FirebaseUserService", "✅ User reloaded, email: " + currentEmail);
                        return currentEmail;
                    } else {
                        android.util.Log.e("FirebaseUserService", "❌ Failed to reload user", task.getException());
                        
                        // Fallback: try to get email without reload
                        String currentEmail = user.getEmail();
                        if (currentEmail != null) {
                            android.util.Log.d("FirebaseUserService", "Got email without reload: " + currentEmail);
                            return currentEmail;
                        }
                        
                        throw new IllegalStateException("Failed to reload user", task.getException());
                    }
                });
    }
    
    /**
     * Checks if user's email has been verified
     */
    public Task<Boolean> isEmailVerified() {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }
        
        return user.reload()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        boolean verified = user.isEmailVerified();
                        android.util.Log.d("FirebaseUserService", "Email verified: " + verified);
                        return verified;
                    } else {
                        throw new IllegalStateException("Failed to check verification", task.getException());
                    }
                });
    }
    
    // ==================== FIRESTORE USER DATA ====================
    
    /**
     * Creates user profile in Firestore
     */
    public Task<Void> createUser(UserModel user) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }
        
        android.util.Log.d("FirebaseUserService", "Creating user profile in Firestore");
        android.util.Log.d("FirebaseUserService", "User ID: " + userId);
        android.util.Log.d("FirebaseUserService", "Email: " + user.getEmail());
        android.util.Log.d("FirebaseUserService", "Profile Image: " + user.getProfileImage());
        
        return firestore.collection(USERS_COLLECTION).document(userId).set(user)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("FirebaseUserService", "✅ User profile created");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseUserService", "❌ Failed to create profile", e);
                });
    }
    
    /**
     * ✅ Updates user profile in Firestore
     * PRESERVES ALL FIELDS in the UserModel
     */
    public Task<Void> updateUser(UserModel user) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }
        
        android.util.Log.d("FirebaseUserService", "=== UPDATING USER IN FIRESTORE ===");
        android.util.Log.d("FirebaseUserService", "User ID: " + userId);
        android.util.Log.d("FirebaseUserService", "Email: " + user.getEmail());
        android.util.Log.d("FirebaseUserService", "Name: " + user.getFullName());
        android.util.Log.d("FirebaseUserService", "Phone: " + user.getPhoneNumber());
        android.util.Log.d("FirebaseUserService", "Profile Image: " + user.getProfileImage());
        
        // Using .set() to ensure ALL fields are written including null values
        return firestore.collection(USERS_COLLECTION).document(userId).set(user)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("FirebaseUserService", "✅ User profile updated in Firestore");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseUserService", "❌ Failed to update profile", e);
                });
    }
    
    /**
     * Gets user profile from Firestore
     */
    public Task<UserModel> getUser(String userId) {
        android.util.Log.d("FirebaseUserService", "Getting user from Firestore: " + userId);
        
        return firestore.collection(USERS_COLLECTION).document(userId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        UserModel user = task.getResult().toObject(UserModel.class);
                        android.util.Log.d("FirebaseUserService", "✅ User profile retrieved");
                        if (user != null) {
                            android.util.Log.d("FirebaseUserService", "Email: " + user.getEmail());
                            android.util.Log.d("FirebaseUserService", "Profile Image: " + user.getProfileImage());
                        }
                        return user;
                    }
                    android.util.Log.w("FirebaseUserService", "⚠️ User profile not found");
                    return null;
                });
    }
    
    /**
     * Gets current user's profile from Firestore
     */
    public Task<UserModel> getCurrentUserProfile() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }
        return getUser(userId);
    }
    
    /**
     * Query users by email
     */
    public Query getUserByEmail(String email) {
        return firestore.collection(USERS_COLLECTION).whereEqualTo("email", email);
    }
    
    /**
     * Deletes user profile from Firestore
     */
    public Task<Void> deleteUser() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }
        
        android.util.Log.d("FirebaseUserService", "Deleting user profile from Firestore");
        
        return firestore.collection(USERS_COLLECTION).document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("FirebaseUserService", "✅ User profile deleted");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseUserService", "❌ Failed to delete profile", e);
                });
    }
}