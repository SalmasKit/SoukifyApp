package com.example.soukify.ui.login;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.soukify.data.repositories.SessionRepository;
import com.example.soukify.data.repositories.UserRepository;
import com.example.soukify.data.models.UserModel;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class LoginActivityViewModel extends AndroidViewModel {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    // PHONE AUTH
    private final MutableLiveData<String> phoneVerificationId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> phoneAuthSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> phoneAuthError = new MutableLiveData<>();

    public LoginActivityViewModel(Application application) {
        super(application);

        sessionRepository = SessionRepository.getInstance(application);
        userRepository = new UserRepository(application);

        observeRepositoryBase();
    }

    // -----------------------------
    // 🔥 OBSERVER CENTRALISÉ
    // -----------------------------
    private void observeRepositoryBase() {

        userRepository.getIsLoading().observeForever(isLoading::setValue);

        userRepository.getErrorMessage().observeForever(error -> {
            if (error != null) {
                errorMessage.setValue(error);
            }
        });

        // ✅ AJOUT ICI (TRÈS IMPORTANT)
        userRepository.getSuccessMessage().observeForever(message -> {
            if (message != null) {
                successMessage.setValue(message);
            }
        });

        userRepository.getCurrentUser().observeForever(user -> {
            if (user != null) {
                createUserSession(user);
            }
        });
    }


    private void createUserSession(UserModel user) {
        sessionRepository.createLoginSession(
                user.getUserId(),
                user.getEmail(),
                user.getFullName()
        );
    }




    // ---------------------------------
    // 📌 LOGIN EMAIL / PASSWORD
    // ---------------------------------
    public void login(String email, String password) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            errorMessage.setValue("Email and password cannot be empty");
            return;
        }

        if (!isValidEmail(email)) {
            errorMessage.setValue("Invalid email format");
            return;
        }

        isLoading.setValue(true);
        userRepository.signIn(email, password);

        userRepository.getCurrentUser().observeForever(user -> {
            if (user != null) {
                loginSuccess.setValue(true);
                successMessage.setValue("Login successful!");
                isLoading.setValue(false);
            }
        });

        userRepository.getErrorMessage().observeForever(error -> {
            if (error != null) {
                errorMessage.setValue(error);
                loginSuccess.setValue(false);
                isLoading.setValue(false);
            }
        });
    }

    // ---------------------------------
    // 📌 SIGN UP
    // ---------------------------------
    public void signUp(String fullName, String email, String password, String confirmPassword, String phone) {
        if (fullName == null || fullName.trim().isEmpty()) {
            errorMessage.setValue("Full name required");
            return;
        }

        if (!isValidEmail(email)) {
            errorMessage.setValue("Invalid email");
            return;
        }

        if (password == null || password.length() < 6) {
            errorMessage.setValue("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorMessage.setValue("Passwords do not match");
            return;
        }

        userRepository.signUp(fullName, email, password, phone);

        userRepository.getCurrentUser().observeForever(user -> {
            if (user != null) {
                successMessage.setValue("Account created successfully!");
            }
        });
    }

    // ---------------------------------
    // 📌 RESET PASSWORD
    // ---------------------------------
    public void resetPassword(String email) {
        if (!isValidEmail(email)) {
            errorMessage.setValue("Email invalide");
            return;
        }
        userRepository.resetPassword(email);
    }

    // ---------------------------------
    // 📌 PROFILE UPDATE
    // ---------------------------------
    public void updateProfile(UserModel user, String newEmail, String password) {
        if (user == null || user.getFullName().trim().isEmpty()) {
            errorMessage.setValue("Invalid user data");
            return;
        }
        userRepository.updateProfile(user, newEmail, password);
        successMessage.setValue("Profile updated");
    }

    // ---------------------------------
    // 📌 SOCIAL LOGIN
    // ---------------------------------
    public void signInWithGoogle(String idToken) {
        isLoading.setValue(true);
        userRepository.signInWithGoogle(idToken);

        userRepository.getCurrentUser().observeForever(user -> {
            if (user != null) {
                loginSuccess.setValue(true);
                successMessage.setValue("Google Login Successful");
                isLoading.setValue(false);
            }
        });

        userRepository.getErrorMessage().observeForever(error -> {
            if (error != null) {
                errorMessage.setValue(error);
                loginSuccess.setValue(false);
                isLoading.setValue(false);
            }
        });
    }

    // ---------------------------------
    // 📌 GENERAL
    // ---------------------------------
    public void logout() {
        userRepository.signOut();
        sessionRepository.logout();
        successMessage.setValue("Signed out");
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // ---------------------------------
    // 📌 GETTERS
    // ---------------------------------
    public LiveData<Boolean> getLoginSuccess() { return loginSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getPhoneVerificationId() { return phoneVerificationId; }
    public LiveData<Boolean> getPhoneAuthSuccess() { return phoneAuthSuccess; }
    public LiveData<String> getPhoneAuthError() { return phoneAuthError; }
    public LiveData<UserModel> getCurrentUser() { return userRepository.getCurrentUser(); }
}