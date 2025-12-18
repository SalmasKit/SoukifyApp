package com.example.soukify.ui.sign;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.soukify.data.repositories.SessionRepository;
import com.example.soukify.data.repositories.UserRepository;
import com.example.soukify.data.models.UserModel;

public class SignActivityViewModel extends AndroidViewModel {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    private final MutableLiveData<Boolean> signupSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public SignActivityViewModel(Application application) {
        super(application);
        sessionRepository = SessionRepository.getInstance(application);
        userRepository = new UserRepository(application);
    }

    // ---------------------------------
    // 📌 GETTERS
    // ---------------------------------
    public LiveData<Boolean> getSignupSuccess() { return signupSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getCurrentUserId() { return sessionRepository.getCurrentUserId(); }

    // ---------------------------------
    // 📌 SIGNUP
    // ---------------------------------
    public void signup(String name, String email, String password, String phone) {
        // Validation de base
        if (name == null || name.isEmpty() ||
                email == null || email.isEmpty() ||
                password == null || password.isEmpty() ||
                phone == null || phone.isEmpty()) {
            errorMessage.setValue("All fields are required");
            signupSuccess.setValue(false);
            return;
        }

        // Inscription via Firebase
        userRepository.signUp(name, email, password, phone);

        // Observer utilisateur pour succès
        userRepository.getCurrentUser().observeForever(user -> {
            if (user != null) {
                // Créer session utilisateur
                sessionRepository.createLoginSession(
                        user.getUserId(),
                        user.getEmail(),
                        user.getFullName()
                );
                signupSuccess.postValue(true);
            }
        });

        // Observer erreurs
        userRepository.getErrorMessage().observeForever(error -> {
            if (error != null) {
                errorMessage.postValue(error);
                signupSuccess.postValue(false);
            }
        });
    }

    // ---------------------------------
    // 📌 LOGOUT
    // ---------------------------------
    public void logout() {
        userRepository.signOut();
        sessionRepository.logout();
    }
}
