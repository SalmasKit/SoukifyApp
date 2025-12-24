package com.example.soukify.data.remote.firebase;

import android.app.Activity;
import android.util.Log;

import com.example.soukify.data.models.UserModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.concurrent.TimeUnit;

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

    /* ===================== AUTH ===================== */

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

    /* ===================== REAUTH ===================== */

    public Task<Void> reauthenticate(String password) {
        FirebaseUser user = getCurrentUser();
        if (user == null || user.getEmail() == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }

        AuthCredential credential =
                EmailAuthProvider.getCredential(user.getEmail(), password);

        return user.reauthenticate(credential);
    }

    public Task<Void> reauthenticate(String email, String password) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }

        AuthCredential credential =
                EmailAuthProvider.getCredential(email, password);

        return user.reauthenticate(credential);
    }

    /* ===================== UPDATE AUTH ===================== */

    public Task<Void> updatePassword(String newPassword) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }
        return user.updatePassword(newPassword);
    }

    public Task<Void> updateEmail(String newEmail) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }
        return user.updateEmail(newEmail);
    }

    public boolean isEmailVerified() {
        FirebaseUser user = getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    
    /* ===================== FIRESTORE USER ===================== */

    public Task<Void> createUser(UserModel user) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(user);
    }

    public Task<Void> updateUser(UserModel user) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(user);
    }

    public Task<UserModel> getUser(String userId) {
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .continueWith(task ->
                        task.isSuccessful() && task.getResult() != null
                                ? task.getResult().toObject(UserModel.class)
                                : null
                );
    }

    public Task<UserModel> getCurrentUserProfile() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }
        return getUser(userId);
    }

    public Query getUserByEmail(String email) {
        return firestore.collection(USERS_COLLECTION)
                .whereEqualTo("email", email);
    }

    public Task<Void> deleteUser() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .delete();
    }

    /* ===================== GOOGLE AUTH ===================== */

    public Task<AuthResult> signInWithGoogle(String idToken) {
        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);
        return auth.signInWithCredential(credential);
    }

    /* ===================== PHONE AUTH ===================== */

    public void sendPhoneVerificationCode(
            String phoneNumber,
            Activity activity,
            PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks
    ) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(activity)
                        .setCallbacks(callbacks)
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public Task<AuthResult> signInWithPhoneAuthCredential(
            PhoneAuthCredential credential
    ) {
        return auth.signInWithCredential(credential);
    }
}
