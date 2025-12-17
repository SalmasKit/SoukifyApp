package com.example.soukify.data.remote.firebase;

import android.app.Activity;


import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.soukify.data.models.UserModel;

import java.util.concurrent.TimeUnit;

/**
 * Firebase User Service - Handles user authentication and user data operations
 * Follows MVVM pattern by providing clean separation from UI layer
 */
public class 
FirebaseUserService {
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

    //update de password
    public Task<Void> sendPasswordResetEmail(String email) {
        return auth.sendPasswordResetEmail(email);
    }
    //recuperer le user actuelle qui est entre acteullement dans le fire base
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
    //verification de identite de user actuelle
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
     * Updates the user's authentication email
     * Requires re-authentication first
     */
    //changement de email
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

    // creation de la User
    public Task<Void> createUser(UserModel user) {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return firestore.collection(USERS_COLLECTION).document(userId).set(user);
    }
    //changement de user
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
    // la fonction pour authentification avec google
    public Task<AuthResult> signInWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        return auth.signInWithCredential(credential);
    }

    //authentification avec phone

    public void sendPhoneVerificationCode(String phoneNumber, Activity activity,
                                          PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public Task<AuthResult> signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        return auth.signInWithCredential(credential);
    }




}