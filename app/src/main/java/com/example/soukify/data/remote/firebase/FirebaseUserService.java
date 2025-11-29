package com.example.soukify.data.remote.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.soukify.data.models.UserModel;

/**
 * Firebase User Service - Handles user authentication and user data operations
 * Follows MVVM pattern by providing clean separation from UI layer
 */
public class FirebaseUserService {
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    
    private static final String USERS_COLLECTION = "users";
    
    public FirebaseUserService(FirebaseAuth auth, FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }
    
    // Authentication operations
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
     * Required for sensitive operations like email change
     */
    public Task<Void> reauthenticate(String password) {
        FirebaseUser user = getCurrentUser();
        if (user == null || user.getEmail() == null) {
            throw new IllegalStateException("No authenticated user");
        }
        
        android.util.Log.d("FirebaseUserService", "Re-authenticating user with email: " + user.getEmail());
        
        // Create credential with current email and password
        com.google.firebase.auth.AuthCredential credential = 
            com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(), password);
        
        return user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("FirebaseUserService", "Re-authentication successful");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseUserService", "Re-authentication failed", e);
                });
    }
    
    /**
     * Updates the user's authentication email
     * Requires re-authentication first
     */
    public Task<Void> updateEmail(String newEmail) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No authenticated user");
        }
        
        android.util.Log.d("FirebaseUserService", "Updating email from " + user.getEmail() + " to " + newEmail);
        
        return user.updateEmail(newEmail)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("FirebaseUserService", "Email updated successfully to: " + newEmail);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseUserService", "Failed to update email", e);
                });
    }
    
    // User data operations
    public Task<Void> createUser(UserModel user) {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return firestore.collection(USERS_COLLECTION).document(userId).set(user);
    }
    
    public Task<Void> updateUser(UserModel user) {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return firestore.collection(USERS_COLLECTION).document(userId).set(user);
    }
    
    public Task<UserModel> getUser(String userId) {
        return firestore.collection(USERS_COLLECTION).document(userId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().toObject(UserModel.class);
                    }
                    return null;
                });
    }
    
    public Task<UserModel> getCurrentUserProfile() {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return getUser(userId);
    }
    
    public Query getUserByEmail(String email) {
        return firestore.collection(USERS_COLLECTION).whereEqualTo("email", email);
    }
    
    public Task<Void> deleteUser() {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return firestore.collection(USERS_COLLECTION).document(userId).delete();
    }
}
