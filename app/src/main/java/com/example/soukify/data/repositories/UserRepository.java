package com.example.soukify.data.repositories;

import android.app.Activity;
import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.soukify.data.models.UserModel;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseUserService;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * ===== User Repository =====
 * Centralise toutes les opérations Firebase :
 * - Auth Email / Password
 * - Auth Google
 * - Auth Phone
 * - Reset Password
 * - Update Password
 * - Update Profile + Email
 * - Firestore User
 * - Session User
 */
public class UserRepository {

    private final FirebaseUserService userService;
    private final Application application;

    private final MutableLiveData<UserModel> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public UserRepository(Application application) {
        this.application = application;
        FirebaseManager manager = FirebaseManager.getInstance(application);
        this.userService = new FirebaseUserService(
                manager.getAuth(),
                manager.getFirestore()
        );
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
                .addOnFailureListener(e -> fail("Sign in failed: " + e.getMessage()));
    }

    public void signUp(String fullName, String email, String password, String phone) {
        startLoading();

        userService.signUp(email, password)
                .addOnSuccessListener(auth -> {
                    FirebaseUser firebaseUser = auth.getUser();
                    if (firebaseUser == null) {
                        fail("User creation failed");
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
                                    fail("Failed to create profile: " + e.getMessage()));
                })
                .addOnFailureListener(e -> fail("Sign up failed: " + e.getMessage()));
    }

    public void signOut() {
        userService.signOut();
        currentUser.postValue(null);
        successMessage.postValue("Signed out successfully");
    }

    
    /* ===================== PASSWORD ===================== */

    public void resetPassword(String email) {
        startLoading();

        userService.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    successMessage.postValue("Password reset email sent");
                    stopLoading();
                })
                .addOnFailureListener(e ->
                        fail("Failed to send reset email: " + e.getMessage()));
    }

    public void updatePassword(String currentPassword, String newPassword) {
        startLoading();

        String email = userService.getCurrentUserEmail();
        if (email == null) {
            fail("No authenticated user");
            return;
        }

        userService.reauthenticate(email, currentPassword)
                .addOnSuccessListener(aVoid ->
                        userService.updatePassword(newPassword)
                                .addOnSuccessListener(v -> {
                                    successMessage.postValue("Password updated successfully");
                                    stopLoading();
                                })
                                .addOnFailureListener(e ->
                                        fail("Password update failed: " + e.getMessage()))
                )
                .addOnFailureListener(e ->
                        fail("Current password incorrect: " + e.getMessage()));
    }

    /* ===================== PROFILE ===================== */

    public void updateProfile(UserModel user) {
        startLoading();

        userService.updateUser(user)
                .addOnSuccessListener(aVoid -> {
                    currentUser.postValue(user);
                    stopLoading();
                })
                .addOnFailureListener(e ->
                        fail("Profile update failed: " + e.getMessage()));
    }

    public void updateProfileWithEmail(UserModel user, String newEmail, String password) {
        startLoading();

        String currentEmail = userService.getCurrentUserEmail();

        if (newEmail == null || newEmail.equals(currentEmail)) {
            updateProfile(user);
            return;
        }

        userService.reauthenticate(password)
                .addOnSuccessListener(v ->
                        userService.updateEmail(newEmail)
                                .addOnSuccessListener(done ->
                                        userService.updateUser(user)
                                                .addOnSuccessListener(ok -> {
                                                    currentUser.postValue(user);
                                                    stopLoading();
                                                })
                                                .addOnFailureListener(e ->
                                                        fail("Profile update failed: " + e.getMessage()))
                                )
                                .addOnFailureListener(e ->
                                        fail("Email update failed: " + e.getMessage()))
                )
                .addOnFailureListener(e ->
                        fail("Re-authentication failed: " + e.getMessage()));
    }

    /* ===================== EMAIL VERIFICATION SYNC ===================== */

    public void completeEmailChange() {
        startLoading();

        UserModel localUser = currentUser.getValue();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (localUser == null || firebaseUser == null) {
            fail("No user authenticated");
            return;
        }

        String firebaseEmail = firebaseUser.getEmail();
        if (firebaseEmail == null || firebaseEmail.equals(localUser.getEmail())) {
            fail("No email change detected");
            return;
        }

        UserModel updated = new UserModel(
                localUser.getFullName(),
                firebaseEmail,
                localUser.getPhoneNumber(),
                localUser.getPasswordHash()
        );
        updated.setUserId(localUser.getUserId());
        updated.setProfileImage(localUser.getProfileImage());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(localUser.getUserId())
                .set(updated)
                .addOnSuccessListener(v -> {
                    currentUser.postValue(updated);
                    successMessage.postValue("Email updated successfully");
                    stopLoading();
                })
                .addOnFailureListener(e ->
                        fail("Firestore update failed: " + e.getMessage()));
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
                        fail("Phone sign-in failed: " + e.getMessage()));
    }

    /* ===================== GOOGLE AUTH ===================== */

    public void signInWithGoogle(String idToken) {
        startLoading();

        userService.signInWithGoogle(idToken)
                .addOnSuccessListener(auth -> {
                    FirebaseUser firebase = auth.getUser();
                    if (firebase == null) {
                        fail("Google user null");
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
                                        firebase.getPhotoUrl() != null ? firebase.getPhotoUrl().toString() : ""
                                );
                                newUser.setUserId(uid);

                                userService.createUser(newUser)
                                        .addOnSuccessListener(v -> {
                                            currentUser.postValue(newUser);
                                            stopLoading();
                                        })
                                        .addOnFailureListener(e ->
                                                fail("Create Google user failed: " + e.getMessage()));
                            });
                })
                .addOnFailureListener(e ->
                        fail("Google Sign-In failed: " + e.getMessage()));
    }

    /* ===================== LOAD PROFILE ===================== */

    public void loadUserProfile() {
        String uid = userService.getCurrentUserId();
        if (uid != null) loadUserProfile(uid);
    }

    private void loadUserProfile(String uid) {
        userService.getUser(uid)
                .addOnSuccessListener(user -> {
                    currentUser.postValue(user);
                    stopLoading();
                })
                .addOnFailureListener(e ->
                        fail("Load profile failed: " + e.getMessage()));
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


}
