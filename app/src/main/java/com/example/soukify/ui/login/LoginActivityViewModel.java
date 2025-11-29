package com.example.soukify.ui.login;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.soukify.data.repositories.SessionRepository;
import com.example.soukify.data.repositories.UserRepository;
import com.example.soukify.data.models.UserModel;

public class LoginActivityViewModel extends AndroidViewModel {
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    
    public LoginActivityViewModel(Application application) {
        super(application);
        sessionRepository = SessionRepository.getInstance(application);
        userRepository = new UserRepository(application);
        
        // Observe repository changes
        observeRepositories();
    }
    
    private void observeRepositories() {
        userRepository.getIsLoading().observeForever(loading -> {
            isLoading.postValue(loading);
        });
        
        userRepository.getErrorMessage().observeForever(error -> {
            if (error != null) {
                errorMessage.postValue(error);
            }
        });
    }
    
    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getCurrentUserId() {
        return sessionRepository.getCurrentUserId();
    }
    
    public LiveData<UserModel> getCurrentUser() {
        return userRepository.getCurrentUser();
    }
    
    // Login functionality
    public void login(String email, String password) {
        // Validate input
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            errorMessage.postValue("Email and password cannot be empty");
            loginSuccess.postValue(false);
            return;
        }
        
        if (!isValidEmail(email)) {
            errorMessage.postValue("Please enter a valid email address");
            loginSuccess.postValue(false);
            return;
        }
        
        // Use Firebase Authentication
        userRepository.signIn(email, password);
        
        // Observe the result
        userRepository.getCurrentUser().observeForever(user -> {
            if (user != null) {
                // Login successful - create session
                sessionRepository.createLoginSession(
                    user.getUserId(), 
                    user.getEmail(), 
                    user.getFullName()
                );
                loginSuccess.postValue(true);
                errorMessage.postValue(null);
                successMessage.postValue("Login successful!");
            }
        });
        
        userRepository.getErrorMessage().observeForever(error -> {
            if (error != null) {
                errorMessage.postValue(error);
                loginSuccess.postValue(false);
            }
        });
    }
    
    // Sign up functionality
    public void signUp(String fullName, String email, String password, String confirmPassword, String phoneNumber) {
        // Validate inputs
        if (fullName == null || fullName.trim().isEmpty()) {
            errorMessage.setValue("Full name is required");
            return;
        }
        
        if (email == null || email.trim().isEmpty()) {
            errorMessage.setValue("Email is required");
            return;
        }
        
        if (!isValidEmail(email)) {
            errorMessage.setValue("Please enter a valid email address");
            return;
        }
        
        if (password == null || password.trim().isEmpty()) {
            errorMessage.setValue("Password is required");
            return;
        }
        
        if (password.length() < 6) {
            errorMessage.setValue("Password must be at least 6 characters");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            errorMessage.setValue("Passwords do not match");
            return;
        }
        
        userRepository.signUp(fullName, email, password, phoneNumber);
        
        // Observe sign up result
        userRepository.getCurrentUser().observeForever(user -> {
            if (user != null) {
                successMessage.postValue("Account created successfully!");
            }
        });
    }
    
    // Password reset functionality
    public void resetPassword(String email) {
        if (email == null || email.trim().isEmpty()) {
            errorMessage.setValue("Email is required");
            return;
        }
        
        if (!isValidEmail(email)) {
            errorMessage.setValue("Please enter a valid email address");
            return;
        }
        
        userRepository.resetPassword(email);
        successMessage.setValue("Password reset email sent");
    }
    
    // Profile management
    public void updateProfile(UserModel user) {
        if (user == null) {
            errorMessage.setValue("Invalid user data");
            return;
        }
        
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            errorMessage.setValue("Full name is required");
            return;
        }
        
        userRepository.updateProfile(user);
        successMessage.setValue("Profile updated successfully");
    }
    
    // Session management
    public void logout() {
        userRepository.signOut();
        sessionRepository.logout();
        successMessage.setValue("Signed out successfully");
    }
    
    public boolean isUserLoggedIn() {
        return userRepository.isUserLoggedIn();
    }
    
    public String getCurrentUserIdString() {
        return userRepository.getCurrentUserId();
    }
    
    public void loadUserProfile() {
        userRepository.loadUserProfile();
    }
    
    public void clearMessages() {
        errorMessage.setValue(null);
        successMessage.setValue(null);
    }
    
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
