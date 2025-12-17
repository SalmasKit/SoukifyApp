package com.example.soukify.data.repositories;

import android.app.Activity;
import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseUserService;
import com.example.soukify.data.models.UserModel;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

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



    public void sendPhoneVerificationCode(String phoneNumber, Activity activity,
                                          PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        userService.sendPhoneVerificationCode(phoneNumber, activity, callbacks);
    }
    public void signInWithPhoneCredential(PhoneAuthCredential credential) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        userService.signInWithPhoneAuthCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        // Charger le profil depuis Firestore
                        loadUserProfile(firebaseUser.getUid());
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Phone sign-in failed: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }




    public UserRepository(Application application) {
        this.application = application;
        FirebaseManager firebaseManager = FirebaseManager.getInstance(application);
        this.userService = new FirebaseUserService(firebaseManager.getAuth(), firebaseManager.getFirestore());
    }

    public LiveData<UserModel> getCurrentUser() {
        return currentUser;
    }

    public String getCurrentUserEmail() {
        return userService.getCurrentUserEmail();
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

    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    public LiveData<String> getSuccessMessage() { return successMessage; }

    public void resetPassword(String email) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        userService.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    successMessage.postValue("Password reset email sent. Check your inbox!");
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to send password reset email: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }


     public void updatePassword(String currentPassword, String newPassword) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        UserModel user = currentUser.getValue();
        if (user == null || user.getEmail() == null) {
            errorMessage.postValue("No user logged in");
            isLoading.postValue(false);
            return;
        }

        String firebaseAuthEmail = userService.getCurrentUserEmail();

        userService.reauthenticate(firebaseAuthEmail, currentPassword)
                .addOnSuccessListener(authResult -> {
                    userService.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid -> {
                                errorMessage.postValue("Password updated successfully");
                                isLoading.postValue(false);
                            })
                            .addOnFailureListener(e -> {
                                errorMessage.postValue("Failed to update password: " + e.getMessage());
                                isLoading.postValue(false);
                            });
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Current password is incorrect: " + e.getMessage());
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


      /**
 * Call this method after user clicks verification link
 * This will sync the verified email from Firebase Auth to Firestore
 * 100% CORRECT LOGIC - Handles all edge cases
 */
public void completeEmailChange() {
    isLoading.setValue(true);
    errorMessage.setValue(null);

    android.util.Log.d("UserRepository", "=== STARTING EMAIL CHANGE COMPLETION ===");

    // Get current user
    UserModel currentUser = this.currentUser.getValue();
    if (currentUser == null) {
        android.util.Log.e("UserRepository", "No current user found");
        errorMessage.postValue("No user data available");
        isLoading.postValue(false);
        return;
    }

    String currentFirestoreEmail = currentUser.getEmail();
    android.util.Log.d("UserRepository", "Current Firestore email: " + currentFirestoreEmail);

    // Get Firebase user and check email
    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    if (firebaseUser == null) {
        android.util.Log.e("UserRepository", "No Firebase user found");
        errorMessage.postValue("Not authenticated");
        isLoading.postValue(false);
        return;
    }

    String firebaseEmail = firebaseUser.getEmail();
    android.util.Log.d("UserRepository", "Current Firebase email: " + firebaseEmail);

    // Check if email actually changed
    if (firebaseEmail == null || firebaseEmail.equalsIgnoreCase(currentFirestoreEmail)) {
        android.util.Log.d("UserRepository", "No email change detected or emails are null");
        errorMessage.postValue("No email change needed");
        isLoading.postValue(false);
        return;
    }

    android.util.Log.d("UserRepository", "EMAIL CHANGE DETECTED: " + currentFirestoreEmail + " -> " + firebaseEmail);

    // Create updated user model with new email
    UserModel updatedUser = new UserModel(
        currentUser.getFullName(),
        firebaseEmail,  // Use Firebase email (verified)
        currentUser.getPhoneNumber(),
        currentUser.getPasswordHash()
    );
    updatedUser.setUserId(currentUser.getUserId());
    updatedUser.setProfileImage(currentUser.getProfileImage());

    android.util.Log.d("UserRepository", "Updating Firestore document with new email");

    // Update Firestore directly
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    firestore.collection("users")
            .document(currentUser.getUserId())
            .set(updatedUser)
            .addOnSuccessListener(aVoid -> {
                android.util.Log.d("UserRepository", "SUCCESS: Firestore updated with email: " + firebaseEmail);
                this.currentUser.postValue(updatedUser);
                isLoading.postValue(false);
                errorMessage.postValue("Email successfully updated to " + firebaseEmail);
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("UserRepository", "FAILED to update Firestore", e);
                errorMessage.postValue("Failed to update email: " + e.getMessage());
                isLoading.postValue(false);
            });
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
    // sign up avec google
    public void signInWithGoogle(String idToken) {

        isLoading.setValue(true);
        errorMessage.setValue(null);

        userService.signInWithGoogle(idToken)
                .addOnSuccessListener(authResult -> {

                    String uid = authResult.getUser().getUid();
                    String name = authResult.getUser().getDisplayName();
                    String email = authResult.getUser().getEmail();
                    String photo = authResult.getUser().getPhotoUrl() != null ?
                            authResult.getUser().getPhotoUrl().toString() : "";

                    // Vérifier si l'utilisateur existe déjà
                    userService.getUser(uid)
                            .addOnSuccessListener(user -> {

                                if (user != null) {
                                    // User déjà enregistré → OK
                                    currentUser.postValue(user);

                                } else {
                                    // Nouvel utilisateur Google → le créer dans Firestore
                                    UserModel newUser = new UserModel(name, email, photo);
                                    newUser.setUserId(uid);

                                    userService.createUser(newUser)
                                            .addOnSuccessListener(aVoid -> {
                                                currentUser.postValue(newUser);
                                            })
                                            .addOnFailureListener(e -> {
                                                errorMessage.postValue("Failed to create user profile: " + e.getMessage());
                                            });
                                }

                                isLoading.postValue(false);
                            })

                            .addOnFailureListener(e -> {
                                errorMessage.postValue("Failed to fetch user profile: " + e.getMessage());
                                isLoading.postValue(false);
                            });

                })

                .addOnFailureListener(e -> {
                    errorMessage.postValue("Google Sign-In failed: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }



    /**
     * Re-authenticates the user with their email and password
     * Used for sensitive operations like shop deletion
     * @param email User's email
     * @param password User's password
     * @return Task for the re-authentication operation
     */
    public com.google.android.gms.tasks.Task<Void> reauthenticate(String email, String password) {
        return userService.reauthenticate(email, password);
    }

}